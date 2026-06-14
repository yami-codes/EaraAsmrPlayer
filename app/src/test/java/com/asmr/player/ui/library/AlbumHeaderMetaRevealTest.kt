package com.asmr.player.ui.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumHeaderMetaRevealTest {
    @Test
    fun shouldExpandAlbumHeaderMetaReveal_expandsOnlineHintMeta() {
        assertTrue(
            shouldExpandAlbumHeaderMetaReveal(
                deferMetaRevealExpected = true,
                presentInitially = true
            )
        )
    }

    @Test
    fun shouldExpandAlbumHeaderMetaReveal_keepsLocalInitialMetaStable() {
        assertFalse(
            shouldExpandAlbumHeaderMetaReveal(
                deferMetaRevealExpected = false,
                presentInitially = true
            )
        )
    }

    @Test
    fun shouldExpandAlbumHeaderMetaReveal_expandsLateMeta() {
        assertTrue(
            shouldExpandAlbumHeaderMetaReveal(
                deferMetaRevealExpected = false,
                presentInitially = false
            )
        )
    }
}
