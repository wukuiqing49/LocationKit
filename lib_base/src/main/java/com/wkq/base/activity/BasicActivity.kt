package com.wkq.base.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import com.gyf.immersionbar.ImmersionBar

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicActivity : PermissionsActivity() {

    var isFullScreen: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (isFullScreen){
//            ImmersionBar.with(this).transparentStatusBar().statusBarDarkFont(true).navigationBarColor(R.color.color_000000) .init()
//        }else{
//            ImmersionBar.with(this).statusBarDarkFont(setStatusBarDarkFont()).init()
//        }

        setContentView(getLayoutView())
        getWindow().getDecorView().post(Runnable {
            if (isFullScreen) {
                hideSystemUI()
            }

        })
        initView()
        initData()
    }


    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上用 WindowInsetsController
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 11 以下用 systemUiVisibility
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    fun isFullScreen(isFull: Boolean) {
        isFullScreen = isFull
    }

    abstract fun getLayoutView(): View?

    abstract fun initView()

    abstract fun initData()

    /**
     * 沉浸式状态栏开启-设置状态栏字体颜色是否为黑色
     * @return Boolean false:白字体 true:黑字体
     */
    open fun setStatusBarDarkFont(): Boolean {
        return false
    }

    /**
     * 内容位于状态栏下方
     */
    fun setContentBelowStatusBar() {
        val rootView = (findViewById<View?>(android.R.id.content) as ViewGroup).getChildAt(0)
        rootView.setPadding(0, ImmersionBar.getStatusBarHeight(this), 0, 0)
    }

}