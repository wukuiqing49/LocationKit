package com.wkq.location.sensor

import android.content.Context
import android.hardware.*
import kotlin.math.abs

/**
 * SensorHelper
 *
 * 封装 Rotation Vector / Linear Acceleration / Step Detector / Pressure 传感器
 * 功能：
 * 1. 支持传感器是否存在的检测
 * 2. 支持回调限速（heading / acceleration / pressure）
 * 3. 支持阈值判断减少无效回调
 * 4. 提供动态配置接口 Config
 */
class SensorHelper(private val context: Context) : SensorEventListener {

    /** 传感器配置，可动态调整采样周期、回调间隔、阈值 */
    data class Config(
        val rotationSamplingDelay: Int = SensorManager.SENSOR_DELAY_GAME,
        val accelerometerSamplingDelay: Int = SensorManager.SENSOR_DELAY_GAME,
        val stepSamplingDelay: Int = SensorManager.SENSOR_DELAY_GAME,
        val pressureSamplingDelay: Int = SensorManager.SENSOR_DELAY_NORMAL,
        val headingIntervalMs: Long = 100L,   // 航向回调限速
        val accIntervalMs: Long = 100L,       // 加速度回调限速
        val pressureIntervalMs: Long = 500L,  // 气压回调限速
        val headingThreshold: Float = 1f,     // 航向变化阈值（°）
        val accThreshold: Float = 0.01f       // 加速度变化阈值 (m/s²)
    )

    /** 外部回调接口 */
    interface SensorCallback {
        fun onHeadingChanged(heading: Float)
        fun onStepDetected(stepCount: Int)
        fun onAccelerationChanged(ax: Float, ay: Float, az: Float)
        fun onPressureChanged(pressure: Float)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // 传感器实例
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    // 设备是否支持这些传感器
    var hasRotation = rotationVectorSensor != null
    var hasAccelerometer = accelerometerSensor != null
    var hasStepDetector = stepDetectorSensor != null
    var hasPressure = pressureSensor != null

    private var callback: SensorCallback? = null
    private var config: Config = Config()

    private var stepCount = 0

    // 上次回调时间戳，用于限速
    private var lastHeadingTime = 0L
    private var lastAccTime = 0L
    private var lastPressureTime = 0L

    private var lastHeading = 0f

    /** 动态设置配置 */
    fun setConfig(config: Config) { this.config = config }

    /** 开始监听传感器 */
    fun start(callback: SensorCallback) {
        this.callback = callback
        rotationVectorSensor?.let { sensorManager.registerListener(this, it, config.rotationSamplingDelay) }
        accelerometerSensor?.let { sensorManager.registerListener(this, it, config.accelerometerSamplingDelay) }
        stepDetectorSensor?.let { sensorManager.registerListener(this, it, config.stepSamplingDelay) }
        pressureSensor?.let { sensorManager.registerListener(this, it, config.pressureSamplingDelay) }
    }

    /** 停止监听传感器 */
    fun stop() { sensorManager.unregisterListener(this) }

    /** 系统传感器回调 */
    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        when (event.sensor.type) {

            // Rotation Vector → 航向角
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var heading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (heading < 0) heading += 360f

                // 限速 + 阈值判断
                if (now - lastHeadingTime > config.headingIntervalMs &&
                    abs(heading - lastHeading) > config.headingThreshold
                ) {
                    lastHeadingTime = now
                    lastHeading = heading
                    callback?.onHeadingChanged(heading)
                }
            }

            // Linear Acceleration → 去重力加速度
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                if (now - lastAccTime > config.accIntervalMs &&
                    (abs(ax) > config.accThreshold || abs(ay) > config.accThreshold || abs(az) > config.accThreshold)
                ) {
                    lastAccTime = now
                    callback?.onAccelerationChanged(ax, ay, az)
                }
            }

            // Step Detector → 步伐检测
            Sensor.TYPE_STEP_DETECTOR -> {
                stepCount++
                callback?.onStepDetected(stepCount)
            }

            // Pressure → 气压
            Sensor.TYPE_PRESSURE -> {
                val pressure = event.values[0]
                if (now - lastPressureTime > config.pressureIntervalMs) {
                    lastPressureTime = now
                    callback?.onPressureChanged(pressure)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
