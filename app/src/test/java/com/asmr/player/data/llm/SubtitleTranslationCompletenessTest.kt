package com.asmr.player.data.llm

import com.asmr.player.util.SubtitleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleTranslationCompletenessTest {
    private val source = listOf(
        SubtitleEntry(0, 1000, "original one"),
        SubtitleEntry(1000, 2000, "original two")
    )

    @Test
    fun isComplete_requiresAllLinesTranslated() {
        val partial = mapOf(0 to "translated one", 1 to "original two")
        assertFalse(SubtitleTranslationCompleteness.isComplete(source, partial))

        val complete = mapOf(0 to "translated one", 1 to "translated two")
        assertTrue(SubtitleTranslationCompleteness.isComplete(source, complete))
    }

    @Test
    fun pendingLines_skipsAlreadyTranslated() {
        val lines = mapOf(0 to "translated one", 1 to "original two")
        val pending = SubtitleTranslationCompleteness.pendingLines(source, lines)
        assertEquals(1, pending.size)
        assertEquals("original two", pending.first().text)
    }
}
