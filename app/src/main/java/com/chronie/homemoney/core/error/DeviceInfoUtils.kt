package com.chronie.homemoney.core.error

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale
import java.util.TimeZone

/**
 * 设备信息工具类
 * 提供获取设备信息的方法
 */
object DeviceInfoUtils {

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "deviceModel" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "osVersion" to Build.VERSION.RELEASE,
            "sdkVersion" to Build.VERSION.SDK_INT.toString(),
            "deviceLanguage" to Locale.getDefault().language,
            "timeZone" to TimeZone.getDefault().id
        )
    }

    /**
     * 获取应用版本信息
     */
    fun getAppVersion(context: Context): AppVersionInfo {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            AppVersionInfo(
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                }
            )
        } catch (e: PackageManager.NameNotFoundException) {
            AppVersionInfo(
                versionName = "Unknown",
                versionCode = "Unknown"
            )
        }
    }

    /**
     * 应用版本信息数据类
     */
    data class AppVersionInfo(
        val versionName: String,
        val versionCode: String
    )
}
