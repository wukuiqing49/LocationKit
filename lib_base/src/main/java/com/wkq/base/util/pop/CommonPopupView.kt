package com.wkq.base.util.pop

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout

import com.lxj.xpopup.core.CenterPopupView
import com.wkq.base.databinding.LayoutCommonPopBinding



/**
 *
 *@Author: wkq
 *
 *@Time: 2025/4/18 14:13
 *
 *@Desc:
 */
internal class CommonPopupView(
   var  mContext: Context, var title: String, var desc: String, var sureText: String?="",
    var listener: CommonPopupListener?=null
) : CenterPopupView(mContext) {

    var binding = LayoutCommonPopBinding.inflate(LayoutInflater.from(context))

    /*重写：布局*/
    override fun addInnerContent() {
        val popupWidth = (getScreenWidth() * 350f / 960f).toInt()

        val params = FrameLayout.LayoutParams(popupWidth, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER

        centerPopupContainer.addView(binding.root, params)
    }

    fun getScreenWidth(): Int {
        val wm = mContext
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        if (wm == null) return -1
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point)
        } else {
            wm.getDefaultDisplay().getSize(point)
        }
        return point.x
    }

    override fun onCreate() {
        super.onCreate()
        initView()
    }

    private fun initView() {
        binding.tvTitle.text = title
        binding.tvContent.text = desc

        if (TextUtils.isEmpty(sureText)){
            binding.tvRight.text = sureText
        }
        binding.tvRight.setOnClickListener {
            listener?.let {
                dismiss()
                it.sureClick()
            }
        }
        binding.tvLeft.setOnClickListener {

            listener?.let {
                it.cancelClick()
            }
            dismiss()
        }
    }

}