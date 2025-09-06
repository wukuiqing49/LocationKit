package com.wkq.base.activity

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.widget.ImageViewCompat
import com.wkq.base.R


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicTitleActivity : BasicActivity() {

    protected lateinit var mFlContent: FrameLayout //内容容器
    protected lateinit var mIncludeTitlebar: View  //Titlebar根布局
    protected lateinit var mIvTitlebarBack: ImageView
    protected lateinit var mTvTitlebarTitle: TextView
    protected lateinit var mFlTitlebarEnd: FrameLayout
    protected lateinit var mTvTitlebarEnd: TextView
    protected lateinit var mIvTitlebarEnd: ImageView

    override fun getLayoutView(): View? {
        return View.inflate(this@BasicTitleActivity, R.layout.activity_base_titlebar, null)
    }

    override fun initView() {
        mFlContent = findViewById<FrameLayout>(R.id.mFlContent)
        mIncludeTitlebar = findViewById<View>(R.id.mIncludeTitle)
        mIvTitlebarBack = mIncludeTitlebar.findViewById<ImageView>(R.id.mIvTitlebarBack)
        mTvTitlebarTitle = mIncludeTitlebar.findViewById<TextView>(R.id.mTvTitlebarTitle)
        mFlTitlebarEnd = mIncludeTitlebar.findViewById<FrameLayout>(R.id.mFlTitlebarEnd)
        mTvTitlebarEnd = mIncludeTitlebar.findViewById<TextView>(R.id.mTvTitlebarEnd)
        mIvTitlebarEnd = mIncludeTitlebar.findViewById<ImageView>(R.id.mIvTitlebarEnd)

        mIvTitlebarBack.setOnClickListener { finish() }
        mFlContent.addView(createContentView())
    }

    abstract fun createContentView(): View

    /**
     * 标题栏与内容区是否重叠
     */
    fun setIsStacking(bl: Boolean): BasicTitleActivity {
        val layoutParams = mFlContent.layoutParams
        layoutParams.height = if (bl) ConstraintLayout.LayoutParams.MATCH_PARENT else 0
        mFlContent.layoutParams = layoutParams
        return this@BasicTitleActivity
    }

    /**
     * title颜色,返回键颜色： 白 or 黑
     */
    @SuppressLint("ResourceType")
    fun setTheme(isWhite: Boolean): BasicTitleActivity {
        mTvTitlebarTitle.setTextColor(
            if (isWhite) getString(R.color.white).toColorInt() else getString(
                R.color.black
            ).toColorInt()
        )
        ImageViewCompat.setImageTintList(
            mIvTitlebarBack,
            if (isWhite) ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)
            ) else ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))
        )
        return this@BasicTitleActivity
    }

    /**
     * title
     */
    fun setTitle(title: String): BasicTitleActivity {
        mTvTitlebarTitle.text = title
        return this@BasicTitleActivity
    }

    /**
     * endText
     */
    @SuppressLint("ResourceAsColor")
    fun setEndText(
        end: String, @ColorInt color: Int = 0, unit: (() -> Unit)?
    ): BasicTitleActivity {
        mTvTitlebarEnd.visibility = View.VISIBLE
        mTvTitlebarEnd.text = end
        if (color != 0) mTvTitlebarEnd.setTextColor(color)
        mFlTitlebarEnd.setOnClickListener {
            unit?.invoke()
        }
        return this@BasicTitleActivity
    }

    /**
     * endImage
     */
    fun setEndImg(@DrawableRes resId: Int, @ColorInt tintColor: Int? = null, unit: (() -> Unit)?): BasicTitleActivity {
        mIvTitlebarEnd.visibility = View.VISIBLE
        if(tintColor == null){
            mIvTitlebarEnd.setImageTintList(null)
            mIvTitlebarEnd.clearColorFilter()
        }else{
            mIvTitlebarEnd.setColorFilter(tintColor)
//            mIvTitlebarEnd.setImageTintList(ColorStateList.valueOf(tintColor))
        }
        mIvTitlebarEnd.setImageResource(resId)
        mFlTitlebarEnd.setOnClickListener {
            unit?.invoke()
        }
        return this@BasicTitleActivity
    }


    fun setContentBelowTitleBar(view:View) {
        view.post {
            view.setPadding(0, mIncludeTitlebar.height, 0, 0)
        }
    }

}