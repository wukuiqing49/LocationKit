package com.wkq.base.util.pop

import android.content.Context
import android.graphics.Color
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.impl.LoadingPopupView

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/4/17 17:24
 *
 *@Desc: PopupView 工具类
 */
internal object PopupUtil {
    /**
     *创建中间弹框
     */
    fun createCenterPopupView(context: Context, centerPopupView: CenterPopupView) {
        dialogBuilder(context).asCustom(centerPopupView).show()
    }

    /**
     *创建底部弹框
     */
    fun createBottomPopupView(
        context: Context, bottomPopupView: BottomPopupView, hasShadowBg: Boolean? = true,
        isTouchOutside: Boolean? = true
    ) {
        dialogBuilder(context, hasShadowBg, isTouchOutside).asCustom(bottomPopupView).show()
    }


    /**
     *创建自定义配置的pop
     */
    fun createCustomPopupViewBuilder(context: Context): XPopup.Builder {
        return XPopup.Builder(context)
    }

    fun createLoading(context: Context, content: String): BasePopupView {
        return XPopup.Builder(context).dismissOnBackPressed(true).dismissOnTouchOutside(false)
            .isLightNavigationBar(false).asLoading(content, LoadingPopupView.Style.ProgressBar)
            .show();
    }

    /**
     *创建通用弹框
     */
    fun createCommonPopupView(
        context: Context, title: String, desc: String, sureText: String? = "",
        listener: CommonPopupListener? = null
    ):BasePopupView {
        return dialogBuilder(context)
            .asCustom(CommonPopupView(context, title, desc, sureText, listener))
            .show()
    }

    private fun dialogBuilder(
        context: Context, hasShadowBg: Boolean? = true, isTouchOutside: Boolean? = true
    ): XPopup.Builder {
        // 暗色字体对应亮色导航栏
//        val isLightStatusBar = if (context is Activity) ImmersionBar.with(
//            context
//        ).barParams.statusBarDarkFont else true
        return XPopup.Builder(context).dismissOnTouchOutside(isTouchOutside)
            .dismissOnBackPressed(true)
//            .moveUpToKeyboard(false)
            .hasShadowBg(hasShadowBg)  //
            .isViewMode(true).enableDrag(false)

            .navigationBarColor(
                Color.TRANSPARENT
            )
            // 和baseActivity设置的导航颜色统一
            .isLightStatusBar(true) // 默认修改状态栏为亮色，dialog有效
            .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
    }
}