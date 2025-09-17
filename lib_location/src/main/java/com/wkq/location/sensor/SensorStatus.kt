package com.wkq.location.sensor

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/16 14:53
 *
 *@Desc: 单个传感器状态
 */

data class SensorStatus(
    val type: String,    // 传感器类型，如 "RotationVector", "StepDetector", "Pressure"
    val supported: Boolean
)
