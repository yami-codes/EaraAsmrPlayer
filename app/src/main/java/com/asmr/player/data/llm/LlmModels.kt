package com.asmr.player.data.llm

import com.asmr.player.i18n.AppLanguage
import java.util.Locale

enum class LlmProviderKind {
    OpenRouter,
    Gemini,
    OpenAi,
    Custom;

    companion object {
        fun detect(endpoint: String): LlmProviderKind {
            val lower = endpoint.trim().lowercase()
            return when {
                lower.contains("openrouter.ai") -> OpenRouter
                lower.contains("generativelanguage.googleapis.com") -> Gemini
                lower.contains("api.openai.com") -> OpenAi
                else -> Custom
            }
        }
    }
}

enum class LlmSubtitleTargetLanguage(val wireValue: String) {
    System("system"),
    English("en"),
    Chinese("zh"),
    Japanese("ja"),
    Thai("th"),
    Korean("ko");

    fun resolveCode(appLanguage: AppLanguage): String {
        return when (this) {
            System -> {
                val locale = appLanguage.toLocale() ?: Locale.getDefault()
                when {
                    locale.language == "en" -> "en"
                    locale.language == "th" -> "th"
                    locale.language.startsWith("zh") -> "zh"
                    locale.language == "ja" -> "ja"
                    locale.language == "ko" -> "ko"
                    else -> "en"
                }
            }
            English -> "en"
            Chinese -> "zh"
            Japanese -> "ja"
            Thai -> "th"
            Korean -> "ko"
        }
    }

    companion object {
        fun fromWireValue(value: String?): LlmSubtitleTargetLanguage =
            entries.firstOrNull { it.wireValue == value } ?: System
    }
}

enum class LlmSubtitleDisplayMode(val wireValue: String) {
    TranslationOnly("translationOnly"),
    Dual("dual");

    companion object {
        fun fromWireValue(value: String?): LlmSubtitleDisplayMode =
            entries.firstOrNull { it.wireValue == value } ?: TranslationOnly
    }
}

enum class LlmBatchSplitMode(val wireValue: String) {
    None("none"),
    Provider("provider"),
    Manual("manual");

    companion object {
        fun fromWireValue(value: String?): LlmBatchSplitMode =
            entries.firstOrNull { it.wireValue == value } ?: Provider
    }
}

enum class SubtitleTranslationPhase {
    CheckingCache,
    Resuming,
    Translating,
    Saving,
    Cached,
    Done
}

data class SubtitleTranslationProgress(
    val phase: SubtitleTranslationPhase,
    val batchIndex: Int? = null,
    val batchTotal: Int? = null,
    val linesTranslated: Int = 0,
    val linesTotal: Int = 0
)

enum class LlmTranslationErrorType {
    MissingApiKey,
    InvalidConfig,
    Auth,
    RateLimited,
    ContentBlocked,
    InvalidResponse,
    Unknown
}

class LlmTranslationException(
    val type: LlmTranslationErrorType,
    override val message: String
) : Exception(message)

sealed class SubtitleTranslationResult {
    data class Success(
        val lyrics: List<TranslatedLyricLine>,
        val fromCache: Boolean = false
    ) : SubtitleTranslationResult()

    data class Partial(
        val lyrics: List<TranslatedLyricLine>,
        val translatedCount: Int,
        val totalCount: Int,
        val error: LlmTranslationException? = null
    ) : SubtitleTranslationResult()

    data class Skipped(
        val lyrics: List<TranslatedLyricLine>
    ) : SubtitleTranslationResult()

    data class Failure(
        val lyrics: List<TranslatedLyricLine>,
        val error: LlmTranslationException
    ) : SubtitleTranslationResult()
}

data class TranslatedLyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val originalText: String? = null
)

data class LlmSettings(
    val translationEnabled: Boolean = false,
    val apiEndpoint: String = LlmDefaults.OPEN_ROUTER_ENDPOINT,
    val mainModel: String = LlmDefaults.OPEN_ROUTER_MAIN_MODEL,
    val liteModel: String = LlmDefaults.OPEN_ROUTER_LITE_MODEL,
    val targetLanguage: LlmSubtitleTargetLanguage = LlmSubtitleTargetLanguage.System,
    val systemPromptOverride: String = "",
    val jailbreakPrompt: String = "",
    val jailbreakAuto: Boolean = true,
    val batchSplitMode: LlmBatchSplitMode = LlmBatchSplitMode.Provider,
    val manualBatchSize: Int = LlmBatchPlanner.DEFAULT_MANUAL_BATCH_SIZE,
    val translateRetryCount: Int = LlmDefaults.DEFAULT_RETRY_COUNT,
    val streamingEnabled: Boolean = true,
    val subtitleDisplayMode: LlmSubtitleDisplayMode = LlmSubtitleDisplayMode.TranslationOnly
)

object LlmDefaults {
    const val OPEN_ROUTER_ENDPOINT = "https://openrouter.ai/api/v1"
    const val OPENAI_ENDPOINT = "https://api.openai.com/v1"
    const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/openai"
    const val OPEN_ROUTER_MAIN_MODEL = "google/gemma-4-31b-it:free"
    const val OPEN_ROUTER_LITE_MODEL = "google/gemma-4-26b-a4b-it:free"
    const val OPENAI_MAIN_MODEL = "gpt-4o-mini"
    const val OPENAI_LITE_MODEL = "gpt-4o-mini"
    const val GEMINI_MAIN_MODEL = "gemini-2.5-flash"
    const val GEMINI_LITE_MODEL = "gemini-2.5-flash-lite"
    const val DEFAULT_RETRY_COUNT = 10
}

data class LlmProviderPreset(
    val kind: LlmProviderKind,
    val endpoint: String,
    val mainModel: String,
    val liteModel: String
) {
    companion object {
        fun forKind(kind: LlmProviderKind): LlmProviderPreset = when (kind) {
            LlmProviderKind.OpenRouter -> LlmProviderPreset(
                kind,
                LlmDefaults.OPEN_ROUTER_ENDPOINT,
                LlmDefaults.OPEN_ROUTER_MAIN_MODEL,
                LlmDefaults.OPEN_ROUTER_LITE_MODEL
            )
            LlmProviderKind.Gemini -> LlmProviderPreset(
                kind,
                LlmDefaults.GEMINI_ENDPOINT,
                LlmDefaults.GEMINI_MAIN_MODEL,
                LlmDefaults.GEMINI_LITE_MODEL
            )
            LlmProviderKind.OpenAi -> LlmProviderPreset(
                kind,
                LlmDefaults.OPENAI_ENDPOINT,
                LlmDefaults.OPENAI_MAIN_MODEL,
                LlmDefaults.OPENAI_LITE_MODEL
            )
            LlmProviderKind.Custom -> LlmProviderPreset(
                kind,
                "",
                "",
                ""
            )
        }
    }
}
