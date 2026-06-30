package com.chronie.homemoneylite.core.common

import java.util.Locale

enum class Language(
    val code: String,
    val englishName: String,
    val localName: String,
    val locale: Locale
) {
    ENGLISH("en-US", "English", "English", Locale.US),
    CHINESE("zh-CN", "Chinese", "中文", Locale.CHINESE);
    val displayName: String
        get() = "$englishName / $localName"

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }

        fun fromLocale(locale: Locale): Language {
            return when {
                locale.language == "zh" -> CHINESE
                else -> ENGLISH
            }
        }

        fun getSystemLanguage(): Language {
            return fromLocale(Locale.getDefault())
        }
    }
}
