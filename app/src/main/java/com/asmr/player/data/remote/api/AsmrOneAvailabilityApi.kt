package com.asmr.player.data.remote.api

import android.os.Build
import com.asmr.player.BuildConfig
import com.asmr.player.data.remote.NetworkHeaders
import com.asmr.player.data.remote.withSearchTimeouts
import com.asmr.player.listentogether.XxHash64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_8

data class AsmrOneAvailabilityRequest(
    val rjs: List<String>
)

data class AsmrOneAvailabilityResponse(
    val items: List<AsmrOneAvailabilityItem> = emptyList(),
    val serverTimeEpochMs: Long = 0L
)

data class AsmrOneAvailabilityItem(
    val rj: String = "",
    val collected: Boolean = false,
    val workId: Int = 0,
    val matchedRjs: List<String> = emptyList(),
    val originalWorkno: String = "",
    val title: String = ""
)

data class AsmrOneCollectedSearchResponse(
    val items: List<AsmrOneCollectedSearchItem>? = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val sort: String = "",
    val serverTimeEpochMs: Long = 0L
)

data class AsmrOneCollectedSearchItem(
    val workId: Int = 0,
    val rj: String = "",
    val title: String = "",
    val circle: String = "",
    val cvs: List<String>? = emptyList(),
    val tags: List<String>? = emptyList(),
    val matchedRjs: List<String>? = emptyList(),
    val originalWorkno: String = "",
    val releaseDate: String = "",
    val createDate: String = "",
    val mainCoverUrl: String = "",
    val dlCount: Int? = null,
    val price: Int? = null,
    val reviewCount: Int? = null,
    val rateCount: Int? = null,
    val rateAverage2dp: Double? = null
)

@Singleton
class AsmrOneAvailabilityApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    internal constructor(
        okHttpClient: OkHttpClient,
        gson: Gson,
        baseUrlProvider: () -> String
    ) : this(okHttpClient, gson) {
        this.baseUrlProvider = baseUrlProvider
    }

    private var baseUrlProvider: () -> String = { BuildConfig.LISTEN_TOGETHER_BASE_URL }
    private val clientSessionId = UUID.randomUUID().toString()
    private val appHeaderValue = "com.asmr.player/${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    private val deviceFingerprint = buildDeviceFingerprint()
    private val userAgent = buildUserAgent()
    private val requestClient by lazy { okHttpClient.withSearchTimeouts() }
    private val trackTreeClient by lazy { okHttpClient.withBackendTrackTreeTimeouts() }

    suspend fun check(rjs: List<String>): Map<String, Boolean> {
        val normalized = rjs
            .asSequence()
            .map { it.trim().uppercase() }
            .filter { RJ_CODE_REGEX.matches(it) }
            .distinct()
            .take(MAX_RJS)
            .toList()
        if (normalized.isEmpty() || backendBaseUrl.isBlank()) return emptyMap()

        return runCatching {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(resolveUrl("api/asmr-one/availability"))
                    .header("User-Agent", userAgent)
                    .header("X-Listen-Together-App", appHeaderValue)
                    .header("X-Listen-Together-Client-Session-Id", clientSessionId)
                    .header("X-Listen-Together-Device-Fingerprint", deviceFingerprint)
                    .header(NetworkHeaders.HEADER_SILENT_IO_ERROR, NetworkHeaders.SILENT_IO_ERROR_ON)
                    .post(gson.toJson(AsmrOneAvailabilityRequest(normalized)).toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                requestClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyMap()
                    val raw = response.body?.string().orEmpty()
                    if (raw.isBlank()) return@withContext emptyMap()
                    val parsed = gson.fromJson(raw, AsmrOneAvailabilityResponse::class.java)
                    val requested = normalized.toSet()
                    buildMap {
                        parsed.items.forEach { item ->
                            item.matchedRequestRjs(requested).forEach { rj ->
                                put(rj, item.collected)
                            }
                        }
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun findCollectedWorkId(rj: String): String? {
        val normalized = rj.trim().uppercase()
        if (!RJ_CODE_REGEX.matches(normalized) || backendBaseUrl.isBlank()) return null
        return runCatching {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(resolveUrl("api/asmr-one/availability"))
                    .header("User-Agent", userAgent)
                    .header("X-Listen-Together-App", appHeaderValue)
                    .header("X-Listen-Together-Client-Session-Id", clientSessionId)
                    .header("X-Listen-Together-Device-Fingerprint", deviceFingerprint)
                    .header(NetworkHeaders.HEADER_SILENT_IO_ERROR, NetworkHeaders.SILENT_IO_ERROR_ON)
                    .post(gson.toJson(AsmrOneAvailabilityRequest(listOf(normalized))).toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                requestClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val raw = response.body?.string().orEmpty()
                    if (raw.isBlank()) return@withContext null
                    val parsed = gson.fromJson(raw, AsmrOneAvailabilityResponse::class.java)
                    parsed.items.firstOrNull { item ->
                        item.collected && item.matchedRequestRjs(setOf(normalized)).isNotEmpty()
                    }?.workId?.takeIf { it > 0 }?.toString()
                }
            }
        }.getOrNull()
    }

    suspend fun search(keyword: String, limit: Int, offset: Int, sort: String): AsmrOneCollectedSearchResponse {
        if (backendBaseUrl.isBlank()) throw IOException("asmr.one backend is not configured")
        return withContext(Dispatchers.IO) {
            val url = resolveUrl("api/asmr-one/search")
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("q", keyword.trim())
                ?.addQueryParameter("limit", limit.coerceIn(1, 100).toString())
                ?.addQueryParameter("offset", offset.coerceAtLeast(0).toString())
                ?.addQueryParameter("sort", sort.trim().ifBlank { "release" })
                ?.build()
                ?: throw IOException("invalid asmr.one backend url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("X-Listen-Together-App", appHeaderValue)
                .header("X-Listen-Together-Client-Session-Id", clientSessionId)
                .header("X-Listen-Together-Device-Fingerprint", deviceFingerprint)
                .header(NetworkHeaders.HEADER_SILENT_IO_ERROR, NetworkHeaders.SILENT_IO_ERROR_ON)
                .get()
                .build()
            requestClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("asmr.one search failed: HTTP ${response.code}")
                }
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return@withContext AsmrOneCollectedSearchResponse()
                gson.fromJson(raw, AsmrOneCollectedSearchResponse::class.java)
                    ?: AsmrOneCollectedSearchResponse()
            }
        }
    }

    suspend fun getTrackTree(workId: String): List<AsmrOneTrackNodeResponse> {
        if (backendBaseUrl.isBlank()) throw IOException("asmr.one backend is not configured")
        val normalizedId = workId.trim()
        if (normalizedId.isBlank()) throw IOException("asmr.one work id is blank")
        return withContext(Dispatchers.IO) {
            val url = resolveUrl("api/asmr-one/tracks")
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("workId", normalizedId)
                ?.build()
                ?: throw IOException("invalid asmr.one tracks backend url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("X-Listen-Together-App", appHeaderValue)
                .header("X-Listen-Together-Client-Session-Id", clientSessionId)
                .header("X-Listen-Together-Device-Fingerprint", deviceFingerprint)
                .header(NetworkHeaders.HEADER_SILENT_IO_ERROR, NetworkHeaders.SILENT_IO_ERROR_ON)
                .get()
                .build()
            trackTreeClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("asmr.one tracks backend failed: HTTP ${response.code}")
                }
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return@withContext emptyList()
                val listType = object : TypeToken<List<AsmrOneTrackNodeResponse>>() {}.type
                gson.fromJson<List<AsmrOneTrackNodeResponse>>(raw, listType).orEmpty()
            }
        }
    }

    private fun resolveUrl(path: String): String {
        val root = backendBaseUrl.trimEnd('/')
        val normalizedPath = path.trimStart('/')
        return "$root/$normalizedPath"
    }

    private val backendBaseUrl: String
        get() = baseUrlProvider().trim()

    private fun buildDeviceFingerprint(): String {
        val source = listOf(
            Build.BRAND.orEmpty(),
            Build.MANUFACTURER.orEmpty(),
            Build.MODEL.orEmpty(),
            Build.DEVICE.orEmpty(),
            Build.PRODUCT.orEmpty(),
            Build.VERSION.SDK_INT.toString(),
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
        ).joinToString(separator = "|") { it.trim() }
        return XxHash64.hashHex(source.toByteArray(UTF_8))
    }

    private fun buildUserAgent(): String {
        val deviceModel = listOf(Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
            .ifBlank { "Android" }
        return "EaraAsmrOneAvailability/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.SDK_INT}; $deviceModel)"
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val RJ_CODE_REGEX = Regex("""RJ\d{6,}""")
        private const val MAX_RJS = 100
    }
}

private fun AsmrOneAvailabilityItem.matchedRequestRjs(requested: Set<String>): List<String> {
    if (requested.isEmpty()) return emptyList()
    return buildList {
        add(rj)
        add(originalWorkno)
        addAll(matchedRjs)
    }
        .asSequence()
        .map { it.trim().uppercase() }
        .filter { it in requested }
        .distinct()
        .toList()
}

private fun OkHttpClient.withBackendTrackTreeTimeouts(): OkHttpClient =
    newBuilder()
        .callTimeout(3, TimeUnit.SECONDS)
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()
