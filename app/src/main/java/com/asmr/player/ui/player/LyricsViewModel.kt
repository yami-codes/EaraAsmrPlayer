package com.asmr.player.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.asmr.player.data.llm.LlmSettings
import com.asmr.player.data.llm.LlmSubtitleDisplayMode
import com.asmr.player.data.llm.LlmTranslationErrorType
import com.asmr.player.data.llm.SubtitleTranslationResult
import com.asmr.player.data.llm.SubtitleTranslationService
import com.asmr.player.data.llm.TranslatedLyricLine
import com.asmr.player.data.lyrics.EXTRA_ALBUM_WORK_ID
import com.asmr.player.data.lyrics.EXTRA_LYRICS_RELATIVE_PATH_NO_EXT
import com.asmr.player.data.lyrics.LyricsLoader
import com.asmr.player.data.lyrics.lyricsTargetContextFromMediaItem
import com.asmr.player.data.settings.SettingsRepository
import com.asmr.player.playback.PlayerConnection
import com.asmr.player.util.SubtitleEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val title: String = "",
    val contentKey: String = "",
    val isLoading: Boolean = false,
    val lyrics: List<SubtitleEntry> = emptyList(),
    val originalLyrics: List<SubtitleEntry> = emptyList(),
    val isTranslating: Boolean = false,
    val translationStatus: String? = null,
    val isLlmTranslated: Boolean = false,
    val showDualSubtitles: Boolean = false,
    val translationError: String? = null
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val lyricsLoader: LyricsLoader,
    private val subtitleTranslationService: SubtitleTranslationService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    val playback = playerConnection.snapshot

    val llmSettings: StateFlow<LlmSettings> = settingsRepository.llmSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LlmSettings())

    private var sourceLyrics: List<SubtitleEntry> = emptyList()
    private var translationJob: Job? = null
    private var translationEpoch = 0

    private suspend fun reloadForItem(item: MediaItem?) {
        translationJob?.cancel()
        translationEpoch += 1
        val epoch = translationEpoch

        val mediaId = item?.mediaId.orEmpty()
        if (mediaId.isBlank()) {
            sourceLyrics = emptyList()
            _uiState.value = LyricsUiState()
            return
        }

        val mediaKey = lyricsContentKeyForItem(item)
        _uiState.value = _uiState.value.copy(isLoading = true)
        val result = lyricsLoader.load(item)
        sourceLyrics = result.lyrics
        val settings = settingsRepository.getLlmSettings()
        val showDual = settings.subtitleDisplayMode == LlmSubtitleDisplayMode.Dual

        _uiState.value = LyricsUiState(
            title = result.title,
            contentKey = mediaKey,
            isLoading = false,
            lyrics = result.lyrics,
            originalLyrics = result.lyrics,
            isLlmTranslated = false,
            showDualSubtitles = showDual,
            translationStatus = null,
            translationError = null
        )

        if (result.lyrics.isEmpty()) return
        if (!settings.translationEnabled) return

        maybeAutoTranslate(item, result.lyrics, result.title, settings, epoch)
    }

    private fun maybeAutoTranslate(
        item: MediaItem,
        lyrics: List<SubtitleEntry>,
        title: String,
        settings: LlmSettings,
        epoch: Int
    ) {
        translationJob = viewModelScope.launch {
            val context = lyricsTargetContextFromMediaItem(item)
            val workId = context?.albumIdentity?.ifBlank { context.mediaId } ?: item.mediaId
            val trackTitle = title.ifBlank { context?.title.orEmpty() }

            _uiState.value = _uiState.value.copy(isTranslating = true, translationStatus = "checking_cache")
            val translationResult = subtitleTranslationService.translateIfEnabled(
                source = lyrics,
                workId = workId,
                trackTitle = trackTitle,
                onProgress = { progress ->
                    if (epoch != translationEpoch) return@translateIfEnabled
                    _uiState.value = _uiState.value.copy(
                        translationStatus = progress.phase.name.lowercase()
                    )
                },
                onPartial = { partial, done, total ->
                    if (epoch != translationEpoch) return@translateIfEnabled
                    applyTranslationResult(partial, settings, done, total, mediaKey = lyricsContentKeyForItem(item))
                }
            )
            if (epoch != translationEpoch) return@launch
            applyFinalResult(translationResult, settings)
        }
    }

    fun translateSubtitlesNow(forceRefresh: Boolean = false) {
        val item = playback.value.currentMediaItem
        val lyrics = sourceLyrics
        if (item == null || lyrics.isEmpty()) {
            _uiState.value = _uiState.value.copy(translationError = "no_subtitles")
            return
        }

        translationJob?.cancel()
        val epoch = ++translationEpoch
        val context = lyricsTargetContextFromMediaItem(item)
        val workId = context?.albumIdentity?.ifBlank { context.mediaId } ?: item.mediaId
        val trackTitle = _uiState.value.title.ifBlank { context?.title.orEmpty() }

        translationJob = viewModelScope.launch {
            val settings = settingsRepository.getLlmSettings()
            _uiState.value = _uiState.value.copy(
                isTranslating = true,
                translationStatus = "starting",
                translationError = null
            )
            val result = subtitleTranslationService.translateNow(
                source = lyrics,
                workId = workId,
                trackTitle = trackTitle,
                forceRefresh = forceRefresh,
                onProgress = { progress ->
                    if (epoch != translationEpoch) return@translateNow
                    _uiState.value = _uiState.value.copy(
                        translationStatus = progress.phase.name.lowercase()
                    )
                },
                onPartial = { partial, done, total ->
                    if (epoch != translationEpoch) return@translateNow
                    applyTranslationResult(partial, settings, done, total, mediaKey = lyricsContentKeyForItem(item))
                }
            )
            if (epoch != translationEpoch) return@launch
            applyFinalResult(result, settings)
        }
    }

    fun restoreOriginalSubtitles() {
        translationJob?.cancel()
        translationEpoch += 1
        _uiState.value = _uiState.value.copy(
            lyrics = sourceLyrics,
            originalLyrics = sourceLyrics,
            isLlmTranslated = false,
            isTranslating = false,
            translationStatus = null,
            translationError = null
        )
    }

    fun refreshCurrentLyrics() {
        viewModelScope.launch {
            reloadForItem(playback.value.currentMediaItem)
        }
    }

    fun originalTextAt(index: Int): String? {
        if (!_uiState.value.showDualSubtitles) return null
        val original = sourceLyrics.getOrNull(index)?.text?.trim()
        val translated = _uiState.value.lyrics.getOrNull(index)?.text?.trim()
        if (original.isNullOrBlank()) return null
        if (original == translated) return null
        return original
    }

    private fun applyTranslationResult(
        lines: List<TranslatedLyricLine>,
        settings: LlmSettings,
        done: Int,
        total: Int,
        mediaKey: String
    ) {
        val display = lines.map { line ->
            SubtitleEntry(line.startMs, line.endMs, line.text)
        }
        _uiState.value = _uiState.value.copy(
            lyrics = display,
            originalLyrics = sourceLyrics,
            contentKey = mediaKey,
            isLlmTranslated = done > 0,
            showDualSubtitles = settings.subtitleDisplayMode == LlmSubtitleDisplayMode.Dual,
            translationStatus = "translating:$done/$total"
        )
    }

    private fun applyFinalResult(result: SubtitleTranslationResult, settings: LlmSettings) {
        when (result) {
            is SubtitleTranslationResult.Success -> {
                val display = result.lyrics.map { SubtitleEntry(it.startMs, it.endMs, it.text) }
                _uiState.value = _uiState.value.copy(
                    lyrics = display,
                    originalLyrics = sourceLyrics,
                    isLlmTranslated = true,
                    isTranslating = false,
                    translationStatus = if (result.fromCache) "cached" else "done",
                    translationError = null,
                    showDualSubtitles = settings.subtitleDisplayMode == LlmSubtitleDisplayMode.Dual
                )
            }
            is SubtitleTranslationResult.Partial -> {
                val display = result.lyrics.map { SubtitleEntry(it.startMs, it.endMs, it.text) }
                _uiState.value = _uiState.value.copy(
                    lyrics = display,
                    originalLyrics = sourceLyrics,
                    isLlmTranslated = result.translatedCount > 0,
                    isTranslating = false,
                    translationStatus = "partial:${result.translatedCount}/${result.totalCount}",
                    translationError = result.error?.type?.name,
                    showDualSubtitles = settings.subtitleDisplayMode == LlmSubtitleDisplayMode.Dual
                )
            }
            is SubtitleTranslationResult.Skipped -> {
                _uiState.value = _uiState.value.copy(
                    isTranslating = false,
                    translationStatus = null
                )
            }
            is SubtitleTranslationResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    isTranslating = false,
                    translationStatus = null,
                    translationError = mapErrorType(result.error.type)
                )
            }
        }
    }

    private fun mapErrorType(type: LlmTranslationErrorType): String = when (type) {
        LlmTranslationErrorType.MissingApiKey -> "missing_api_key"
        LlmTranslationErrorType.Auth -> "auth"
        LlmTranslationErrorType.RateLimited -> "rate_limited"
        LlmTranslationErrorType.ContentBlocked -> "content_blocked"
        LlmTranslationErrorType.InvalidConfig -> "invalid_config"
        LlmTranslationErrorType.InvalidResponse -> "invalid_response"
        LlmTranslationErrorType.Unknown -> "unknown"
    }

    init {
        viewModelScope.launch {
            playerConnection.lyricsReloadRequests.collect {
                reloadForItem(playback.value.currentMediaItem)
            }
        }
        viewModelScope.launch {
            var lastMediaKey: String? = null
            playerConnection.snapshot.collect { snap ->
                val item = snap.currentMediaItem
                val mediaId = item?.mediaId.orEmpty()
                val mediaKey = lyricsContentKeyForItem(item)
                if (mediaId.isBlank()) {
                    sourceLyrics = emptyList()
                    _uiState.value = LyricsUiState()
                    return@collect
                }
                if (lastMediaKey == mediaKey) return@collect
                lastMediaKey = mediaKey
                reloadForItem(item)
            }
        }
    }

    private fun lyricsContentKeyForItem(item: MediaItem?): String {
        val mediaId = item?.mediaId.orEmpty()
        val extras = item?.mediaMetadata?.extras
        return listOf(
            mediaId,
            extras?.getString(EXTRA_LYRICS_RELATIVE_PATH_NO_EXT).orEmpty(),
            extras?.getString("rj_code").orEmpty(),
            extras?.getString(EXTRA_ALBUM_WORK_ID).orEmpty()
        ).joinToString("|")
    }
}
