package com.asmr.player.i18n

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.asmr.player.R

/**
 * Typed accessors for frequently used string resources.
 * Most UI strings are referenced directly via R.string.* after migration.
 */
object Strings {
    @Composable
    fun navLibrary() = stringResource(R.string.nav_library)

    @Composable
    fun navSearch() = stringResource(R.string.nav_search)

    @Composable
    fun navSettings() = stringResource(R.string.nav_settings)

    fun languageLabelRes(@StringRes key: String): Int = when (key) {
        "language_system" -> R.string.language_system
        "language_english" -> R.string.language_english
        "language_thai" -> R.string.language_thai
        "language_chinese_simplified" -> R.string.language_chinese_simplified
        else -> R.string.language_system
    }
}
