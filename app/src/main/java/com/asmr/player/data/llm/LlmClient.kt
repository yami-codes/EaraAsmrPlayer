package com.asmr.player.data.llm

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class LlmChatResult(
    val content: String,
    val usage: LlmUsage? = null
)

data class LlmUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * OpenAI-compatible chat client.
 * Ported from LizuNemuri `lib/data/services/llm_client.dart`.
 */
@Singleton
class LlmClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    suspend fun chatCompletion(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<Map<String, String>>,
        temperature: Double = 0.2
    ): LlmChatResult = withContext(Dispatchers.IO) {
        val normalizedEndpoint = normalizeEndpoint(endpoint)
        if (normalizedEndpoint.isEmpty()) {
            throw LlmTranslationException(LlmTranslationErrorType.InvalidConfig, "empty endpoint")
        }
        if (apiKey.isBlank()) {
            throw LlmTranslationException(LlmTranslationErrorType.MissingApiKey, "missing api key")
        }
        if (model.isBlank()) {
            throw LlmTranslationException(LlmTranslationErrorType.InvalidConfig, "empty model")
        }

        val body = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", temperature)
            addProperty("stream", false)
            add("messages", gson.toJsonTree(messages))
        }

        val requestBuilder = Request.Builder()
            .url("$normalizedEndpoint/chat/completions")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")

        if (LlmProviderKind.detect(endpoint) == LlmProviderKind.OpenRouter) {
            requestBuilder
                .header("HTTP-Referer", "https://github.com/yami-codes/EaraAsmrPlayer")
                .header("X-Title", "EaraAsmrPlayer")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw mapHttpError(response.code, responseBody)
        }

        val parsed = JsonParser.parseString(responseBody).asJsonObject
        val content = parsed.getAsJsonArray("choices")
            ?.firstOrNull()
            ?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")
            ?.asString
            ?: throw LlmTranslationException(LlmTranslationErrorType.InvalidResponse, "empty content")

        val usage = parsed.getAsJsonObject("usage")?.let { usageObj ->
            LlmUsage(
                promptTokens = usageObj.get("prompt_tokens")?.asInt ?: 0,
                completionTokens = usageObj.get("completion_tokens")?.asInt ?: 0,
                totalTokens = usageObj.get("total_tokens")?.asInt ?: 0
            )
        }

        LlmChatResult(content = content, usage = usage)
    }

    suspend fun chatCompletionStream(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<Map<String, String>>,
        temperature: Double = 0.2,
        onChunk: (String) -> Unit
    ): LlmUsage? = withContext(Dispatchers.IO) {
        val normalizedEndpoint = normalizeEndpoint(endpoint)
        if (normalizedEndpoint.isEmpty()) {
            throw LlmTranslationException(LlmTranslationErrorType.InvalidConfig, "empty endpoint")
        }
        if (apiKey.isBlank()) {
            throw LlmTranslationException(LlmTranslationErrorType.MissingApiKey, "missing api key")
        }

        val body = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", temperature)
            addProperty("stream", true)
            add("messages", gson.toJsonTree(messages))
        }

        val requestBuilder = Request.Builder()
            .url("$normalizedEndpoint/chat/completions")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")

        if (LlmProviderKind.detect(endpoint) == LlmProviderKind.OpenRouter) {
            requestBuilder
                .header("HTTP-Referer", "https://github.com/yami-codes/EaraAsmrPlayer")
                .header("X-Title", "EaraAsmrPlayer")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val responseBody = response.body?.string().orEmpty()
            throw mapHttpError(response.code, responseBody)
        }

        var usage: LlmUsage? = null
        response.body?.byteStream()?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val raw = line ?: continue
                    if (!raw.startsWith("data:")) continue
                    val payload = raw.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    val json = runCatching { JsonParser.parseString(payload).asJsonObject }.getOrNull()
                        ?: continue
                    json.getAsJsonObject("usage")?.let { usageObj ->
                        usage = LlmUsage(
                            promptTokens = usageObj.get("prompt_tokens")?.asInt ?: 0,
                            completionTokens = usageObj.get("completion_tokens")?.asInt ?: 0,
                            totalTokens = usageObj.get("total_tokens")?.asInt ?: 0
                        )
                    }
                    val delta = json.getAsJsonArray("choices")
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.getAsJsonObject("delta")
                        ?.get("content")
                        ?.asString
                    if (!delta.isNullOrEmpty()) {
                        onChunk(delta)
                    }
                }
            }
        }
        usage
    }

    suspend fun fetchModelContextLength(endpoint: String, apiKey: String, model: String): Int? =
        withContext(Dispatchers.IO) {
            val normalizedEndpoint = normalizeEndpoint(endpoint)
            if (normalizedEndpoint.isEmpty() || apiKey.isBlank() || model.isBlank()) return@withContext null
            val request = Request.Builder()
                .url("$normalizedEndpoint/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            runCatching {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string().orEmpty()
                    val root = JsonParser.parseString(body).asJsonObject
                    val data = root.getAsJsonArray("data") ?: return@withContext null
                    data.firstOrNull { element ->
                        element.asJsonObject.get("id")?.asString == model
                    }?.asJsonObject?.get("context_length")?.asInt
                }
            }.getOrNull()
        }

    fun parseJsonArrayResponse(content: String): List<Map<String, Any?>> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()
        val json = runCatching { JsonParser.parseString(trimmed) }.getOrNull() ?: return emptyList()
        val array = when {
            json is JsonArray -> json
            json is JsonObject && json.has("translations") -> json.getAsJsonArray("translations")
            else -> return emptyList()
        }
        return array.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            element.asJsonObject.entrySet().associate { (k, v) ->
                k to when {
                    v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asInt
                    v.isJsonPrimitive -> v.asString
                    else -> v.toString()
                }
            }
        }
    }

    private fun normalizeEndpoint(raw: String): String {
        var url = raw.trim()
        while (url.endsWith('/')) {
            url = url.dropLast(1)
        }
        return url
    }

    private fun mapHttpError(code: Int, body: String): LlmTranslationException {
        val lower = body.lowercase()
        return when (code) {
            401, 403 -> LlmTranslationException(LlmTranslationErrorType.Auth, body)
            429 -> LlmTranslationException(LlmTranslationErrorType.RateLimited, body)
            else -> when {
                lower.contains("content_policy") || lower.contains("safety") ->
                    LlmTranslationException(LlmTranslationErrorType.ContentBlocked, body)
                else -> LlmTranslationException(LlmTranslationErrorType.Unknown, "HTTP $code: $body")
            }
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
