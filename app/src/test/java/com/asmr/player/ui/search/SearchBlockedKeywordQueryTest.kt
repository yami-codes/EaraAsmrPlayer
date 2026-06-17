package com.asmr.player.ui.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchBlockedKeywordQueryTest {
    @Test
    fun appendBlockedKeywordsForOnlineSearch_appendsExclusionsAfterKeyword() {
        val query = appendBlockedKeywordsForOnlineSearch(
            keyword = "校园",
            blockedKeywords = listOf("猫娘", "言语侵犯")
        )

        assertEquals("校园 -猫娘 -言语侵犯", query)
    }

    @Test
    fun appendBlockedKeywordsForOnlineSearch_supportsBlankBaseKeyword() {
        val query = appendBlockedKeywordsForOnlineSearch(
            keyword = " ",
            blockedKeywords = listOf("猫娘")
        )

        assertEquals("-猫娘", query)
    }

    @Test
    fun appendBlockedKeywordsForOnlineSearch_trimsIgnoresBlankAndDeduplicates() {
        val query = appendBlockedKeywordsForOnlineSearch(
            keyword = "校园",
            blockedKeywords = listOf("  猫娘  ", "", "猫娘", "VOICE", "voice")
        )

        assertEquals("校园 -猫娘 -VOICE", query)
    }

    @Test
    fun appendBlockedKeywordsForOnlineSearch_normalizesLeadingMinus() {
        val query = appendBlockedKeywordsForOnlineSearch(
            keyword = "校园",
            blockedKeywords = listOf("-猫娘", "-", " - ")
        )

        assertEquals("校园 -猫娘", query)
    }
}
