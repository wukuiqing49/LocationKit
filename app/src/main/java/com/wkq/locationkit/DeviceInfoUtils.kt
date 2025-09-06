package com.wkq.locationkit
import android.content.Context
import android.os.Build
import com.blankj.utilcode.util.DeviceUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.text.contains
import kotlin.text.format
import kotlin.text.lowercase


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/8/28 13:31
 *
 *@Desc:
 */


object DeviceInfoUtils {
    // 系统属性常量
    private const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"
    private const val KEY_EMUI_VERSION = "ro.build.version.emui"
    private const val KEY_COLOROS_VERSION = "ro.build.version.opporom"
    private const val KEY_ORIGINOS_VERSION = "ro.vivo.os.version"
    private const val KEY_ONEUI_VERSION = "ro.build.version.oneui"
    private const val KEY_MAGUI_VERSION = "ro.build.version.magui"

    /**
     * 获取设备品牌
     */
    fun getBrand(): String? {
        return Build.BRAND
    }

    /**
     * 获取设备型号
     */
    fun getModel(): String? {
        return Build.MODEL
    }

    /**
     * 获取Android系统版本号（如12、13）
     */
    fun getAndroidVersion(): String? {
        return Build.VERSION.RELEASE
    }

    /**
     * 获取Android SDK版本号
     */
    fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    /**
     * 获取系统名称（如MIUI、EMUI等）
     */
    fun getSystemName(): String {
        try {
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
            val brand = Build.BRAND.lowercase(Locale.getDefault())

            // 小米 MIUI
            if (manufacturer.contains("xiaomi") || brand.contains("xiaomi")) {
                val version = getProp("ro.miui.ui.version.name")
                if (version != null) return "MIUI"
            }
            // 华为 EMUI
            if (manufacturer.contains("huawei") || brand.contains("huawei")) {
                val version = getProp("ro.build.version.emui")
                if (version != null) return "EMUI"
            }
            // 荣耀 Magic UI
            if (manufacturer.contains("honor") || brand.contains("honor")) {
                val version = getProp("ro.build.version.magic")
                if (version != null) return "Magic UI"
            }
            // OPPO ColorOS
            if (manufacturer.contains("oppo") || brand.contains("oppo")) {
                val version = getProp("ro.build.version.opporom")
                if (version != null) return "ColorOS"
            }
            // Vivo OriginOS
            if (manufacturer.contains("vivo") || brand.contains("vivo")) {
                val version = getProp("ro.vivo.os.version")
                if (version != null) return "OriginOS"
            }
            // 三星 OneUI
            if (manufacturer.contains("samsung")) {
                val version = getProp("ro.build.version.oneui")
                if (version != null) return "OneUI"
            }
            // 魅族 Flyme
            if (manufacturer.contains("meizu")) {
                val version = getProp("ro.build.display.id")
                if (version != null) return "Flyme"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // 默认 Android
        return "Android"
    }

    /**
     * 获取系统版本信息，尽可能兼容主流 ROM
     */
    fun getSafeSystemVersion(): String? {
        try {
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
            val brand = Build.BRAND.lowercase(Locale.getDefault())
            val display = Build.DISPLAY
            val release = if (Build.VERSION.RELEASE != null) Build.VERSION.RELEASE else "未知"

            if (manufacturer.contains("xiaomi") || brand.contains("xiaomi")) {
                val version = getProp("ro.miui.ui.version.name")
                return if (version != null) version else display
            } else if (manufacturer.contains("huawei") || brand.contains("huawei")) {
                val version = getProp("ro.build.version.emui")
                return if (version != null) version else display
            } else if (manufacturer.contains("honor") || brand.contains("honor")) {
                val version = getProp("ro.build.version.magic")
                return if (version != null) version else display
            } else if (manufacturer.contains("oppo") || brand.contains("oppo")) {
                val version = getProp("ro.build.version.opporom")
                return if (version != null) version else display
            } else if (manufacturer.contains("vivo") || brand.contains("vivo")) {
                val version = getProp("ro.vivo.os.version")
                return if (version != null) version else display
            } else if (manufacturer.contains("samsung")) {
                val version = getProp("ro.build.version.oneui")
                return if (version != null) version else display
            } else if (manufacturer.contains("meizu")) {
                val version = getProp("ro.build.display.id")
                return if (version != null) version else display
            } else {
                // 未知 ROM，返回 Android 系统版本号
                return "Android " + release
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "未知系统"
        }
    }

    /**
     * 获取系统属性
     */
    private fun getProp(propName: String?): String? {
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop " + propName)
            input = BufferedReader(InputStreamReader(p.getInputStream()))
            return input.readLine()
        } catch (e: Exception) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    /**
     * 获取完整的设备信息
     */
    fun getFullDeviceInfo(): String {
        return String.format(
            "品牌: %s\n型号: %s\n系统: %s %s\nAndroid版本: %s\nSDK版本: %d",
            getBrand(),
            getModel(),
            getSystemName(),
            getSafeSystemVersion(),
            getAndroidVersion(),
            getSdkVersion()
        )
    }


       fun getPhoneInfo(
        context: Context
    ): String {
        val sb = StringBuilder()


//
//        try {
//            // ---------------- 应用信息 ----------------
//            sb.append("=== 应用信息 ===\n")
//            val packageInfo = runCatching {
//                context.packageManager.getPackageInfo(
//                    context.packageName, 0
//                )
//            }.getOrNull()
//            sb.append("应用版本: ${packageInfo?.versionName ?: "未知"}\n")
//            sb.append("应用包名: ${context.packageName}\n\n")
//        } catch (_: Exception) {
//            sb.append("获取应用信息失败\n\n")
//        }

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
