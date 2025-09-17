package com.wkq.location.sensor

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PDRTracker: 惯性步行定位追踪器
 * 支持：
 * 1. 步伐更新经纬度 + 卡尔曼滤波平滑
 * 2. 可调 Q/R 控制平滑程度
 * 3. 步长动态 + 指数平滑
 * 4. 方向角低通滤波
 * 5. 高度支持气压 / GPS fallback
 * 6. 传感器自动适配
 */
class PDRTracker(
    processNoise: Double = 1e-5,
    measurementNoise: Double = 1e-2
) {

    var lat = 0.0
        private set
    var lng = 0.0
        private set
    var altitude = 0f
        private set

     var lastHeading = 0f
    private var headingFiltered = 0f

    internal var stepLength = 0.7
    private var lastAccMagnitude = 0f

    private var basePressure: Float? = null

    private var hasPressure = true
    private var hasHeading = true
    private var hasStep = true
    private var hasAccelerometer = true

    private val kalman = KalmanFilter(processNoise, measurementNoise)

    /** 设置传感器支持性 */
    fun setSensorSupport(rotation: Boolean, step: Boolean, pressure: Boolean) {
        hasHeading = rotation
        hasStep = step
        hasPressure = pressure
    }

    /** 设置初始经纬度 */
    fun setStart(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
        kalman.reset()
        kalman.update(lat, lng)
    }

    /** 设置 GPS 高度 fallback */
    fun setGpsAltitude(gpsAltitude: Float) {
        if (!hasPressure) altitude = gpsAltitude
    }

    /** 更新方向角，低通滤波 */
    fun updateHeading(heading: Float) {
        if (!hasHeading) return
        headingFiltered = 0.8f * headingFiltered + 0.2f * heading
        lastHeading = headingFiltered
    }

    /** 动态计算步长 + 指数平滑 */
    fun computeStepLength(ax: Float, ay: Float, az: Float) {
        if (!hasAccelerometer) return
        val magnitude = sqrt(ax*ax + ay*ay + az*az)
        val diff = magnitude - lastAccMagnitude
        lastAccMagnitude = magnitude
        val rawStep = (0.6 + diff*0.1f).coerceIn(0.5, 1.2)
        stepLength = 0.8 * stepLength + 0.2 * rawStep
    }

    /** 气压 → 高度 */
    fun updatePressure(pressure: Float) {
        if (!hasPressure) return
        if (basePressure == null) basePressure = pressure
        altitude = 44330f * (1f - (pressure / basePressure!!).pow(1f / 5.255f))
    }

    /** 步伐更新，经纬度推算 + 卡尔曼平滑 */
    fun onStepDetected(): Pair<Double, Double> {
        if (!hasStep) return Pair(lat, lng)

        val rad = Math.toRadians(lastHeading.toDouble())
        lat += stepLength * cos(rad) / 111111
        lng += stepLength * sin(rad) / (111111 * cos(Math.toRadians(lat)))

        // 卡尔曼滤波平滑
        val (smLat, smLng) = kalman.update(lat, lng)
        lat = smLat
        lng = smLng

        return Pair(lat, lng)
    }

    /** 设置卡尔曼参数，可实时调整 */
    fun setKalmanParams(processNoise: Double, measurementNoise: Double) {
        kalman.processNoise = processNoise
        kalman.measurementNoise = measurementNoise
    }

    fun reset() {
        lat = 0.0
        lng = 0.0
        altitude = 0f
        lastHeading = 0f
        headingFiltered = 0f
        stepLength = 0.7
        lastAccMagnitude = 0f
        basePressure = null
        kalman.reset()
    }
}

