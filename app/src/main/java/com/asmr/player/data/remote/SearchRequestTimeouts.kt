package com.asmr.player.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal const val SEARCH_CALL_TIMEOUT_SECONDS = 20L
internal const val SEARCH_CONNECT_TIMEOUT_SECONDS = 10L
internal const val SEARCH_READ_TIMEOUT_SECONDS = 15L
internal const val SEARCH_WRITE_TIMEOUT_SECONDS = 15L

internal fun OkHttpClient.withSearchTimeouts(): OkHttpClient =
    newBuilder()
        // Bound the full request lifecycle so stalled DNS or TCP handshakes do not keep
        // the search screen locked forever.
        .callTimeout(SEARCH_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .connectTimeout(SEARCH_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(SEARCH_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(SEARCH_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
