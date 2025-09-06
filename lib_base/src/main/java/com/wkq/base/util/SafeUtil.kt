package com.wkq.base.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.Fragment

import es.dmoral.toasty.Toasty
import kotlin.let
import kotlin.text.isNullOrEmpty

/**
 * @author yyf
 *
 */
fun Context.showToast(msg: String?) {
    if (this.isFinishing()) return
    if (!msg.isNullOrEmpty()) {
        Toasty.Config.getInstance()
            .allowQueue(false) // 可选（阻止多个吐司排队显示）
            .apply()
        Toasty.normal(this, msg).show()
    }
}

fun Fragment.showToast(msg: String?) {
    if (context.isFinishing()) return
    context?.let {
        if (!msg.isNullOrEmpty()) {
            Toasty.Config.getInstance()
                .allowQueue(false) // 可选（阻止多个吐司排队显示）
                .apply()
            Toasty.normal(it, msg).show()
        }
    }
}

fun View.showToast(msg: String?) {
    if (context.isFinishing()) return
    context?.let {
        if (!msg.isNullOrEmpty()) {
            Toasty.Config.getInstance()
                .allowQueue(false) // 可选（阻止多个吐司排队显示）
                .apply()
            Toasty.normal(it, msg).show()
        }
    }
}



/**
 * 安全判断 Context 关联的 Activity 是否处于无效状态（已销毁/即将销毁）
 * 同时处理 Context 为 null 或非 Activity 的情况
 * @return true：上下文无效（null/非Activity/Activity已销毁）；false：上下文有效（Activity正常存活）
 */
fun Context?.isFinishing(): Boolean {
    // 1. 处理 Context 为 null 的情况
    if (this == null) {
        return true
    }

    // 2. 从 Context 中获取最底层的 Activity（处理 ContextWrapper 包装的情况）
    val activity = this.unwrapActivity() ?: return true  // 非 Activity 上下文视为无效（如 Service/Application）

    // 3. 检查 Activity 是否处于销毁状态
    return activity.isFinishing || activity.isDestroyed
}

/**
 * 从 Context 中解析出底层的 Activity（处理 ContextWrapper 嵌套包装的情况）
 * @return 解析到的 Activity，若无法解析（非 Activity 类型）则返回 null
 */
private fun Context.unwrapActivity(): Activity? {
    var context = this
    // 循环解开 ContextWrapper 包装，直到找到 Activity 或无法继续解开
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null  // 非 Activity 类型的 Context（如 Application/Service）
}