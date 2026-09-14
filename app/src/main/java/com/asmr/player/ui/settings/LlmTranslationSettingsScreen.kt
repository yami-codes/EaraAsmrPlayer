package com.asmr.player.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asmr.player.R
import com.asmr.player.data.llm.LlmBatchSplitMode
import com.asmr.player.data.llm.LlmProviderKind
import com.asmr.player.data.llm.LlmProviderPreset
import com.asmr.player.data.llm.LlmSettings
import com.asmr.player.data.llm.LlmSubtitleDisplayMode
import com.asmr.player.data.llm.LlmSubtitleTargetLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmTranslationSettingsScreen(
    onBack: () -> Unit,
    viewModel: LlmTranslationSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.llmSettings.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    var endpoint by remember(settings.apiEndpoint) { mutableStateOf(settings.apiEndpoint) }
    var mainModel by remember(settings.mainModel) { mutableStateOf(settings.mainModel) }
    var liteModel by remember(settings.liteModel) { mutableStateOf(settings.liteModel) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey.orEmpty()) }
    var systemPrompt by remember(settings.systemPromptOverride) { mutableStateOf(settings.systemPromptOverride) }
    var jailbreakPrompt by remember(settings.jailbreakPrompt) { mutableStateOf(settings.jailbreakPrompt) }
    var manualBatchSize by remember(settings.manualBatchSize) { mutableStateOf(settings.manualBatchSize.toString()) }
    var obscureKey by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.llm_translation_configure)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.save(
                            settings = settings.copy(
                                apiEndpoint = endpoint.trim(),
                                mainModel = mainModel.trim(),
                                liteModel = liteModel.trim(),
                                systemPromptOverride = systemPrompt,
                                jailbreakPrompt = jailbreakPrompt,
                                manualBatchSize = manualBatchSize.toIntOrNull() ?: settings.manualBatchSize
                            ),
                            apiKey = apiKeyInput.trim()
                        )
                        onBack()
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProviderPresetRow(
                    selected = LlmProviderKind.detect(endpoint),
                    onSelect = { kind ->
                        val preset = LlmProviderPreset.forKind(kind)
                        if (kind != LlmProviderKind.Custom) {
                            endpoint = preset.endpoint
                            mainModel = preset.mainModel
                            liteModel = preset.liteModel
                        }
                    }
                )
            }
            item {
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text(stringResource(R.string.llm_api_endpoint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = mainModel,
                    onValueChange = { mainModel = it },
                    label = { Text(stringResource(R.string.llm_main_model)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = liteModel,
                    onValueChange = { liteModel = it },
                    label = { Text(stringResource(R.string.llm_lite_model)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(stringResource(R.string.llm_api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (obscureKey) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        IconButton(onClick = { obscureKey = !obscureKey }) {
                            Icon(
                                if (obscureKey) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
            item {
                EnumChipRow(
                    title = stringResource(R.string.llm_target_language),
                    options = LlmSubtitleTargetLanguage.entries,
                    selected = settings.targetLanguage,
                    label = { lang ->
                        when (lang) {
                            LlmSubtitleTargetLanguage.System -> stringResource(R.string.language_system)
                            LlmSubtitleTargetLanguage.English -> stringResource(R.string.language_english)
                            LlmSubtitleTargetLanguage.Chinese -> stringResource(R.string.language_chinese_simplified)
                            LlmSubtitleTargetLanguage.Japanese -> stringResource(R.string.llm_target_lang_ja)
                            LlmSubtitleTargetLanguage.Thai -> stringResource(R.string.language_thai)
                            LlmSubtitleTargetLanguage.Korean -> stringResource(R.string.language_korean_short)
                        }
                    },
                    onSelect = { viewModel.updateSettings(settings.copy(targetLanguage = it)) }
                )
            }
            item {
                EnumChipRow(
                    title = stringResource(R.string.llm_subtitle_display_mode),
                    options = LlmSubtitleDisplayMode.entries,
                    selected = settings.subtitleDisplayMode,
                    label = { mode ->
                        when (mode) {
                            LlmSubtitleDisplayMode.TranslationOnly -> stringResource(R.string.llm_subtitle_display_translation_only)
                            LlmSubtitleDisplayMode.Dual -> stringResource(R.string.llm_subtitle_display_dual)
                        }
                    },
                    onSelect = { viewModel.updateSettings(settings.copy(subtitleDisplayMode = it)) }
                )
            }
            item {
                EnumChipRow(
                    title = stringResource(R.string.llm_batch_split_mode),
                    options = LlmBatchSplitMode.entries,
                    selected = settings.batchSplitMode,
                    label = { mode ->
                        when (mode) {
                            LlmBatchSplitMode.None -> stringResource(R.string.llm_batch_split_none)
                            LlmBatchSplitMode.Provider -> stringResource(R.string.llm_batch_split_provider)
                            LlmBatchSplitMode.Manual -> stringResource(R.string.llm_batch_split_manual)
                        }
                    },
                    onSelect = { viewModel.updateSettings(settings.copy(batchSplitMode = it)) }
                )
            }
            item {
                OutlinedTextField(
                    value = manualBatchSize,
                    onValueChange = { manualBatchSize = it },
                    label = { Text(stringResource(R.string.llm_manual_batch_size)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.llm_streaming_enabled),
                    checked = settings.streamingEnabled,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(streamingEnabled = it)) }
                )
            }
            item {
                ToggleRow(
                    title = stringResource(R.string.llm_jailbreak_auto),
                    checked = settings.jailbreakAuto,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(jailbreakAuto = it)) }
                )
            }
            item {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text(stringResource(R.string.llm_system_prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
            item {
                OutlinedTextField(
                    value = jailbreakPrompt,
                    onValueChange = { jailbreakPrompt = it },
                    label = { Text(stringResource(R.string.llm_jailbreak_prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
    }
}

@Composable
private fun ProviderPresetRow(
    selected: LlmProviderKind,
    onSelect: (LlmProviderKind) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.llm_provider_preset), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                LlmProviderKind.OpenRouter,
                LlmProviderKind.Gemini,
                LlmProviderKind.OpenAi,
                LlmProviderKind.Custom
            ).forEach { kind ->
                LlmOptionChip(
                    label = when (kind) {
                        LlmProviderKind.OpenRouter -> stringResource(R.string.llm_preset_openrouter)
                        LlmProviderKind.Gemini -> stringResource(R.string.llm_preset_gemini)
                        LlmProviderKind.OpenAi -> stringResource(R.string.llm_preset_openai)
                        LlmProviderKind.Custom -> stringResource(R.string.llm_preset_custom)
                    },
                    selected = selected == kind,
                    onClick = { onSelect(kind) }
                )
            }
        }
    }
}

@Composable
private fun <T> EnumChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                LlmOptionChip(
                    label = label(option),
                    selected = selected == option,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun LlmOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
