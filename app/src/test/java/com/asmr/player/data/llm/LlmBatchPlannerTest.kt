package com.asmr.player.data.llm

import com.asmr.player.util.SubtitleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmBatchPlannerTest {
    private val sample = (0 until 30).map { index ->
        SubtitleEntry(index * 1000L, index * 1000L + 500L, "line $index")
    }

    @Test
    fun planBatches_none_returnsSingleBatch() {
        val batches = LlmBatchPlanner.planBatches(
            subtitles = sample,
            mode = LlmBatchSplitMode.None,
            manualBatchSize = 5
        )
        assertEquals(1, batches.size)
        assertEquals(30, batches.first().size)
    }

    @Test
    fun planBatches_manual_splitsBySize() {
        val batches = LlmBatchPlanner.planBatches(
            subtitles = sample,
            mode = LlmBatchSplitMode.Manual,
            manualBatchSize = 10
        )
        assertEquals(3, batches.size)
        assertEquals(10, batches[0].size)
        assertEquals(10, batches[1].size)
        assertEquals(10, batches[2].size)
    }

    @Test
    fun planBatches_provider_respectsContextWindow() {
        val batches = LlmBatchPlanner.planBatches(
            subtitles = sample,
            mode = LlmBatchSplitMode.Provider,
            manualBatchSize = 25,
            providerContextTokens = 4096
        )
        assertTrue(batches.isNotEmpty())
        assertEquals(30, batches.sumOf { it.size })
    }
}
