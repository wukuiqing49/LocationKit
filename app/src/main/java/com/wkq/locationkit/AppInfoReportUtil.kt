package com.wkq.locationkit

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.DeviceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 *
 *@Author: wkq
 *
 *@Time: 2025/7/29 10:37
 *
 *@Desc: App 信息上报工具类
 */
object AppInfoReportUtil {

    // 全局独立作用域，独立于页面生命周期
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 上传设备 + 异常信息
     * type: 异常类型
     * content: 异常内容
     */
    fun reportVirtualLog(context: Context, type: String, content: String) {
        scope.launch {
            try {
                // 生成完整设备报告
                val formatContent = getSafeFullDeviceReport(context, type, content)

                // 上传到后台

            } catch (e: Exception) {
                e.printStackTrace() // 捕获整个流程异常，保证安全
                val message = e?.message ?: ""
                Log.e("虚拟空间检测异常:", message)
            }
        }
    }

 private   fun getSafeFullDeviceReport(
        context: Context, errorType: String? = null, errorContent: String? = null,userContent: String? = null
    ): String {
        val sb = StringBuilder()

        try {
            // ---------------- 异常信息 ----------------
            sb.append("=== 异常信息 ===\n")
            sb.append("类型: ${errorType ?: "无"}\n")
            sb.append("内容: ${errorContent ?: "无"}\n\n")
        } catch (_: Exception) {
            sb.append("获取异常信息失败\n\n")
        }

        try {
            // ---------------- 用户信息 ----------------
            sb.append("=== 用户信息 ===\n")

            sb.append("用户信息: ${userContent?: "未知"}\n")

        } catch (_: Exception) {
            sb.append("获取用户信息失败\n\n")
        }

        try {
            // ---------------- 应用信息 ----------------
            sb.append("=== 应用信息 ===\n")
            val packageInfo = runCatching {
                context.packageManager.getPackageInfo(
                    context.packageName, 0
                )
            }.getOrNull()
            sb.append("应用版本: ${packageInfo?.versionName ?: "未知"}\n")
            sb.append("应用包名: ${context.packageName}\n\n")
        } catch (_: Exception) {
            sb.append("获取应用信息失败\n\n")
        }

        try {
            // ---------------- 手机基本信息 ----------------
            sb.append("=== 手机基本信息 ===\n")
            sb.append(
                "品牌: ${runCatching { DeviceUtils.getManufacturer() }.getOrDefault("未知")}\n"
            )
            sb.append("型号: ${runCatching { DeviceUtils.getModel() }.getOrDefault("未知")}\n")
            sb.append(
                "CPU ABI: ${
                    runCatching { DeviceUtils.getABIs()?.joinToString() }.getOrDefault(
                        "未知"
                    )
                }\n"
            )
            sb.append("是否平板: ${runCatching { DeviceUtils.isTablet() }.getOrDefault(false)}\n")
            sb.append(
                "Android版本: ${
                    runCatching { DeviceUtils.getSDKVersionName() }.getOrDefault(
                        "未知"
                    )
                }\n"
            )
            sb.append(
                "SDK版本号: ${runCatching { DeviceUtils.getSDKVersionCode() }.getOrDefault(-1)}\n"
            )
            sb.append(
                "是否模拟器: ${runCatching { DeviceUtils.isEmulator() }.getOrDefault(false)}\n"
            )
            sb.append(
                "是否Root: ${runCatching { DeviceUtils.isDeviceRooted() }.getOrDefault(false)}\n"
            )
            sb.append(
                "ADB是否开启: ${
                    runCatching { DeviceUtils.isAdbEnabled() }.getOrDefault(
                        "未知"
                    )
                }\n\n"
            )
        } catch (_: Exception) {
            sb.append("获取手机信息失败\n\n")
        }



        try {
            // ---------------- 唯一设备标识 ----------------
            sb.append("=== 唯一设备标识 ===\n")
            sb.append(
                "唯一设备ID: ${
                    runCatching { DeviceUtils.getUniqueDeviceId() }.getOrDefault(
                        "未知"
                    )
                }\n"
            )
        } catch (_: Exception) {
            sb.append("获取唯一设备ID失败\n")
        }

        return sb.toString()
    }


}