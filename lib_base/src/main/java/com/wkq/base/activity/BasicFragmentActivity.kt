package com.wkq.base.activity

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.wkq.base.R


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicFragmentActivity : BasicActivity() {
    override fun getLayoutView(): View? {
        return View.inflate(this, R.layout.activity_basic_fragment, null)
    }

    override fun initData() {
        var fm: FragmentManager = supportFragmentManager
        val bt = fm.beginTransaction()
        bt.add(R.id.mFlContent, createFragment())
        bt.commit()
    }

    abstract fun createFragment(): Fragment
}