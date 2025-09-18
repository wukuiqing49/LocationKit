package com.wkq.location

import android.content.Context
import android.location.Location
import android.util.Log
import com.wkq.location.sensor.LocationCallback
import com.wkq.location.sensor.PDRTracker
import com.wkq.location.sensor.SensorHelper
import com.wkq.location.sensor.SensorPointInfo
import com.wkq.location.sensor.SensorStatus
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.max
/**
 * HybridLocationFusionManager
 *
 * 功能：
 * 1. 融合 GPS + PDR（步行推算） + Pressure（气压高度）
 * 2. 动态步长 + 航向平滑 + Kalman 融合
 * 3. 支持轨迹记录
 * 4. 自动处理传感器支持性，不支持时 fallback 到 GPS 或默认值
 * 5. 回调包含速度信息
 */
class LocationSensorFusionManager(private val context: Context) {

    private var callback: LocationCallback? = null
    private val sensorHelper = SensorHelper(context)
    private val pdr = PDRTracker() // 可调卡尔曼已集成
    private var lastGpsLocation: Location? = null
    private var lastCallbackTime = 0L
    private val callbackInterval = 200L
    private var lastPoint: SensorPointInfo? = null

    val trackPoints = mutableListOf<SensorPointInfo>()

    /** 启动融合定位 */
    fun start(callback: LocationCallback) {
        this.callback = callback

        // 首次回调传感器支持状态
        val sensorStatusList = listOf(
            SensorStatus("RotationVector", sensorHelper.hasRotation),
            SensorStatus("StepDetector", sensorHelper.hasStepDetector),
            SensorStatus("Pressure", sensorHelper.hasPressure),
            SensorStatus("LinearAcceleration", sensorHelper.hasAccelerometer)
        )
        callback.onSensorSupportStatus(sensorStatusList)

        // 启动 GPS 定位
        LocationKit.startLocation { result ->
            if (result.success) {
                lastGpsLocation = result.location
                lastGpsLocation?.let {
                    pdr.setStart(it.latitude, it.longitude)
                    pdr.setGpsAltitude(it.altitude.toFloat())
                    pdr.setSensorSupport(
                        rotation = sensorHelper.hasRotation,
                        step = sensorHelper.hasStepDetector,
                        pressure = sensorHelper.hasPressure
                    )
                    val point = SensorPointInfo(it.latitude, it.longitude, pdr.altitude, 0f, 0f)
                    trackPoints.add(point)
                    triggerCallback(point)
                }
            }
        }

        // 启动传感器监听
        sensorHelper.start(object : SensorHelper.SensorCallback {
            override fun onHeadingChanged(heading: Float) { pdr.updateHeading(heading) }

            override fun onStepDetected(stepCount: Int) {
                val pos = pdr.onStepDetected()  // 内部已卡尔曼滤波
                val heading = pdr.lastHeading
                val alt = pdr.altitude
                val point = SensorPointInfo(pos.first, pos.second, alt, heading, 0f)
                trackPoints.add(point)
                Log.d("PDR", "step detected: lat=${pos.first}, lng=${pos.second}, stepLength=${pdr.stepLength}")
                triggerCallback(point)


            }

            override fun onAccelerationChanged(ax: Float, ay: Float, az: Float) { pdr.computeStepLength(ax, ay, az) }

            override fun onPressureChanged(pressure: Float) { pdr.updatePressure(pressure) }
        })
    }

    /** 停止融合定位 */
    fun stop() {
        sensorHelper.stop()
        LocationKit.stopLocation()
    }

    /** 清空轨迹 */
    fun reset() {
        trackPoints.clear()
        lastGpsLocation = null
        pdr.reset()
        lastPoint = null
        lastCallbackTime = 0L
    }

    /** 触发回调，融合 GPS + PDR + Pressure + 速度 */
    private var firstPoint: SensorPointInfo? = null
    private val loopCorrectionThreshold = 5f   // 米，距离起点小于该值触发闭环校正
    private val loopCorrectionWindow = 10      // 最近10个点进行微调

    private fun triggerCallback(point: SensorPointInfo) {
        val now = System.currentTimeMillis()
        if (now - lastCallbackTime < callbackInterval) return
        val dt = (now - lastCallbackTime) / 1000f

        // ---------------- 动态 GPS 权重 ----------------
        val gpsWeight = lastGpsLocation?.let {
            if (it.accuracy > 50f) 0f else ((50 - it.accuracy) / 50f).coerceIn(0.1f, 0.7f)
        } ?: 0f

        // 融合 GPS + PDR
        val fusedLat = lastGpsLocation?.let { gpsWeight * it.latitude + (1 - gpsWeight) * point.lat } ?: point.lat
        val fusedLng = lastGpsLocation?.let { gpsWeight * it.longitude + (1 - gpsWeight) * point.lng } ?: point.lng
        val fusedAlt = lastGpsLocation?.let { gpsWeight * it.altitude.toFloat() + (1 - gpsWeight) * point.altitude } ?: point.altitude

        // ---------------- 速度计算 + 微动过滤 ----------------
        var speed = 0f
        lastPoint?.let { last ->
            val distance = distanceBetween(last.lat, last.lng, fusedLat, fusedLng)
            speed = if (dt > 0) (distance / dt).toFloat() else 0f
            val gpsHalf = (lastGpsLocation?.accuracy ?: 5f) / 2
            val minMoveDistance = max(gpsHalf, 0.5f)
            if (distance < minMoveDistance && speed < 0.1f) return
        }

        // ---------------- 动态低通滤波 ----------------
        val alpha = gpsWeight.coerceIn(0.3f, 0.7f)
        val finalLat = alpha * fusedLat + (1 - alpha) * (lastPoint?.lat ?: fusedLat)
        val finalLng = alpha * fusedLng + (1 - alpha) * (lastPoint?.lng ?: fusedLng)
        val finalAlt = alpha * fusedAlt + (1 - alpha) * (lastPoint?.altitude ?: fusedAlt)

        val fusedPoint = SensorPointInfo(finalLat, finalLng, finalAlt, point.heading, speed)
        lastCallbackTime = now
        lastPoint = fusedPoint

        // ---------------- 设置起点 ----------------
        if (firstPoint == null) firstPoint = fusedPoint

        trackPoints.add(fusedPoint)

        // ---------------- 闭环校正 ----------------
        firstPoint?.let { start ->
            val distToStart = distanceBetween(start.lat, start.lng, fusedPoint.lat, fusedPoint.lng)
            if (distToStart < loopCorrectionThreshold && trackPoints.size > loopCorrectionWindow) {
                val correctionPoints = trackPoints.takeLast(loopCorrectionWindow)
                val deltaLat = (start.lat - fusedPoint.lat) / loopCorrectionWindow
                val deltaLng = (start.lng - fusedPoint.lng) / loopCorrectionWindow
                for (i in correctionPoints.indices) {
                    val idx = trackPoints.size - loopCorrectionWindow + i
                    val p = trackPoints[idx]
                    trackPoints[idx] = p.copy(
                        lat = p.lat + deltaLat * (i + 1),
                        lng = p.lng + deltaLng * (i + 1)
                    )
                }
            }
        }

        // 回调
        callback?.onLocationUpdate(fusedPoint)
    }




    /** Haversine 计算两点间距离（米） */
    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0 // 地球半径
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat/2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng/2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
