package com.asmr.player.ui.hotlistening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmr.player.data.settings.SettingsRepository
import com.asmr.player.domain.model.Album
import com.asmr.player.hotlistening.HotListeningApi
import com.asmr.player.hotlistening.HotListeningItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HotListeningViewModel @Inject constructor(
    private val hotListeningApi: HotListeningApi,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HotListeningUiState>(HotListeningUiState.Loading)
    val uiState: StateFlow<HotListeningUiState> = _uiState.asStateFlow()

    val viewMode: StateFlow<Int> = settingsRepository.hotListeningViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private var currentPeriod: String = "day"

    init {
        loadTopListings(currentPeriod)
    }

    fun selectPeriod(period: String) {
        if (period != currentPeriod) {
            currentPeriod = period
            loadTopListings(period)
        }
    }

    fun setViewMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setHotListeningViewMode(mode.coerceIn(0, 1))
        }
    }

    fun refresh() {
        loadTopListings(currentPeriod)
    }

    private fun loadTopListings(period: String) {
        if (!hotListeningApi.isBackendConfigured) {
            _uiState.update { HotListeningUiState.Error("后端未配置") }
            return
        }
        _uiState.update { HotListeningUiState.Loading }
        viewModelScope.launch {
            runCatching {
                val items = hotListeningApi.getTopListings(period)
                if (items == null) {
                    _uiState.update { HotListeningUiState.Error("请求失败") }
                    return@launch
                }
                val albums = items.map { it.toAlbum() }
                _uiState.update {
                    HotListeningUiState.Success(
                        albums = albums,
                        period = period
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    HotListeningUiState.Error(error.message ?: "加载失败")
                }
            }
        }
    }

    private fun HotListeningItem.toAlbum(): Album {
        return Album(
            title = this.title,
            path = "",
            circle = this.circle,
            cv = this.cv,
            tags = this.tagList,
            coverUrl = this.coverUrl,
            rjCode = this.rj
        )
    }
}

sealed class HotListeningUiState {
    data object Loading : HotListeningUiState()
    data class Success(
        val albums: List<Album>,
        val period: String
    ) : HotListeningUiState()
    data class Error(val message: String) : HotListeningUiState()
}
