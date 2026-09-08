package com.chronie.homemoneylite.core.common

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 应用内语言管理：跟随系统 / 简体中文 / English。
 *
 * 实现说明：
 * - 项目已移除 AppCompat（无 AppCompatDelegate.setApplicationLocales），因此通过
 *   createConfigurationContext 包装 Context 生效，minSdk 23 亦可用；
 * - Android 13+ 同步写入系统 LocaleManager（android.app.LocaleManager），
 *   使系统「应用语言」设置与应用内选择一致；
 * - 选择结果持久化在 app_language_prefs，冷启动时由 Application / Activity 的
 *   attachBaseContext 读取。
 */
object AppLanguageManager {

    /** 跟随系统语言 */
    const val FOLLOW_SYSTEM = "system"

    /** 简体中文 */
    const val ZH = "zh"

    /** English */
    const val EN = "en"

    private const val PREFS_NAME = "app_language_prefs"
    private const val KEY_LANGUAGE = "app_language"

    /** 语言选项顺序：跟随系统、简体中文、English */
    val OPTIONS: List<String> = listOf(FOLLOW_SYSTEM, ZH, EN)

    /** 当前持久化选择，默认跟随系统 */
    fun getPreference(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM

    /**
     * 保存语言选择。调用方保存后需自行重建 Activity 使界面刷新。
     */
    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, tag)
            .apply()

        // Android 13+：写入系统应用级语言，保证与系统设置双向一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager.applicationLocales =
                    if (tag == FOLLOW_SYSTEM) LocaleList.getEmptyLocaleList()
                    else LocaleList.forLanguageTags(tag)
            }
        }
    }

    /** 解析实际生效的语言 */
    fun resolveLocale(context: Context): Locale {
        val pref = getPreference(context)
        if (pref != FOLLOW_SYSTEM) return Locale.forLanguageTag(pref)

        // 跟随系统：Android 13+ 优先取系统为该应用单独指定的语言
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val appLocales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
                if (appLocales != null && !appLocales.isEmpty) return appLocales[0]
            }
        }
        return Locale.getDefault()
    }

    /** 用生效语言包装 Context，供 attachBaseContext 使用 */
    fun wrapContext(context: Context): Context {
        val locale = resolveLocale(context)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(config)
    }
}
