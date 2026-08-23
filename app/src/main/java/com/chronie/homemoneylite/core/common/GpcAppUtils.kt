package com.chronie.homemoneylite.core.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * 金猪币（GPC）App 安装检测与拉起工具。
 *
 * 关键修复点（历史 bug：用户已安装 GPC 却提示「未检测到」）：
 *  根因是 Android 11+（targetSdk 30+）的「包可见性」限制——未声明 <queries>
 *  时，getPackageInfo / resolveActivity 即便 GPC 已安装也会返回“未安装”。
 *  修复：在 AndroidManifest 的 <queries> 中声明 GPC 包名与 gpc:// intent；
 *  本工具优先用深链 resolveActivity 预检，失败再兜底启动 GPC 主 Activity。
 */
object GpcAppUtils {

    const val GPC_PACKAGE = "com.pig.coin"

    /** GPC 主 Activity（LAUNCHER），作为深链无法解析时的兜底启动目标。 */
    private const val GPC_MAIN_ACTIVITY = "$GPC_PACKAGE.MainActivity"

    fun isGpcInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GPC_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 尝试拉起 GPC App（深链优先，主 Activity 兜底）。
     *
     * 探测策略：优先用深链 resolveActivity 预检（manifest 已声明 <queries> 的
     * gpc:// intent，高版本 Android 上可见）；若深链不可见但包已安装，则兜底
     * 启动 GPC 主 Activity。两者都失败才判定为未安装，避免高版本包可见性限制
     * 造成的误报。
     *
     * @return true=已成功发起启动（GPC 已安装）；false=GPC 未安装，回调 [onNotInstalled]。
     */
    fun launchGpc(
        context: Context,
        deepLink: String,
        onNotInstalled: () -> Unit
    ): Boolean {
        val pm = context.packageManager
        val uri = Uri.parse(deepLink)

        // 1) 深链优先：不限定 package，让系统按 BROWSABLE 规则自由解析
        val deepIntent = Intent(Intent.ACTION_VIEW, uri)
        if (deepIntent.resolveActivity(pm) != null) {
            context.startActivity(deepIntent)
            return true
        }

        // 2) 深链不可见（部分 ROM 仍受限）但包已安装：兜底启动 GPC 主 Activity
        if (isGpcInstalled(context)) {
            val mainIntent = Intent(Intent.ACTION_VIEW).apply {
                setClassName(GPC_PACKAGE, GPC_MAIN_ACTIVITY)
            }
            if (mainIntent.resolveActivity(pm) != null) {
                try {
                    context.startActivity(mainIntent)
                    return true
                } catch (_: Exception) {
                    // setClassName 极少失败，落到未安装处理
                }
            }
        }

        // 3) 均失败 → 判定未安装
        onNotInstalled()
        return false
    }

    /**
     * 用系统浏览器打开 GPC OAuth 授权页（http/https URL）。
     *
     * 为什么用浏览器而不是拉起 GPC App：
     * GPC 授权成功后服务端会 302 重定向到 gpc://oauth/callback?code=...
     * 若授权页在 GPC App 内部 WebView 打开，该重定向会被 WebView 吞掉、
     * 无法逃逸到系统，hfm 收不到 code（现象：只打开 GPC 主页、绑定卡死）。
     * 用系统浏览器打开时，重定向发生在浏览器进程，浏览器遇到 gpc:// scheme
     * 会按 BROWSABLE intent-filter 把深链路由给 hfm 的 EolManageActivity，
     * 从而完整走通 OAuth 授权码回跳。
     *
     * @return true=已成功发起（浏览器可处理）；false=无浏览器可用。
     */
    fun openBrowserAuthorize(context: Context, url: String): Boolean {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(pm) != null) {
            context.startActivity(intent)
            return true
        }
        return false
    }
}
