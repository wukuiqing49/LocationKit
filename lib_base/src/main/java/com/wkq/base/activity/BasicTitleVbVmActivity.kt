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
abstract class BasicTitleVbVmActivity<VB : ViewBinding, VM : ViewModel>() : BasicTitleActivity() {

    val binding: VB by lazy { createVB() }
    val mViewModel: VM? by lazy { createVM() }

    private fun createVB(): VB {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        if (klass == ViewBinding::class.java) {
            throw RuntimeException("${javaClass.simpleName}:ViewBinding必须有实现")
        }
        val method = klass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        return method.invoke(null, LayoutInflater.from(this@BasicTitleVbVmActivity)) as VB
    }

    private fun createVM(): VM? {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[1] as Class<VM>
        if (klass == ViewModel::class.java) return null
        return ViewModelProvider.NewInstanceFactory().create<VM>(klass) as VM
    }

    override fun createContentView(): View {
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