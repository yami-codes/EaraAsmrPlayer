package com.asmr.player.data.llm

import com.asmr.player.util.SubtitleEntry

/**
 * Computes subtitle batches for LLM translation requests.
 * Ported from LizuNemuri `lib/core/llm/llm_batch_planner.dart`.
 */
object LlmBatchPlanner {
    const val DEFAULT_MANUAL_BATCH_SIZE = 25
    private const val DEFAULT_CONTEXT_TOKENS = 128_000
    private const val CHARS_PER_TOKEN = 3.5
    private const val USABLE_CONTEXT_FRACTION = 0.35

    fun planBatches(
        subtitles: List<SubtitleEntry>,
        mode: LlmBatchSplitMode,
        manualBatchSize: Int,
        providerContextTokens: Int? = null
    ): List<List<SubtitleEntry>> {
        if (subtitles.isEmpty()) return emptyList()
        return when (mode) {
            LlmBatchSplitMode.None -> listOf(subtitles.toList())
            LlmBatchSplitMode.Manual -> chunk(subtitles, manualBatchSize.coerceIn(1, 500))
            LlmBatchSplitMode.Provider -> {
                val context = providerContextTokens ?: DEFAULT_CONTEXT_TOKENS
                val size = estimateBatchSize(subtitles, context)
                chunk(subtitles, size)
            }
        }
    }

    private fun estimateBatchSize(subtitles: List<SubtitleEntry>, contextTokens: Int): Int {
        val tokenBudget = (contextTokens * USABLE_CONTEXT_FRACTION).toInt()
        if (tokenBudget <= 0) return DEFAULT_MANUAL_BATCH_SIZE

        val sample = subtitles.take(20)
        val avgChars = if (sample.isEmpty()) {
            40.0
        } else {
            sample.map { it.text.length }.average()
        }

        val tokensPerLine = ((avgChars * 2) / CHARS_PER_TOKEN).toInt().coerceIn(8, 200)
        val batch = tokenBudget / tokensPerLine
        return batch.coerceIn(5, subtitles.size)
    }

    private fun chunk(items: List<SubtitleEntry>, size: Int): List<List<SubtitleEntry>> {
        val batches = mutableListOf<List<SubtitleEntry>>()
        var index = 0
        while (index < items.size) {
            val end = (index + size).coerceAtMost(items.size)
            batches += items.subList(index, end)
            index = end
        }
        return batches
    }
}
