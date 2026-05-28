package com.asmr.player.ui.player

import com.asmr.player.listentogether.ListenTogetherStatus
import com.asmr.player.listentogether.ListenTogetherUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListenTogetherAudiencePresentationTest {

    @Test
    fun disabledStateHidesAudienceLine() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState()
        )

        assertNull(presentation)
    }

    @Test
    fun listenerCountExcludesCurrentUser() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState(
                available = true,
                listenerCount = 5,
                status = ListenTogetherStatus.Ready
            )
        )

        assertEquals(
            ListenTogetherAudiencePresentation.Audience(companionCount = 4),
            presentation
        )
    }

    @Test
    fun singleListenerShowsZeroCompanions() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState(
                available = true,
                listenerCount = 1,
                status = ListenTogetherStatus.Ready
            )
        )

        assertEquals(
            ListenTogetherAudiencePresentation.Audience(companionCount = 0),
            presentation
        )
    }

    @Test
    fun preparingStateFallsBackToSoloListening() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState(
                available = false,
                status = ListenTogetherStatus.Preparing
            )
        )

        assertNull(presentation)
    }

    @Test
    fun errorStateFallsBackToSoloListening() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState(
                available = true,
                status = ListenTogetherStatus.Error
            )
        )

        assertEquals(
            ListenTogetherAudiencePresentation.Audience(companionCount = 0),
            presentation
        )
    }

    @Test
    fun backendUnavailableFallsBackToSoloListening() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState(
                available = true,
                status = ListenTogetherStatus.BackendUnavailable
            )
        )

        assertEquals(
            ListenTogetherAudiencePresentation.Audience(companionCount = 0),
            presentation
        )
    }

    @Test
    fun onlineAudioShowsSkippedMessage() {
        val presentation = resolveListenTogetherAudiencePresentation(
            ListenTogetherUiState(
                available = false,
                status = ListenTogetherStatus.OnlineSkipped
            )
        )

        assertEquals(
            ListenTogetherAudiencePresentation.Status("在线音频不参与统计"),
            presentation
        )
    }
}
