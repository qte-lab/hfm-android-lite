package com.chronie.homemoney.core.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    
    private val _currentLanguage = MutableStateFlow(getSavedLanguage())
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val localeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
                handleSystemLocaleChange()
            }
        }
    }

    init {
        applyLanguage(_currentLanguage.value)
        registerLocaleChangeReceiver()
    }

    fun setLanguage(language: Language) {
        prefs.edit {
            putString(KEY_LANGUAGE, language.code)
            putBoolean(KEY_LANGUAGE_SET_BY_USER, true)
        }
        _currentLanguage.value = language
        applyLanguage(language)
    }

    fun getLanguageFromSystemSettings(): Language {
        return Language.getSystemLanguage()
    }

    fun checkAndApplySystemLanguage() {
        val userSetLanguage = prefs.getBoolean(KEY_LANGUAGE_SET_BY_USER, false)
        
        if (!userSetLanguage) {
            val systemLanguage = getLanguageFromSystemSettings()
            if (systemLanguage != _currentLanguage.value) {
                _currentLanguage.value = systemLanguage
                applyLanguage(systemLanguage)
            }
        }
    }

    private fun getSavedLanguage(): Language {
        val savedCode = prefs.getString(KEY_LANGUAGE, null)
        return if (savedCode != null) {
            Language.fromCode(savedCode)
        } else {
            getLanguageFromSystemSettings()
        }
    }

    private fun applyLanguage(language: Language) {
        val locale = language.locale
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun handleSystemLocaleChange() {
        val userSetLanguage = prefs.getBoolean(KEY_LANGUAGE_SET_BY_USER, false)
        
        if (!userSetLanguage) {
            val systemLanguage = getLanguageFromSystemSettings()
            if (systemLanguage != _currentLanguage.value) {
                _currentLanguage.value = systemLanguage
                applyLanguage(systemLanguage)
            }
        }
    }

    private fun registerLocaleChangeReceiver() {
        val filter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        context.registerReceiver(localeChangeReceiver, filter)
    }

    fun migrateOldLanguageCode(oldCode: String?) {
        if (oldCode == null) return
        
        val newLanguage = when (oldCode) {
            "en" -> Language.ENGLISH
            "id" -> Language.INDONESIAN
            "ja" -> Language.JAPANESE
            "ko" -> Language.KOREAN
            "ms" -> Language.MALAY
            "th" -> Language.THAI
            "vi" -> Language.VIETNAMESE
            "zh-CN" -> Language.SIMPLIFIED_CHINESE
            "zh-HK" -> Language.TRADITIONAL_CHINESE_HONG_KONG
            "zh-MO" -> Language.TRADITIONAL_CHINESE_MACAU
            "zh-SG" -> Language.SIMPLIFIED_CHINESE_SINGAPORE
            "zh-TW" -> Language.TRADITIONAL_CHINESE_TAIWAN
            else -> return
        }
        
        setLanguage(newLanguage)
    }

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_LANGUAGE_SET_BY_USER = "language_set_by_user"
    }
}
