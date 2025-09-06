package com.wkq.locationkit
import android.content.Context
import android.content.Intent
import android.view.View
import kotlin.apply

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/6/9 11:31
 *
 *@Desc:
 */


/**
* 处理误触
 */
fun View.setSafeClickListener(interval: Long = 500L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > interval) {
            lastClickTime = now
            action(it)
        }
    }
}


fun Context.jumpToLoginByClassName(className: String) {
    try {
        val loginClass = Class.forName(className)
        val intent = Intent(this, loginClass).apply {
            flags= Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

