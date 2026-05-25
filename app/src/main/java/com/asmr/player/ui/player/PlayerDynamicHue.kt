package com.asmr.player.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.MediaItem
import androidx.core.graphics.ColorUtils
import com.asmr.player.toThemeMediaSource
import com.asmr.player.ui.theme.AsmrColorScheme
import com.asmr.player.ui.theme.HuePalette
import com.asmr.player.ui.theme.ThemeMode
import com.asmr.player.ui.theme.deriveHuePalette
import com.asmr.player.ui.theme.neutralPaletteForMode
import com.asmr.player.ui.theme.rememberDynamicHuePalette
import com.asmr.player.ui.theme.rememberDynamicHuePaletteFromVideoFrame

@Composable
internal fun rememberPlayerDynamicHuePalette(
    mediaItem: MediaItem?,
    colorScheme: AsmrColorScheme,
    transitionDurationMs: Int = 1000,
    cachedTransitionDurationMs: Int = 260
): State<HuePalette> {
    val themeMediaSource = remember(mediaItem) { mediaItem.toThemeMediaSource() }
    val mode = colorScheme.mode
    val neutral = remember(mode) { neutralPaletteForMode(mode) }
    val fallbackHue = remember(
        colorScheme.primaryStrong,
        colorScheme.onPrimary,
        mode,
        neutral
    ) {
        deriveHuePalette(
            primary = colorScheme.primaryStrong,
            mode = mode,
            neutral = neutral,
            fallbackOnPrimary = colorScheme.onPrimary
        )
    }

    return if (themeMediaSource.isVideo && themeMediaSource.artworkUri == null) {
        rememberDynamicHuePaletteFromVideoFrame(
            videoUri = themeMediaSource.videoUri,
            mode = mode,
            neutral = neutral,
            fallbackHue = fallbackHue,
            transitionDurationMs = transitionDurationMs,
            cachedTransitionDurationMs = cachedTransitionDurationMs
        )
    } else {
        rememberDynamicHuePalette(
            artworkModel = themeMediaSource.artworkUri,
            mode = mode,
            neutral = neutral,
            fallbackHue = fallbackHue,
            transitionDurationMs = transitionDurationMs,
            cachedTransitionDurationMs = cachedTransitionDurationMs
        )
    }
}

internal data class PlayerThemeColors(
    val accentColor: Color,
    val onAccentColor: Color,
    val backdropTintColor: Color,
    val videoBackdropColor: Color
)

@Composable
internal fun rememberPlayerThemeColors(
    mediaItem: MediaItem?,
    colorScheme: AsmrColorScheme,
    coverBackgroundEnabled: Boolean,
    transitionDurationMs: Int = 1000,
    cachedTransitionDurationMs: Int = 260
): PlayerThemeColors {
    val dynamicHue by rememberPlayerDynamicHuePalette(
        mediaItem = mediaItem,
        colorScheme = colorScheme,
        transitionDurationMs = transitionDurationMs,
        cachedTransitionDurationMs = cachedTransitionDurationMs
    )

    return remember(dynamicHue, colorScheme, coverBackgroundEnabled) {
        resolvePlayerThemeColors(
            dynamicHue = dynamicHue,
            colorScheme = colorScheme,
            coverBackgroundEnabled = coverBackgroundEnabled
        )
    }
}

internal fun resolvePlayerThemeColors(
    dynamicHue: HuePalette,
    colorScheme: AsmrColorScheme,
    coverBackgroundEnabled: Boolean
): PlayerThemeColors {
    val accentColor = if (coverBackgroundEnabled) {
        dynamicHue.primaryStrong
    } else {
        colorScheme.primary
    }
    val onAccentColor = if (coverBackgroundEnabled) {
        dynamicHue.onPrimary
    } else {
        colorScheme.onPrimary
    }
    val backdropTintColor = if (coverBackgroundEnabled) {
        derivePlayerBackdropTintColor(
            primary = dynamicHue.primary,
            primarySoft = dynamicHue.primarySoft,
            background = colorScheme.background,
            mode = colorScheme.mode
        )
    } else {
        colorScheme.background
    }
    return PlayerThemeColors(
        accentColor = accentColor,
        onAccentColor = onAccentColor,
        backdropTintColor = backdropTintColor,
        videoBackdropColor = backdropTintColor
    )
}

internal fun derivePlayerBackdropTintColor(
    primary: Color,
    primarySoft: Color,
    background: Color,
    mode: ThemeMode
): Color {
    val seedMixed = Color(
        ColorUtils.blendARGB(
            primarySoft.toArgb(),
            primary.toArgb(),
            when (mode) {
                ThemeMode.Light -> 0.52f
                ThemeMode.Dark -> 0.72f
                ThemeMode.SoftDark -> 0.64f
            }
        )
    )
    val mixed = Color(
        ColorUtils.blendARGB(
            background.toArgb(),
            seedMixed.toArgb(),
            when (mode) {
                ThemeMode.Light -> 0.86f
                ThemeMode.Dark -> 0.88f
                ThemeMode.SoftDark -> 0.86f
            }
        )
    )
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(mixed.toArgb(), hsl)

    when (mode) {
        ThemeMode.Light -> {
            hsl[1] = hsl[1].coerceIn(0.22f, 0.56f)
            hsl[2] = hsl[2].coerceIn(0.66f, 0.78f)
        }
        ThemeMode.Dark -> {
            hsl[1] = hsl[1].coerceIn(0.24f, 0.58f)
            hsl[2] = hsl[2].coerceIn(0.42f, 0.56f)
        }
        ThemeMode.SoftDark -> {
            hsl[1] = hsl[1].coerceIn(0.22f, 0.52f)
            hsl[2] = hsl[2].coerceIn(0.46f, 0.60f)
        }
    }

    return Color(ColorUtils.HSLToColor(hsl))
}
