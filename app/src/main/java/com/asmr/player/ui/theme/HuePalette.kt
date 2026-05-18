package com.asmr.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class HuePalette(
    val primary: Color,
    val primarySoft: Color,
    val primaryStrong: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onPrimaryContainer: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

