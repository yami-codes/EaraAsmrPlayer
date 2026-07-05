package com.asmr.player.util

import android.content.Context
import com.asmr.player.R

object AppErrorMessageFormatter {
    private val whitespaceRegex = Regex("""\s+""")
    private val rjSampleRegex = Regex("""[（(]\s*如\s*RJ\d{6,}\s*[)）]""", RegexOption.IGNORE_CASE)
    private val technicalDetailRegexes = listOf(
        Regex("""\bHTTP\s*\d{3}\b""", RegexOption.IGNORE_CASE),
        Regex("""\b[A-Za-z]+Exception\b"""),
        Regex("""\b[A-Z]{2,}(?:_[A-Z0-9]+)+\b"""),
        Regex("""\b(error\s*code|errorCode|status\s*code|code)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(sockettimeout|timeout|timed out|ioexception|illegalstate|unknownhost|ssl|eof|cancelled|canceled|failed)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bRJ\d{6,}\b""", RegexOption.IGNORE_CASE)
    )

    fun sanitize(rawMessage: String, context: Context): String {
        val fallback = context.getString(R.string.error_operation_failed)
        return sanitize(rawMessage, fallback)
    }

    fun sanitize(rawMessage: String, fallback: String): String {
        val normalized = rawMessage
            .replace(whitespaceRegex, " ")
            .replace(rjSampleRegex, "")
            .trim()
            .trimEnd('：', ':')

        if (normalized.isBlank()) return fallback

        knownTechnicalMessage(normalized, fallback)?.let { return it }

        if (hasTechnicalDetails(normalized)) {
            inferCategory(normalized, fallback)?.let { return it }
            return fallback
        }

        return normalized
    }

    private fun knownTechnicalMessage(message: String, fallback: String): String? {
        val lower = message.lowercase()
        return when {
            ".m3u8" in lower -> fallback
            "401" in lower || "认证" in message || "登录" in message && ("失败" in message || "过期" in message) -> fallback
            "403" in lower || "访问被拒绝" in message -> fallback
            "404" in lower || "not found" in lower -> fallback
            "429" in lower -> fallback
            "500" in lower || "502" in lower || "503" in lower || "504" in lower -> fallback
            "timeout" in lower || "timed out" in lower -> fallback
            "unknownhost" in lower || "network" in lower || "ioexception" in lower || "网络" in message -> fallback
            else -> null
        }
    }

    private fun inferCategory(message: String, fallback: String): String? = fallback

    private fun hasTechnicalDetails(message: String): Boolean {
        return technicalDetailRegexes.any { it.containsMatchIn(message) } || !containsOnlyReadableChinese(message)
    }

    private fun containsOnlyReadableChinese(message: String): Boolean {
        val stripped = message
            .replace(Regex("""[，。！？；：“”‘’（）()、/\\\-+*#@]"""), "")
            .replace(Regex("""\d+"""), "")
            .trim()
        if (stripped.isBlank()) return false
        return stripped.all { it.code in 0x4E00..0x9FFF || it.isWhitespace() }
    }
}
