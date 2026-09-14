package com.asmr.player.data.llm

import android.content.Context
import com.asmr.player.util.SubtitleEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for translated subtitle lines.
 * Ported from LizuNemuri `lib/core/llm/subtitle_translation_cache.dart`.
 */
@Singleton
class SubtitleTranslationCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private fun baseDir(): File {
        val dir = File(context.filesDir, SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun workDir(workId: String): File {
        val safeWork = workId.replace(Regex("[^\\w\\-]"), "_")
        val dir = File(baseDir(), safeWork)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sourceHash(lyrics: List<SubtitleEntry>): String {
        val payload = lyrics.joinToString("\n") { it.text }
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun load(
        workId: String,
        fileName: String,
        targetLang: String,
        hash: String
    ): Map<Int, String>? {
        val primary = primaryFile(workId, targetLang, hash)
        val fromPrimary = readLines(primary)
        if (fromPrimary != null) return fromPrimary

        val legacy = legacyFile(workId, fileName, targetLang, hash)
        val fromLegacy = readLines(legacy)
        if (fromLegacy != null) {
            writeLines(primary, fromLegacy)
            return fromLegacy
        }

        val workDirectory = workDir(workId)
        val safeLang = targetLang.replace(Regex("[^\\w\\-]"), "_")
        val suffix = "_${safeLang}_$hash.json"
        workDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(suffix)) {
                val lines = readLines(file)
                if (lines != null) {
                    writeLines(primary, lines)
                    return lines
                }
            }
        }
        return null
    }

    fun save(
        workId: String,
        fileName: String,
        targetLang: String,
        hash: String,
        lines: Map<Int, String>
    ) {
        val primary = primaryFile(workId, targetLang, hash)
        writeLines(primary, lines)
    }

    private fun primaryFile(workId: String, targetLang: String, hash: String): File {
        val safeLang = targetLang.replace(Regex("[^\\w\\-]"), "_")
        return File(workDir(workId), "${hash}_$safeLang.json")
    }

    private fun legacyFile(workId: String, fileName: String, targetLang: String, hash: String): File {
        val safeFile = fileName.replace(Regex("[^\\w.\\-]"), "_")
        val safeLang = targetLang.replace(Regex("[^\\w\\-]"), "_")
        return File(workDir(workId), "${safeFile}_${safeLang}_$hash.json")
    }

    private fun readLines(file: File): Map<Int, String>? {
        if (!file.exists()) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        val map = runCatching {
            gson.fromJson<Map<String, String>>(file.readText(), type)
        }.getOrNull() ?: return null
        val out = mutableMapOf<Int, String>()
        map.forEach { (key, value) ->
            key.toIntOrNull()?.let { out[it] = value }
        }
        return out.ifEmpty { null }
    }

    private fun writeLines(file: File, lines: Map<Int, String>) {
        val jsonMap = lines.mapKeys { it.key.toString() }
        file.writeText(gson.toJson(jsonMap))
    }

    companion object {
        private const val SUBDIR = "translated_subtitles"
    }
}
