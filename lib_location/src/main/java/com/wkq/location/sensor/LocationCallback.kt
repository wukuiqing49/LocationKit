package com.wkq.location.sensor

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/17 9:29
 *
 *@Desc:
 */
interface LocationCallback {
    // 经度 纬度 海拔高度, 航向(角度)
    fun onLocationUpdate(info :SensorPointInfo)

    /**
     * 设备传感器支持状态回调
     * @param sensors 每个传感器类型和是否支持
     */
    fun onSensorSupportStatus(sensors: List<SensorStatus>)
}