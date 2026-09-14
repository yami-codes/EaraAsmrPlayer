package com.asmr.player.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingTranslationParserTest {
    @Test
    fun feed_parsesNdjsonLinesIncrementally() {
        val parser = StreamingTranslationParser()
        val first = parser.feed("{\"index\":0,\"text\":\"hello\"}\n{\"index\":1,\"text\":\"")
        assertEquals(listOf(0 to "hello"), first)

        val second = parser.feed("world\"}\n")
        assertEquals(listOf(1 to "world"), second)
    }

    @Test
    fun flush_parsesTrailingLineWithoutNewline() {
        val parser = StreamingTranslationParser()
        parser.feed("{\"index\":2,\"text\":\"tail\"}")
        val flushed = parser.flush()
        assertEquals(listOf(2 to "tail"), flushed)
    }

    @Test
    fun feed_ignoresMarkdownFenceFragments() {
        val parser = StreamingTranslationParser()
        val result = parser.feed("```json\n")
        assertTrue(result.isEmpty())
    }
}
