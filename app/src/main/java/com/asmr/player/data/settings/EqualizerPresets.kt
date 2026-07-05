package com.asmr.player.data.settings

import androidx.annotation.StringRes
import com.asmr.player.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class AsmrPreset(
    val name: String,
    val bandLevels: List<Int>,
    val virtualizerStrength: Int = 0,
    val isCustom: Boolean = false
) {
    @StringRes
    fun labelResOrNull(): Int? = EqualizerPresets.labelResForName(name)

    fun displayLabel(fallback: String = name): String = name
}

object EqualizerPresets {
    private val gson = Gson()

    private val presetLabelResByName = mapOf(
        "default" to R.string.eq_preset_default,
        "whisper" to R.string.whisper,
        "deep" to R.string.ear_licking,
        "bright" to R.string.tapping_crisp,
        "soft" to R.string.tapping_dull,
        "custom" to R.string.custom,
        "默认" to R.string.eq_preset_default,
        "耳语增强" to R.string.whisper,
        "低沉氛围" to R.string.ear_licking,
        "明亮清晰" to R.string.tapping_crisp,
        "柔软包裹" to R.string.tapping_dull,
        "人声突出" to R.string.vocal_enhancement_vocal
    )

    fun labelResForName(name: String): Int? = presetLabelResByName[name]

    val DefaultPresets = listOf(
        AsmrPreset(
            name = "default",
            bandLevels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            virtualizerStrength = 0
        ),
        AsmrPreset(
            name = "whisper",
            bandLevels = listOf(-400, -300, -200, -100, 0, 200, 400, 700, 900, 800),
            virtualizerStrength = 200
        ),
        AsmrPreset(
            name = "deep",
            bandLevels = listOf(300, 250, 200, 150, 100, 0, -100, -250, -400, -500),
            virtualizerStrength = 500
        ),
        AsmrPreset(
            name = "bright",
            bandLevels = listOf(-300, -200, -100, 0, 150, 300, 650, 900, 1000, 800),
            virtualizerStrength = 100
        ),
        AsmrPreset(
            name = "soft",
            bandLevels = listOf(400, 500, 600, 400, 150, -200, -500, -800, -1100, -1200),
            virtualizerStrength = 150
        ),
        AsmrPreset(
            name = "vocal",
            bandLevels = listOf(-300, -200, -100, 100, 300, 700, 900, 650, 200, -150),
            virtualizerStrength = 0
        )
    )

    fun decodeCustomPresets(json: String?): List<AsmrPreset> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<AsmrPreset>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun encodeCustomPresets(presets: List<AsmrPreset>): String {
        return gson.toJson(presets)
    }
}
