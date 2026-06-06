package com.chronie.homemoney.core.common

import java.util.Locale

enum class Language(
    val code: String,
    val englishName: String,
    val localName: String,
    val locale: Locale
) {
    ENGLISH("en-US", "English", "English", Locale.US),
    INDONESIAN("id-ID", "Indonesian", "Bahasa Indonesia", Locale.forLanguageTag("id-ID")),
    JAPANESE("ja-JP", "Japanese", "日本語", Locale.JAPAN),
    KOREAN("ko-KR", "Korean", "한국어", Locale.KOREA),
    MALAY("ms-MY", "Malay", "Bahasa Melayu", Locale.forLanguageTag("ms-MY")),
    SIMPLIFIED_CHINESE("zh-CN", "Simplified Chinese (Mainland China)", "简体中文（中国大陆）", Locale.SIMPLIFIED_CHINESE),
    SIMPLIFIED_CHINESE_SINGAPORE("zh-SG", "Simplified Chinese (Singapore)", "简体中文（新加坡）", Locale.Builder().setLanguage("zh").setRegion("SG").build()),
    THAI("th-TH", "Thai", "ไทย", Locale.forLanguageTag("th-TH")),
    TRADITIONAL_CHINESE_HONG_KONG("zh-HK", "Traditional Chinese (Hong Kong)", "繁體中文（香港）", Locale.Builder().setLanguage("zh").setRegion("HK").build()),
    TRADITIONAL_CHINESE_MACAU("zh-MO", "Traditional Chinese (Macau)", "繁體中文（澳門）", Locale.Builder().setLanguage("zh").setRegion("MO").build()),
    TRADITIONAL_CHINESE_TAIWAN("zh-TW", "Traditional Chinese (Taiwan)", "繁體中文（台灣）", Locale.Builder().setLanguage("zh").setRegion("TW").build()),
    VIETNAMESE("vi-VN", "Vietnamese", "Tiếng Việt", Locale.forLanguageTag("vi-VN"));
    val displayName: String
        get() = "$englishName / $localName"

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }

        fun fromLocale(locale: Locale): Language {
            return when {
                locale.language == "ja" -> JAPANESE
                locale.language == "ko" -> KOREAN
                locale.language == "vi" -> VIETNAMESE
                locale.language == "th" -> THAI
                locale.language == "id" -> INDONESIAN
                locale.language == "ms" -> MALAY
                locale.language == "zh" && locale.country == "TW" -> TRADITIONAL_CHINESE_TAIWAN
                locale.language == "zh" && locale.country == "HK" -> TRADITIONAL_CHINESE_HONG_KONG
                locale.language == "zh" && locale.country == "MO" -> TRADITIONAL_CHINESE_MACAU
                locale.language == "zh" && locale.country == "SG" -> SIMPLIFIED_CHINESE_SINGAPORE
                locale.language == "zh" -> SIMPLIFIED_CHINESE
                else -> ENGLISH
            }
        }

        fun getSystemLanguage(): Language {
            return fromLocale(Locale.getDefault())
        }
    }
}
