package com.chronie.homemoneylite.core.common

import java.util.Locale

enum class Language(
    val code: String,
    val englishName: String,
    val localName: String,
    val locale: Locale
) {
    CHINESE("zh-CN", "Chinese", "中文", Locale.SIMPLIFIED_CHINESE);

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: CHINESE
        }

        fun getSystemLanguage(): Language {
            return CHINESE
        }
    }
}
