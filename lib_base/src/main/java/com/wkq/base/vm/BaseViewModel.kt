package com.wkq.base.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/4/22 10:18
 *
 *@Desc: ViewModel的基类
 */
abstract class BaseViewModel : ViewModel() {

    val error = MutableLiveData<String?>()

    fun showError(msg: String?) {
        msg?.let {
            error.value = msg
        }
    }

    abstract fun cancel()


}