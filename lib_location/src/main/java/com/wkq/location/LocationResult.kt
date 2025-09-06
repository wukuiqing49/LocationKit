package com.wkq.location

import android.location.Location

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/5 10:40
 *
 *@Desc:
 */
data class LocationResult(
    val success: Boolean,
    val location: Location?,
    val msg: String
)


