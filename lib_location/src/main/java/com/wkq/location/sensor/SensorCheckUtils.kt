package com.wkq.location.sensor



import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * @Author: wkq
 *
 * @Time: 2025/9/16 14:24
 *
 * SensorCheckUtils
 * 功能：检测设备传感器是否可用
 * 支持常用传感器：
 *  - TYPE_ROTATION_VECTOR
 *  - TYPE_LINEAR_ACCELERATION
 *  - TYPE_STEP_DETECTOR
 *  - TYPE_PRESSURE
 */
object SensorCheckUtils {

    /**
     * 判断设备是否支持指定类型的传感器
     * @param context 上下文
     * @param sensorType Sensor.TYPE_XXX
     * @return true 支持 / false 不支持
     */
    fun isSensorSupported(context: Context, sensorType: Int): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(sensorType)
        return sensor != null
    }

    /**
     * 检查常用传感器支持情况
     * @param context 上下文
     * @return Map<传感器名称, 是否支持>
     */
    fun getCommonSensorStatus(context: Context): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        result["Rotation Vector"] = isSensorSupported(context, Sensor.TYPE_ROTATION_VECTOR)
        result["Linear Acceleration"] = isSensorSupported(context, Sensor.TYPE_LINEAR_ACCELERATION)
        result["Step Detector"] = isSensorSupported(context, Sensor.TYPE_STEP_DETECTOR)
        result["Pressure"] = isSensorSupported(context, Sensor.TYPE_PRESSURE)
        return result
    }

    /**
     * 直接返回 STEP_DETECTOR 是否支持
     */
    fun isStepDetectorSupported(context: Context): Boolean {
        return isSensorSupported(context, Sensor.TYPE_STEP_DETECTOR)
    }

    /**
     * 直接返回 ROTATION_VECTOR 是否支持
     */
    fun isRotationVectorSupported(context: Context): Boolean {
        return isSensorSupported(context, Sensor.TYPE_ROTATION_VECTOR)
    }

    /**
     * 直接返回 LINEAR_ACCELERATION 是否支持
     */
    fun isLinearAccelerationSupported(context: Context): Boolean {
        return isSensorSupported(context, Sensor.TYPE_LINEAR_ACCELERATION)
    }

    /**
     * 直接返回 PRESSURE 是否支持
     */
    fun isPressureSupported(context: Context): Boolean {
        return isSensorSupported(context, Sensor.TYPE_PRESSURE)
    }
}
