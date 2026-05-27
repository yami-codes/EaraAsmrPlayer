package com.asmr.player.listentogether

import android.os.Build
import com.asmr.player.BuildConfig
import com.asmr.player.data.remote.NetworkHeaders
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import kotlin.text.Charsets.UTF_8
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListenTogetherRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val clientSessionId = UUID.randomUUID().toString()
    private val appHeaderValue = "com.asmr.player/${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    private val deviceFingerprint = buildDeviceFingerprint()
    private val userAgent = buildUserAgent()

    val isBackendConfigured: Boolean
        get() = baseUrl.isNotBlank()

    suspend fun upsertPresence(
        identity: ListenTogetherTrackIdentity,
        playbackPositionMs: Long,
        isPlaying: Boolean
    ): ListenTogetherPresenceResponse? {
        if (!isBackendConfigured) return null
        val payload = ListenTogetherPresencePayload(
            sessionKey = identity.sessionKey,
            albumKey = identity.albumKey,
            mediaFingerprint = identity.mediaFingerprint,
            fileSizeBytes = identity.fileSizeBytes,
            fingerprintAlgorithm = identity.fingerprintAlgorithm,
            mediaKind = identity.mediaKind.name.lowercase(),
            sourcePathHint = identity.sourcePath.substringAfterLast('/').substringAfterLast('\\'),
            mediaId = identity.mediaId,
            rjCode = identity.rjCode,
            title = identity.displayTitle,
            artist = identity.displayArtist,
            playbackPositionMs = playbackPositionMs.coerceAtLeast(0L),
            isPlaying = isPlaying,
            sentAtEpochMs = System.currentTimeMillis(),
            clientSessionId = clientSessionId,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) / Android ${Build.VERSION.SDK_INT}"
        )
        return postJson("presence/upsert", payload, ListenTogetherPresenceResponse::class.java)
    }

    suspend fun leave(identity: ListenTogetherTrackIdentity) {
        if (!isBackendConfigured) return
        val payload = ListenTogetherLeavePayload(
            sessionKey = identity.sessionKey,
            clientSessionId = clientSessionId,
            sentAtEpochMs = System.currentTimeMillis()
        )
        postJson<Unit>("presence/leave", payload, null)
    }

    private suspend fun <T : Any> postJson(path: String, body: Any, responseClass: Class<T>?): T? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(resolveUrl(path))
                .header("User-Agent", userAgent)
                .header("X-Listen-Together-App", appHeaderValue)
                .header("X-Listen-Together-Client-Session-Id", clientSessionId)
                .header("X-Listen-Together-Device-Fingerprint", deviceFingerprint)
                .header(NetworkHeaders.HEADER_SILENT_IO_ERROR, NetworkHeaders.SILENT_IO_ERROR_ON)
                .post(gson.toJson(body).toRequestBody(JSON_MEDIA_TYPE))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("ListenTogether request failed: ${response.code}")
                }
                if (responseClass == null) return@withContext null
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return@withContext null
                gson.fromJson(raw, responseClass)
            }
        }
    }

    private fun resolveUrl(path: String): String {
        val root = baseUrl.trimEnd('/')
        val normalizedPath = path.trimStart('/')
        return "$root/$normalizedPath"
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val baseUrl: String
            get() = BuildConfig.LISTEN_TOGETHER_BASE_URL
    }

    private fun buildDeviceFingerprint(): String {
        val source = listOf(
            Build.BRAND,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.DEVICE,
            Build.PRODUCT,
            Build.VERSION.SDK_INT.toString(),
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
        ).joinToString(separator = "|") { it.trim() }
        return XxHash64.hashHex(source.toByteArray(UTF_8))
    }

    private fun buildUserAgent(): String {
        val deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
            .ifBlank { "Android" }
        return "EaraListenTogether/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.SDK_INT}; $deviceModel)"
    }
}
