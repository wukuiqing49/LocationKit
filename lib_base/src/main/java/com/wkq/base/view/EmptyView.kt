package com.wkq.base.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.wkq.base.R
import com.wkq.base.databinding.LayoutEmptyBinding

class EmptyView : RelativeLayout {
    private   var mContext: Context
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        mContext = context
        // 设置布局参数以实现居中显示
        val layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(CENTER_IN_PARENT, TRUE)
        this.layoutParams = layoutParams
        initView(context)
    }

    private   lateinit var binding: LayoutEmptyBinding
    private fun initView(context: Context) {
        binding = LayoutEmptyBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setEmpty(resId: Int?=R.mipmap.iv_empty_default,emptyText: String?="",textColor: Int?,textSize: Float?){
        setEmptyImage(resId)
        setEmptyText(emptyText)
        setEmptyTextColor(textColor)
        setEmptyTextSize(textSize)
    }



    fun setEmptyImage(resId: Int?) {
        binding.ivContent.setImageResource(resId!!)
    }

    fun setEmptyText(text: String?) {
        text?.let {
            binding.tvDesc.text = text
        }

    }

    fun setEmptyTextColor(color: Int?) {
        binding.tvDesc.setTextColor(color!!)
    }

    fun setEmptyTextSize(size: Float?) {
        binding.tvDesc.textSize = size!!
    }
}
