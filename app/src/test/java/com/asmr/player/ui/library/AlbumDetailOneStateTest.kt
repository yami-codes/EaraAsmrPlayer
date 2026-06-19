package com.asmr.player.ui.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailOneStateTest {
    @Test
    fun shouldShowAsmrOneDirectoryLoading_dependsOnOneStateOnly() {
        assertTrue(
            shouldShowAsmrOneDirectoryLoading(
                isAwaitingAsmrOneLoad = true,
                isLoadingAsmrOne = false,
                hasAsmrOneTree = false,
                hasDirectoryBrowser = false
            )
        )
        assertTrue(
            shouldShowAsmrOneDirectoryLoading(
                isAwaitingAsmrOneLoad = false,
                isLoadingAsmrOne = false,
                hasAsmrOneTree = true,
                hasDirectoryBrowser = false
            )
        )
        assertFalse(
            shouldShowAsmrOneDirectoryLoading(
                isAwaitingAsmrOneLoad = false,
                isLoadingAsmrOne = false,
                hasAsmrOneTree = false,
                hasDirectoryBrowser = false
            )
        )
    }
}
