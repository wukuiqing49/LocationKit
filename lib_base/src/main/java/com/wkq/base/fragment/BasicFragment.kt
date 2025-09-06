package com.wkq.base.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicFragment : BasePermissionFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return createView(inflater, container)
    }

    abstract fun createView(
        inflater: LayoutInflater, group: ViewGroup?
    ): View?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    abstract fun initView()
    abstract fun initData()

}