package com.asmr.player.i18n

import java.util.Locale

enum class AppLanguage(val wireValue: String, val displayNameResKey: String) {
    System("system", "language_system"),
    English("en", "language_english"),
    Thai("th", "language_thai"),
    ChineseSimplified("zh-CN", "language_chinese_simplified");

    fun toLocale(): Locale? = when (this) {
        System -> null
        English -> Locale.ENGLISH
        Thai -> Locale("th")
        ChineseSimplified -> Locale.SIMPLIFIED_CHINESE
    }

    companion object {
        fun fromWireValue(value: String?): AppLanguage =
            entries.firstOrNull { it.wireValue == value } ?: System
    }
}
