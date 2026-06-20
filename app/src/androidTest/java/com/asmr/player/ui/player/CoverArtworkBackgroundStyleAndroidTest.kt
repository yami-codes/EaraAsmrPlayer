package com.asmr.player.ui.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverArtworkBackgroundStyleAndroidTest {

    @Test
    fun clarity100_hasNoBlur_andLowerOverlayTint() {
        val lowClarity = coverArtworkBackdropStyle(clarity = 0.05f, isDark = true)
        val highClarity = coverArtworkBackdropStyle(clarity = 1f, isDark = true)

        assertEquals(0f, highClarity.blurDp.value, 0.001f)
        assertTrue(highClarity.overlayAlpha < lowClarity.overlayAlpha)
        assertTrue(highClarity.tintAlpha < lowClarity.tintAlpha)
        assertTrue(highClarity.scrimAlpha < lowClarity.scrimAlpha)
    }

    @Test
    fun artworkVisibility_increasesAsClarityIncreases() {
        val hiddenClarity = coverArtworkBackdropStyle(clarity = 0f, isDark = false)
        val lowClarity = coverArtworkBackdropStyle(clarity = 0.05f, isDark = false)
        val midClarity = coverArtworkBackdropStyle(clarity = 0.5f, isDark = false)
        val highClarity = coverArtworkBackdropStyle(clarity = 1f, isDark = false)

        assertEquals(0f, hiddenClarity.artworkAlpha, 0.0001f)
        assertTrue(lowClarity.artworkAlpha < midClarity.artworkAlpha)
        assertTrue(midClarity.artworkAlpha < highClarity.artworkAlpha)
    }

    @Test
    fun zeroClarity_hidesArtworkButKeepsThemeBackdrop() {
        val style = coverArtworkBackdropStyle(clarity = 0f, isDark = true)

        assertEquals(0f, style.artworkAlpha, 0.0001f)
        assertTrue(style.baseTintAlpha >= 0.9f)
        assertTrue(style.tintAlpha >= 0.26f)
        assertTrue(style.overlayAlpha <= 0.04f)
        assertTrue(style.scrimAlpha <= 0.01f)
    }

    @Test
    fun lowClarity_keepsBackdropTintVisible_inDarkMode() {
        val style = coverArtworkBackdropStyle(clarity = 0.05f, isDark = true)

        assertTrue(style.baseTintAlpha >= 0.89f)
        assertTrue(style.scrimAlpha <= 0.01f)
        assertTrue(style.tintAlpha >= 0.25f)
    }

    @Test
    fun lowClarity_avoidsWhiteWash_inLightMode() {
        val style = coverArtworkBackdropStyle(clarity = 0.05f, isDark = false)

        assertTrue(style.baseTintAlpha >= 0.81f)
        assertTrue(style.overlayAlpha <= 0.02f)
        assertEquals(0f, style.scrimAlpha, 0.0001f)
    }
}
