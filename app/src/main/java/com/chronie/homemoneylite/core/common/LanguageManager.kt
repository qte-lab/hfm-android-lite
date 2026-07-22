package com.chronie.homemoneylite.core.common

import android.content.Context
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

    init {
        applyLanguage(_currentLanguage.value)
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

    fun migrateOldLanguageCode(oldCode: String?) {
        if (oldCode == null) return

        if (oldCode == "zh") {
            setLanguage(Language.CHINESE)
        }
    }

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_LANGUAGE_SET_BY_USER = "language_set_by_user"
    }
}