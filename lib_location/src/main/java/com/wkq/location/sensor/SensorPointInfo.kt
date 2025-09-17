package com.wkq.location.sensor

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/17 9:29
 *
 *@Desc:
 */
data class SensorPointInfo(val lat: Double, val lng: Double, val altitude: Float, val heading: Float,val speed: Float=0f)