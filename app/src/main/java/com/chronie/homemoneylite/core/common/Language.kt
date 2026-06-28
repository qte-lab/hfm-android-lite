package com.chronie.homemoneylite.core.common

import java.util.Locale

enum class Language(
    val code: String,
    val englishName: String,
    val localName: String,
    val locale: Locale
) {
    ENGLISH("en-US", "English", "English", Locale.US),
    SIMPLIFIED_CHINESE("zh-CN", "Simplified Chinese", "简体中文", Locale.SIMPLIFIED_CHINESE),
    TRADITIONAL_CHINESE("zh-TW", "Traditional Chinese", "繁體中文", Locale.TRADITIONAL_CHINESE),;
    val displayName: String
        get() = "$englishName / $localName"

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }

        fun fromLocale(locale: Locale): Language {
            return when {
                locale.language == "zh" && locale.country == "TW" -> TRADITIONAL_CHINESE
                locale.language == "zh" && locale.country == "HK" -> TRADITIONAL_CHINESE
                locale.language == "zh" && locale.country == "MO" -> TRADITIONAL_CHINESE
                locale.language == "zh" -> SIMPLIFIED_CHINESE
                else -> ENGLISH
            }
        }

        fun getSystemLanguage(): Language {
            return fromLocale(Locale.getDefault())
        }
    }
}
