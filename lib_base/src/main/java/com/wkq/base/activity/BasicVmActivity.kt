package com.wkq.base.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wkq.base.vm.BaseViewModel

import java.lang.reflect.ParameterizedType

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicVmActivity<VM : ViewModel> : BasicActivity() {

    val mViewModel: VM? by lazy { createVM() }

    private fun createVM(): VM? {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[0] as Class<VM>
        if(klass == ViewModel::class.java) return null
        return ViewModelProvider.NewInstanceFactory().create<VM>(klass) //klass.newInstance()
    }
    override fun onDestroy() {
        super.onDestroy()
        mViewModel?.let {
            if (it is BaseViewModel) {
                it.cancel()
            }
        }
    }
}