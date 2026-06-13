package com.asmr.player.ui.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChromeLockStateTest {
    @Test
    fun loadingState_locksSearchChrome() {
        val lockState = resolveSearchChromeLockState(SearchUiState.Loading)

        assertTrue(lockState.filterControlsLocked)
        assertTrue(lockState.searchSubmitLocked)
        assertFalse(lockState.showSearchSpinner)
        assertFalse(lockState.interactionLocked)
    }

    @Test
    fun errorState_keepsSearchChromeAvailable() {
        val lockState = resolveSearchChromeLockState(SearchUiState.Error("timeout"))

        assertFalse(lockState.filterControlsLocked)
        assertFalse(lockState.searchSubmitLocked)
        assertFalse(lockState.showSearchSpinner)
        assertFalse(lockState.interactionLocked)
    }

    @Test
    fun busySuccessState_showsInlineSpinnerAndLocksActions() {
        val lockState = resolveSearchChromeLockState(
            SearchUiState.Success(
                results = emptyList(),
                keyword = "test",
                page = 1,
                order = SearchSortOption.Trend,
                collectedSort = SearchCollectedSortOption.ReleaseNew,
                purchasedOnly = false,
                presaleOnly = false,
                chineseTranslatedOnly = false,
                collectedOnly = true,
                locale = "ja_JP",
                canGoPrev = false,
                canGoNext = false,
                pendingRequest = SearchPendingRequest(
                    kind = SearchPendingRequestKind.Search,
                    targetPage = 1
                )
            )
        )

        assertTrue(lockState.filterControlsLocked)
        assertTrue(lockState.searchSubmitLocked)
        assertTrue(lockState.showSearchSpinner)
        assertTrue(lockState.interactionLocked)
    }
}
