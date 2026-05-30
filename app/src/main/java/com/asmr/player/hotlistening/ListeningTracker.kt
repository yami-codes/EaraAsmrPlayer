package com.asmr.player.hotlistening

import androidx.media3.common.MediaItem
import com.asmr.player.playback.PlaybackSnapshot
import com.asmr.player.ui.player.isOnlineMedia
import com.asmr.player.util.DlsiteWorkNo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningTracker @Inject constructor(
    private val hotListeningApi: HotListeningApi
) {
    private var observationJob: Job? = null

    fun start(scope: CoroutineScope, playback: StateFlow<PlaybackSnapshot>) {
        stop()
        observationJob = scope.launch {
            var accumulatedMs = 0L
            var lastPositionMs = 0L
            var reportedForCurrentMediaId: String? = null
            var lastMediaId: String? = null

            playback
                .map { snapshot ->
                    val item = snapshot.currentMediaItem
                    val mediaId = item?.mediaId ?: ""
                    val isOnline = item.isOnlineMedia()
                    Triple(snapshot.isPlaying, snapshot.positionMs.coerceAtLeast(0L), isOnline) to mediaId
                }
                .distinctUntilChanged { old, new ->
                    old.first.first == new.first.first &&
                        old.first.second == new.first.second &&
                        old.second == new.second
                }
                .collect { (triple, mediaId) ->
                    val (isPlaying, positionMs, isOnline) = triple

                    if (mediaId != lastMediaId) {
                        accumulatedMs = 0L
                        lastPositionMs = positionMs
                        lastMediaId = mediaId
                        reportedForCurrentMediaId = null
                        return@collect
                    }

                    if (!isPlaying) {
                        lastPositionMs = positionMs
                        return@collect
                    }

                    val delta = positionMs - lastPositionMs
                    if (delta > 0L && delta < 3_000L) {
                        accumulatedMs = accumulatedMs + delta
                    }
                    lastPositionMs = positionMs

                    if (accumulatedMs >= REPORT_THRESHOLD_MS && !isOnline && reportedForCurrentMediaId != mediaId) {
                        reportedForCurrentMediaId = mediaId
                        val currentItem = playback.value.currentMediaItem
                        val rjCode = extractRjCode(item = currentItem)
                        if (rjCode.isNotBlank() && hotListeningApi.isBackendConfigured) {
                            scope.launch {
                                runCatching {
                                    hotListeningApi.reportListen(rjCode)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun stop() {
        observationJob?.cancel()
        observationJob = null
    }

    private fun extractRjCode(item: MediaItem?): String {
        if (item == null) return ""
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        val mediaId = item.mediaId.trim()
        val uri = item.localConfiguration?.uri?.toString().orEmpty().trim()

        return DlsiteWorkNo.extractRjCode(
            listOfNotNull(
                extras?.getString("rj_code"),
                mediaId,
                uri,
                metadata.albumTitle?.toString(),
                metadata.title?.toString()
            ).joinToString(" ")
        )
    }

    private companion object {
        private const val REPORT_THRESHOLD_MS = 10_000L
    }
}
