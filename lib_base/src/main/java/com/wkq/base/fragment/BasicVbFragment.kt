package com.wkq.base.fragment


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
abstract class BasicVbFragment<T : ViewBinding> : BasicFragment() {

    lateinit var binding: T

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

    override fun createView(
        inflater: LayoutInflater, group: ViewGroup?
    ): View? {
        binding = createVB(inflater, group)
        return binding.root
    }

}