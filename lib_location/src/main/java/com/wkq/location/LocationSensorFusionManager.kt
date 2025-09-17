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
    private fun triggerCallback(point: SensorPointInfo) {
        val now = System.currentTimeMillis()
        if (now - lastCallbackTime < callbackInterval) return
        val dt = (now - lastCallbackTime) / 1000f

        // 融合 GPS + PDR
        val gpsWeight = lastGpsLocation?.let {
            val w = ((50 - it.accuracy) / 50f).coerceIn(0f, 1f)
            if (it.accuracy > 25f) 0f else w
        } ?: 0f

        val finalLat = lastGpsLocation?.let { gpsWeight * it.latitude + (1 - gpsWeight) * point.lat } ?: point.lat
        val finalLng = lastGpsLocation?.let { gpsWeight * it.longitude + (1 - gpsWeight) * point.lng } ?: point.lng
        val finalAlt = lastGpsLocation?.let { gpsWeight * it.altitude.toFloat() + (1 - gpsWeight) * point.altitude } ?: point.altitude

        // 计算速度和微小移动过滤
        var speed = 0f
        lastPoint?.let { last ->
            val distance = distanceBetween(last.lat, last.lng, finalLat, finalLng)
            speed = if (dt > 0) (distance / dt).toFloat() else 0f
            val gpsHalf = (lastGpsLocation?.accuracy ?: 5f) / 2
            val minMoveDistance = if (gpsHalf > 1.0f) gpsHalf else 1.0f
            val distanceF = distance.toFloat()
            if (distanceF < minMoveDistance && speed < 0.1f) return
        }

        // 平滑融合（低通滤波）
        val alpha = 0.3f
        val fusedLat = alpha * finalLat + (1 - alpha) * (lastPoint?.lat ?: finalLat)
        val fusedLng = alpha * finalLng + (1 - alpha) * (lastPoint?.lng ?: finalLng)
        val fusedAlt = alpha * finalAlt + (1 - alpha) * (lastPoint?.altitude ?: finalAlt)

        val fusedPoint = SensorPointInfo(fusedLat, fusedLng, fusedAlt, point.heading, speed)
        lastCallbackTime = now
        lastPoint = fusedPoint

        trackPoints.add(fusedPoint)
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
