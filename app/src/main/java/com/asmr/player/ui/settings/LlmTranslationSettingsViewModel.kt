package com.asmr.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmr.player.data.llm.LlmApiKeyStore
import com.asmr.player.data.llm.LlmSettings
import com.asmr.player.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LlmTranslationSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val apiKeyStore: LlmApiKeyStore
) : ViewModel() {
    val llmSettings: StateFlow<LlmSettings> = settingsRepository.llmSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LlmSettings())

    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    init {
        viewModelScope.launch {
            _apiKey.value = apiKeyStore.getApiKey()
        }
    }

    fun updateSettings(settings: LlmSettings) {
        viewModelScope.launch {
            settingsRepository.updateLlmSettings(settings)
        }
    }

    fun save(settings: LlmSettings, apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateLlmSettings(settings)
            if (apiKey.isBlank()) {
                apiKeyStore.clearApiKey()
            } else {
                apiKeyStore.saveApiKey(apiKey)
            }
            _apiKey.value = apiKey.ifBlank { null }
        }
    }
}
