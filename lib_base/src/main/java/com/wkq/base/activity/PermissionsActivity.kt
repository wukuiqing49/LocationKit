package com.wkq.base.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
open class PermissionsActivity : AppCompatActivity() {

    private var permissionType = -1
    private var permissionList = mutableListOf<String>()




    private val requestPermissionsLauncher = registerForActivityResult(
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (isPermissionPermanentlyDenied(entry.key)) {
                        permanentlyDeniedPermissions.add(entry.key)
                    }
                }

            }
        }
        if (deniedPermissions.isEmpty()) {
            authorized(permissionType, deniedPermissions)
        } else {
            if (permanentlyDeniedPermissions.isNotEmpty()) {
                PopupUtil.createCommonPopupView(
                    this, "权限申请", "以下权限被永久拒绝，是否前往设置页面进行授权？", "前往设置",
                    object : CommonPopupListener {
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
            var allPermissionsGranted = true
            for (permission in permissionList) {
                if (ContextCompat.checkSelfPermission(
                        this, permission
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
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        openSettingsLauncher.launch(intent)
    }

    /**
     * 判断权限是否被永久拒绝（用户勾选了"不再询问"）
     * @param permission 需要检查的权限
     * @return true: 永久拒绝，false: 临时拒绝或首次申请
     */
    private fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return !shouldShowRequestPermissionRationale(
            permission
        ) && ContextCompat.checkSelfPermission(
            this, permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    /**
     *获取权限
     */
    fun requestPermissionsLauncher(type: Int, permissionList: MutableList<String>) {
        permissionType = type
        this.permissionList = permissionList

        requestPermissionsLauncher.launch(permissionList.toTypedArray())
    }



    /**
     *是否拥有权限
     */

    fun isGranted(permissions: List<String>?): Boolean {
        // Android 6.0 以下无需运行时权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        // 空权限列表默认返回 false
        return permissions?.all { isGrantedOne(it) } ?: false
    }

    // 检查单个权限的辅助函数
    private fun isGrantedOne(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


    // 获取适配当前系统的媒体权限
    fun getMediaPermissions(): MutableList<String> {
        return if (Build.VERSION.SDK_INT >= 34) {
            // Android 14+ 需要全部三个权限
            mutableListOf<String>(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13-14 需要两个细分权限
            mutableListOf<String>(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // Android 12 及以下只需旧权限
            mutableListOf<String>(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }


    /**
     *已授权
     */
    open fun authorized(permissionType: Int, permissionList: MutableList<String>) {}

}


