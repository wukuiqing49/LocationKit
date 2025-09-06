package com.wkq.base.activity

import android.view.LayoutInflater
import android.view.View
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
abstract class BasicVbActivity<VB : ViewBinding> : BasicActivity() {
    val binding: VB by lazy { createVB() }
    override fun getLayoutView(): View? {
        return binding.root
    }

    private fun createVB(): VB {
        val superclass = javaClass.genericSuperclass
        val klass = (superclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        if (klass == ViewBinding::class.java) {
            throw RuntimeException("${javaClass.simpleName}:ViewBinding必须有实现")
        }
        val method = klass.getDeclaredMethod("inflate", LayoutInflater::class.java)
        return method.invoke(null, LayoutInflater.from(this@BasicVbActivity)) as VB
    }
}