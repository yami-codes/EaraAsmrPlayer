package com.asmr.player.ui.library

import com.asmr.player.data.remote.api.AsmrOneTrackNodeResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailAsmrOneBackendFirstTest {
    @Test
    fun fetchAsmrOneTracksBackendFirst_usesBackendTreeImmediately() = runBlocking {
        var fallbackCalled = false
        val backendTree = listOf(AsmrOneTrackNodeResponse(title = "backend.wav", mediaDownloadUrl = "https://example.com/backend.wav"))

        val result = fetchAsmrOneTracksBackendFirst(
            backendRjs = listOf("rj01271410"),
            fetchBackend = { rj -> "1271410" to backendTree.also { assertEquals("RJ01271410", rj) } },
            fetchFallback = {
                fallbackCalled = true
                Triple("fallback", null, emptyList())
            }
        )

        assertEquals("1271410", result.first)
        assertEquals(backendTree, result.third)
        assertFalse(fallbackCalled)
    }

    @Test
    fun fetchAsmrOneTracksBackendFirst_fallsBackWhenBackendFailsOrHasNoTree() = runBlocking {
        val requested = mutableListOf<String>()
        val fallbackTree = listOf(AsmrOneTrackNodeResponse(title = "fallback.wav", mediaDownloadUrl = "https://example.com/fallback.wav"))

        val result = fetchAsmrOneTracksBackendFirst(
            backendRjs = listOf("RJ01271410", "RJ01271411"),
            fetchBackend = { rj ->
                requested += rj
                if (rj == "RJ01271410") error("backend unavailable")
                "1271411" to emptyList()
            },
            fetchFallback = { Triple("fallback", 1, fallbackTree) }
        )

        assertEquals(listOf("RJ01271410", "RJ01271411"), requested)
        assertEquals("fallback", result.first)
        assertEquals(1, result.second)
        assertEquals(fallbackTree, result.third)
        assertTrue(result.third.isNotEmpty())
    }
}
