package com.asmr.player.ui.hotlistening

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmr.player.R
import com.asmr.player.data.settings.SettingsRepository
import com.asmr.player.domain.model.Album
import com.asmr.player.hotlistening.HotListeningApi
import com.asmr.player.hotlistening.HotListeningItem
import com.asmr.player.hotlistening.HotListeningSortMode
import com.asmr.player.util.MessageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HotListeningViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val hotListeningApi: HotListeningApi,
    private val settingsRepository: SettingsRepository,
    val messageManager: MessageManager,
) : ViewModel() {

    private val _rawUiState = MutableStateFlow<HotListeningRawUiState>(HotListeningRawUiState.Loading)
    val uiState: StateFlow<HotListeningUiState> = combine(
        _rawUiState,
        settingsRepository.searchBlockedKeywords
    ) { rawState, blockedKeywords ->
        rawState.toUiState(blockedKeywords)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HotListeningUiState.Loading)

    private val _selectedPeriod = MutableStateFlow("day")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    var scrollPosition: HotListeningScrollPosition = HotListeningScrollPosition()
        private set

    val viewMode: StateFlow<Int> = settingsRepository.hotListeningViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val sortMode: StateFlow<HotListeningSortMode> = settingsRepository.hotListeningSortMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HotListeningSortMode.PlayCount)

    init {
        viewModelScope.launch {
            combine(_selectedPeriod, sortMode) { period, mode -> period to mode }
                .collectLatest { (period, mode) ->
                    loadTopListings(period, mode)
                }
        }
    }

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun selectSortMode(mode: HotListeningSortMode) {
        viewModelScope.launch {
            settingsRepository.setHotListeningSortMode(mode)
        }
    }

    fun setViewMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setHotListeningViewMode(mode.coerceIn(0, 1))
        }
    }

    fun updateListScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        scrollPosition = scrollPosition.copy(
            listFirstVisibleItemIndex = firstVisibleItemIndex.coerceAtLeast(0),
            listFirstVisibleItemScrollOffset = firstVisibleItemScrollOffset.coerceAtLeast(0)
        )
    }

    fun updateGridScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        scrollPosition = scrollPosition.copy(
            gridFirstVisibleItemIndex = firstVisibleItemIndex.coerceAtLeast(0),
            gridFirstVisibleItemScrollOffset = firstVisibleItemScrollOffset.coerceAtLeast(0)
        )
    }

    fun resetScrollPosition() {
        scrollPosition = HotListeningScrollPosition()
    }

    fun refresh() {
        val period = _selectedPeriod.value
        val mode = sortMode.value
        viewModelScope.launch {
            loadTopListings(period, mode)
        }
    }

    private suspend fun loadTopListings(period: String, sortMode: HotListeningSortMode) {
        if (!hotListeningApi.isBackendConfigured) {
            _rawUiState.update { HotListeningRawUiState.Error(appContext.getString(R.string.str_8cdaa720)) }
            return
        }
        _rawUiState.update { HotListeningRawUiState.Loading }
        runCatching {
            val items = hotListeningApi.getTopListings(period, sortMode)
            if (items == null) {
                _rawUiState.update { HotListeningRawUiState.Error(appContext.getString(R.string.str_f50bf418)) }
                return
            }
            val entries = items.map { it.toEntry(sortMode) }
            _rawUiState.update {
                HotListeningRawUiState.Success(
                    entries = entries,
                    period = period,
                    sortMode = sortMode
                )
            }
        }.onFailure { error ->
            _rawUiState.update {
                HotListeningRawUiState.Error(error.message ?: appContext.getString(R.string.str_866b795e))
            }
        }
    }

    private fun HotListeningItem.toEntry(sortMode: HotListeningSortMode): HotListeningEntry {
        return HotListeningEntry(
            album = Album(
                title = title,
                path = "",
                circle = circle,
                cv = cv,
                tags = tagList,
                coverUrl = coverUrl,
                rjCode = rj,
                releaseDate = releaseDate.trim(),
                ratingValue = ratingValue ?: rateAverage2dp,
                ratingCount = (ratingCount ?: rateCount ?: reviewCount ?: 0).coerceAtLeast(0),
                dlCount = dlCount.coerceAtLeast(0),
                priceJpy = (priceJpy ?: price ?: 0).coerceAtLeast(0),
                description = description.trim()
            ),
            playCount = playCount,
            listenDurationMs = listenDurationMs,
            sortMode = sortMode
        )
    }
}

private sealed class HotListeningRawUiState {
    data object Loading : HotListeningRawUiState()
    data class Success(
        val entries: List<HotListeningEntry>,
        val period: String,
        val sortMode: HotListeningSortMode
    ) : HotListeningRawUiState()
    data class Error(val message: String) : HotListeningRawUiState()
}

data class HotListeningScrollPosition(
    val listFirstVisibleItemIndex: Int = 0,
    val listFirstVisibleItemScrollOffset: Int = 0,
    val gridFirstVisibleItemIndex: Int = 0,
    val gridFirstVisibleItemScrollOffset: Int = 0
)

data class HotListeningEntry(
    val album: Album,
    val playCount: Int,
    val listenDurationMs: Long,
    val sortMode: HotListeningSortMode
)

internal fun formatHotListeningMetricLabel(context: Context, entry: HotListeningEntry): String {
    return when (entry.sortMode) {
        HotListeningSortMode.PlayCount -> formatHotListeningCompactCount(context, entry.playCount.toLong())
        HotListeningSortMode.ListenDuration -> formatHotListeningCompactDuration(context, entry.listenDurationMs)
    }
}

private fun formatHotListeningCompactCount(context: Context, value: Long): String {
    return when {
        value >= 100_000_000L -> formatHotListeningDecimalUnit(context, value, 100_000_000L, R.string.str_37ae2852)
        value >= 10_000L -> formatHotListeningDecimalUnit(context, value, 10_000L, R.string.str_9d032066)
        else -> value.toString()
    }
}

private fun formatHotListeningDecimalUnit(
    context: Context,
    value: Long,
    unitValue: Long,
    unitResId: Int
): String {
    val unit = context.getString(unitResId)
    val whole = value / unitValue
    val decimal = (value % unitValue) / (unitValue / 10L)
    return if (decimal > 0L && whole < 100L) {
        "$whole.$decimal$unit"
    } else {
        "$whole$unit"
    }
}

private fun formatHotListeningCompactDuration(context: Context, ms: Long): String {
    if (ms < 60_000L) return context.getString(R.string.str_d950c32d)

    val totalMinutes = ms / 60_000L
    if (totalMinutes < 60L) return context.getString(R.string.str_2424a778, totalMinutes)

    val totalHours = totalMinutes / 60L
    val compactHours = when {
        totalHours >= 100_000_000L -> formatHotListeningDecimalUnit(context, totalHours, 100_000_000L, R.string.str_37ae2852)
        totalHours >= 10_000L -> formatHotListeningDecimalUnit(context, totalHours, 10_000L, R.string.str_9d032066)
        else -> totalHours.toString()
    }
    return context.getString(R.string.str_53128531, compactHours)
}

sealed class HotListeningUiState {
    data object Loading : HotListeningUiState()
    data class Success(
        val entries: List<HotListeningEntry>,
        val blockedEntries: List<HotListeningEntry>,
        val period: String,
        val sortMode: HotListeningSortMode
    ) : HotListeningUiState()
    data class Error(val message: String) : HotListeningUiState()
}

internal data class HotListeningBlockedEntries(
    val visibleEntries: List<HotListeningEntry>,
    val blockedEntries: List<HotListeningEntry>
)

private fun HotListeningRawUiState.toUiState(blockedKeywords: List<String>): HotListeningUiState {
    return when (this) {
        HotListeningRawUiState.Loading -> HotListeningUiState.Loading
        is HotListeningRawUiState.Error -> HotListeningUiState.Error(message)
        is HotListeningRawUiState.Success -> {
            val filtered = filterHotListeningEntries(entries, blockedKeywords)
            HotListeningUiState.Success(
                entries = filtered.visibleEntries,
                blockedEntries = filtered.blockedEntries,
                period = period,
                sortMode = sortMode
            )
        }
    }
}

internal fun filterHotListeningEntries(
    entries: List<HotListeningEntry>,
    blockedKeywords: List<String>
): HotListeningBlockedEntries {
    val normalizedKeywords = normalizeHotListeningBlockedKeywords(blockedKeywords)
    if (normalizedKeywords.isEmpty()) {
        return HotListeningBlockedEntries(
            visibleEntries = entries,
            blockedEntries = emptyList()
        )
    }
    val (blockedEntries, visibleEntries) = entries.partition { entry ->
        val searchableText = entry.album.toBlockedKeywordSearchText()
        normalizedKeywords.any { keyword -> searchableText.contains(keyword) }
    }
    return HotListeningBlockedEntries(
        visibleEntries = visibleEntries,
        blockedEntries = blockedEntries
    )
}

private fun normalizeHotListeningBlockedKeywords(keywords: List<String>): List<String> {
    return keywords
        .map { it.trim() }
        .map { it.removePrefix("-").trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .map { it.lowercase(Locale.ROOT) }
}

private fun Album.toBlockedKeywordSearchText(): String {
    return buildString {
        appendLine(title)
        appendLine(circle)
        appendLine(cv)
        appendLine(rjCode)
        appendLine(workId)
        tags.forEach { appendLine(it) }
    }.lowercase(Locale.ROOT)
}
