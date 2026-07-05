package com.asmr.player.shared.i18n

enum class SharedAppLanguage(val wireValue: String) {
    System("system"),
    English("en"),
    Thai("th"),
    ChineseSimplified("zh-CN");

    companion object {
        fun fromWireValue(value: String?): SharedAppLanguage =
            entries.firstOrNull { it.wireValue == value } ?: System
    }
}
