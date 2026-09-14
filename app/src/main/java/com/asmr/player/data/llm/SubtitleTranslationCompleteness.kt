package com.asmr.player.data.llm

import com.asmr.player.util.SubtitleEntry

object SubtitleTranslationCompleteness {
    fun isLineTranslated(source: SubtitleEntry, translated: String?): Boolean {
        if (translated == null) return false
        val trimmed = translated.trim()
        if (trimmed.isEmpty()) return false
        return trimmed != source.text.trim()
    }

    fun translatedCount(source: List<SubtitleEntry>, lines: Map<Int, String>): Int {
        return source.indices.count { index ->
            isLineTranslated(source[index], lines[index])
        }
    }

    fun isComplete(source: List<SubtitleEntry>, lines: Map<Int, String>): Boolean {
        if (source.isEmpty()) return true
        return source.indices.all { index -> isLineTranslated(source[index], lines[index]) }
    }

    fun pendingLines(source: List<SubtitleEntry>, lines: Map<Int, String>): List<SubtitleEntry> {
        return source.filterIndexed { index, entry -> !isLineTranslated(entry, lines[index]) }
    }
}
