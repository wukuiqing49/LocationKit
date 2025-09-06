package com.wkq.locationkit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.wkq.base.util.showToast

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/9/2 13:53
 *
 *@Desc:
 */
class LocationReceiver() : BroadcastReceiver() {

    var onLocation: ((Location?) -> Unit)? = null

    constructor(callback: (Location?) -> Unit) : this() {
        this.onLocation = callback
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val location = intent?.getParcelableExtra<Location>("extra_location")
        onLocation?.invoke(location)
        context?.showToast("LocationReceiver: $location")
    }
}