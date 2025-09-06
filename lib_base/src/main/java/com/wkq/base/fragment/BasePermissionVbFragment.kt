package com.wkq.base.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.wkq.base.util.pop.CommonPopupListener
import com.wkq.base.util.pop.PopupUtil
import com.wkq.base.util.showToast


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/23 9:59
 *
 *@Desc:
 */
abstract class BasePermissionVbFragment<T : ViewBinding> : Fragment() {

    lateinit var mViewBinding: T
    private var permissionType = -1
    private var permissionList: Array<String> = arrayOf()

    protected val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedPermissions = mutableListOf<String>()
        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()

        permissions.entries.forEach { entry ->
            if (entry.value) {
                grantedPermissions.add(entry.key)
            } else {
                deniedPermissions.add(entry.key)
                if (!shouldShowRequestPermissionRationale(entry.key)) {
                    permanentlyDeniedPermissions.add(entry.key)
                }
            }
        }

        if (deniedPermissions.isEmpty()) {
            authorized(permissionType, deniedPermissions)
        } else {
            if (permanentlyDeniedPermissions.isNotEmpty()) {
                PopupUtil.createCommonPopupView(
                    requireActivity(), "权限申请", "以下权限被永久拒绝，是否前往设置页面进行授权？",
                    "前往设置", object : CommonPopupListener {
                        override fun sureClick() {
                            //前往系统设置
                            openAppSettings()
                        }
                        override fun cancelClick() {
                        }
                    })

            } else {
                showToast("部分权限被拒绝: ${deniedPermissions.joinToString()}")
            }
        }
    }

    private val openSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkPermissionsAfterSettingsReturn()
        }

    private fun checkPermissionsAfterSettingsReturn() {
        var allPermissionsGranted = true
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                allPermissionsGranted = false
                break
            }
        }
        if (allPermissionsGranted) {
            showToast("权限已在设置中开启")
        } else {
            showToast("权限仍未全部开启，请再次检查设置")
        }
    }

    private fun openAppSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        openSettingsLauncher.launch(intent)
    }

    // 获取权限
    fun requestPermissionsLauncher(type: Int, permissionList: Array<String>) {
        permissionType = type
        this.permissionList = permissionList
        requestPermissionsLauncher.launch(permissionList)
    }

    /**
     *已授权
     */
    abstract fun authorized(permissionType: Int, permissionList: MutableList<String>)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        mViewBinding = createViewBinding(inflater, container)
        return mViewBinding.root
    }

    abstract fun createViewBinding(inflater: LayoutInflater, group: ViewGroup?): T

}