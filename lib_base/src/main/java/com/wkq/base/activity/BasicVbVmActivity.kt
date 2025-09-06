package com.wkq.base.activity

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
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
abstract class BasicVbVmActivity<VB : ViewBinding, VM : ViewModel> : BasicActivity() {

    val binding: VB by lazy { createVB() }

    val mViewModel: VM? by lazy { createVM() }

    private fun createVB(): VB {
        
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        if (klass == ViewBinding::class.java) {
            throw RuntimeException("${javaClass.simpleName}:ViewBinding必须有实现")
        }
        val method = klass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        return method.invoke(null, LayoutInflater.from(this@BasicVbVmActivity)) as VB
    }

    private fun createVM(): VM? {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[1] as Class<*>
        if (!ViewModel::class.java.isAssignableFrom(klass)) return null

        @Suppress("UNCHECKED_CAST")
        return ViewModelProvider(this)[klass as Class<VM>]
    }

    override fun getLayoutView(): View? {
        return binding.root
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