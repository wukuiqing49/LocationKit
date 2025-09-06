package com.wkq.base.fragment


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType
/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasicVbVmFragment<T : ViewBinding, VM : ViewModel> : BasicFragment() {

    lateinit var binding: T

    val mViewModel: VM? by lazy { createVM() }

    private fun createVB(inflater: LayoutInflater, group: ViewGroup?): T {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[0] as Class<T>
        if (klass == ViewBinding::class.java) {
            throw RuntimeException("${javaClass.simpleName}:ViewBinding必须有实现")
        }
        val method = klass.getDeclaredMethod("inflate", LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return method.invoke(null, inflater, group, false) as T
    }

    private fun createVM(): VM? {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[1] as Class<VM>
        if (klass == ViewModel::class.java) return null
        return ViewModelProvider.NewInstanceFactory().create<VM>(klass) as VM
    }

    override fun createView(
        inflater: LayoutInflater, group: ViewGroup?
    ): View? {
        binding = createVB(inflater, group)
        return binding.root
    }

}