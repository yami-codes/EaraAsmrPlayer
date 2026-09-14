package com.asmr.player.data.llm

import com.google.gson.JsonArray
import com.google.gson.JsonParser

/**
 * Incrementally parses NDJSON subtitle translation lines from a stream.
 * Ported from LizuNemuri `lib/core/llm/streaming_translation_parser.dart`.
 */
class StreamingTranslationParser {
    private val buffer = StringBuilder()

    fun feed(chunk: String): List<Pair<Int, String>> {
        buffer.append(chunk)
        return drainCompleteLines(flushRemainder = false)
    }

    fun flush(): List<Pair<Int, String>> = drainCompleteLines(flushRemainder = true)

    private fun drainCompleteLines(flushRemainder: Boolean): List<Pair<Int, String>> {
        val text = buffer.toString()
        buffer.clear()

        val endsWithNewline = text.endsWith('\n')
        val parts = text.split('\n').toMutableList()

        var remainder: String? = null
        if (!endsWithNewline && parts.isNotEmpty() && !flushRemainder) {
            remainder = parts.removeAt(parts.lastIndex)
        }

        val results = mutableListOf<Pair<Int, String>>()
        for (raw in parts) {
            results += parseLine(raw)
        }

        if (flushRemainder && remainder == null && parts.isEmpty() && text.isNotEmpty()) {
            results += parseLine(text)
        } else if (flushRemainder && remainder != null) {
            results += parseLine(remainder)
        } else if (remainder != null) {
            buffer.append(remainder)
        }

        return results
    }

    private fun parseLine(raw: String): List<Pair<Int, String>> {
        var line = raw.trim()
        if (line.isEmpty()) return emptyList()
        if (line.startsWith("```")) return emptyList()

        val entry = parseObject(line)
        if (entry != null) return listOf(entry)

        if (line.startsWith("[")) {
            return runCatching {
                val decoded = JsonParser.parseString(line)
                if (decoded is JsonArray) {
                    decoded.mapNotNull { element ->
                        if (!element.isJsonObject) return@mapNotNull null
                        parseObjectMap(element.asJsonObject.entrySet().associate { (k, v) -> k to v })
                    }
                } else {
                    emptyList()
                }
            }.getOrDefault(emptyList())
        }

        return emptyList()
    }

    private fun parseObject(line: String): Pair<Int, String>? {
        return runCatching {
            val decoded = JsonParser.parseString(line)
            if (!decoded.isJsonObject) return null
            parseObjectMap(decoded.asJsonObject.entrySet().associate { (k, v) -> k to v })
        }.getOrNull()
    }

    private fun parseObjectMap(obj: Map<String, Any?>): Pair<Int, String>? {
        val indexValue = obj["index"]
        val textValue = obj["text"]
        val text = textValue?.toString()?.trim() ?: return null
        val index = when (indexValue) {
            is Number -> indexValue.toInt()
            is String -> indexValue.toIntOrNull()
            else -> null
        } ?: return null
        return index to text
    }
}
