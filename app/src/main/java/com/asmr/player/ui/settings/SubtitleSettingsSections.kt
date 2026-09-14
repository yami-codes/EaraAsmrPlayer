package com.asmr.player.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asmr.player.R
import com.asmr.player.data.settings.DeepSeekReasoningEffort
import com.asmr.player.data.settings.DeepSeekTranslationSettings
import com.asmr.player.subtitle.DEEPSEEK_SUBTITLE_MODEL
import com.asmr.player.subtitle.DeepSeekAccountState
import com.asmr.player.subtitle.SubtitleModelDownloadSource
import com.asmr.player.subtitle.SubtitleModelInstallationState
import com.asmr.player.subtitle.SubtitleModelOperation
import com.asmr.player.subtitle.SubtitleModelState
import com.asmr.player.subtitle.SubtitleTranscriptionModels
import com.asmr.player.subtitle.configuredSubtitleModelDownloadSources
import com.asmr.player.subtitle.formatDeepSeekBalances
import com.asmr.player.subtitle.formatDeepSeekTokenTotal
import com.asmr.player.ui.common.EaraLogoLoadingIndicator
import com.asmr.player.ui.theme.AsmrTheme
import com.asmr.player.util.Formatting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeepSeekTranslationSettingsSection(
    state: DeepSeekApiKeyUiState,
    accountState: DeepSeekAccountState = DeepSeekAccountState(),
    settings: DeepSeekTranslationSettings,
    apiKeyInput: String,
    compact: Boolean,
    segmentedButtonColors: SegmentedButtonColors,
    onApiKeyInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onThinkingEnabledChanged: (Boolean) -> Unit,
    onReasoningEffortChanged: (DeepSeekReasoningEffort) -> Unit,
    onFinalPolishEnabledChanged: (Boolean) -> Unit,
    activeTipKey: String? = null,
    onToggleTip: ((String) -> Unit)? = null
) {
    val colorScheme = AsmrTheme.colorScheme
    val actionButtonColors = subtitleSettingsPrimaryTonalButtonColors()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = DEEPSEEK_SUBTITLE_MODEL,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("deepseek_model_name")
        )
        if (state.configured) {
            Row(
                modifier = Modifier.width(if (compact) 208.dp else 248.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.deepseek_account_summary,
                        formatDeepSeekTokenTotal(accountState.totalTokens),
                        formatDeepSeekBalances(accountState.balances)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (accountState.balanceAvailable == false) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("deepseek_account_summary")
                )
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.deepseek_api_key_configured_cd),
                    tint = Color(0xFF3E9B63),
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("deepseek_api_key_configured")
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val inputModifier = if (compact) {
            Modifier.weight(1f)
        } else {
            Modifier.widthIn(max = 280.dp)
        }
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = onApiKeyInputChanged,
            modifier = inputModifier
                .height(48.dp)
                .testTag("deepseek_api_key_input"),
            placeholder = {
                Text(
                    text = stringResource(R.string.deepseek_api_key_placeholder),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !state.saving,
            isError = state.errorMessage != null
        )
        FilledTonalButton(
            onClick = onSave,
            enabled = apiKeyInput.isNotBlank() && !state.saving,
            modifier = Modifier
                .height(48.dp)
                .testTag("deepseek_api_key_action"),
            colors = actionButtonColors,
            shape = RoundedCornerShape(14.dp)
        ) {
            if (state.saving) {
                EaraLogoLoadingIndicator(size = 18.dp)
            } else {
                Text(
                    if (state.configured) {
                        stringResource(R.string.deepseek_replace_key)
                    } else {
                        stringResource(R.string.deepseek_save_key)
                    }
                )
            }
        }
    }
    state.errorMessage?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    SettingsToggleRow(
        text = stringResource(R.string.deepseek_thinking_mode),
        checked = settings.thinkingEnabled,
        onCheckedChange = onThinkingEnabledChanged
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.deepseek_reasoning_effort),
            style = MaterialTheme.typography.bodyMedium,
            color = if (settings.thinkingEnabled) colorScheme.textPrimary else colorScheme.textTertiary
        )
        Spacer(modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .testTag("deepseek_reasoning_effort")
        ) {
            DeepSeekReasoningEffort.entries.forEachIndexed { index, effort ->
                SegmentedButton(
                    selected = settings.reasoningEffort == effort,
                    onClick = { onReasoningEffortChanged(effort) },
                    enabled = settings.thinkingEnabled,
                    modifier = Modifier.testTag("deepseek_reasoning_${effort.wireValue}"),
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = DeepSeekReasoningEffort.entries.size
                    ),
                    colors = segmentedButtonColors,
                    icon = {},
                    label = {
                        Text(
                            when (effort) {
                                DeepSeekReasoningEffort.LOW -> "Low"
                                DeepSeekReasoningEffort.HIGH -> "High"
                                DeepSeekReasoningEffort.MAX -> "Max"
                            }
                        )
                    }
                )
            }
        }
    }

    SettingsToggleRow(
        text = stringResource(R.string.deepseek_final_polish),
        checked = settings.finalPolishEnabled,
        onCheckedChange = onFinalPolishEnabledChanged,
        infoKey = "final_polish",
        infoTitle = stringResource(R.string.deepseek_final_polish),
        infoText = stringResource(R.string.deepseek_final_polish_info),
        activeTipKey = activeTipKey,
        onToggleTip = onToggleTip
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubtitleModelSettingsSection(
    state: SubtitleModelState,
    selectedSourceIds: Map<String, String>,
    deviceSupported: Boolean,
    segmentedButtonColors: SegmentedButtonColors,
    onSourceSelected: (String, SubtitleModelDownloadSource) -> Unit,
    onDownload: (String, SubtitleModelDownloadSource) -> Unit,
    onCancelDownload: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearFailure: (String) -> Unit
) {
    var selectedModelId by rememberSaveable {
        mutableStateOf(
            state.operation?.modelId ?: SubtitleTranscriptionModels.SENSE_VOICE_SMALL_INT8.id
        )
    }
    LaunchedEffect(state.operation?.modelId) {
        state.operation?.modelId?.let { selectedModelId = it }
    }
    val model = SubtitleTranscriptionModels.fromId(selectedModelId)
        ?: SubtitleTranscriptionModels.default
    val installation = state.installation(model.id)
    val installed = installation is SubtitleModelInstallationState.Available
    val isActive = state.activeModelId == model.id
    val operation = state.operation?.takeIf { it.modelId == model.id }
    val running = operation is SubtitleModelOperation.Queued ||
        operation is SubtitleModelOperation.Downloading ||
        operation is SubtitleModelOperation.Verifying
    val anotherOperationRunning = state.operation != null &&
        state.operation.modelId != model.id &&
        state.operation !is SubtitleModelOperation.Failed
    val availableSources = configuredSubtitleModelDownloadSources(model)
    val selectedSource = operation?.source
        ?: SubtitleModelDownloadSource.fromId(selectedSourceIds[model.id])
        ?: availableSources.firstOrNull()
        ?: SubtitleModelDownloadSource.HuggingFace
    val colors = AsmrTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SubtitleTranscriptionModels.all.forEachIndexed { index, candidate ->
                SegmentedButton(
                    selected = model.id == candidate.id,
                    onClick = { selectedModelId = candidate.id },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = SubtitleTranscriptionModels.all.size
                    ),
                    colors = segmentedButtonColors,
                    icon = {},
                    modifier = Modifier.testTag("subtitle_model_choice_${candidate.id}"),
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.activeModelId == candidate.id) {
                                    stringResource(R.string.subtitle_model_current, candidate.optionName)
                                } else {
                                    candidate.optionName
                                },
                                maxLines = 1
                            )
                            Text(
                                text = Formatting.formatFileSize(candidate.artifactBytes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = when {
                    isActive && installed -> stringResource(R.string.subtitle_model_status_active_installed)
                    isActive -> stringResource(R.string.subtitle_model_status_active_not_installed)
                    installed -> stringResource(R.string.subtitle_model_status_installed)
                    else -> stringResource(R.string.subtitle_model_status_not_installed)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) colors.primaryStrong else colors.textSecondary,
                modifier = Modifier.testTag("subtitle_model_status_${model.id}")
            )
        }

        if (!installed && availableSources.size > 1) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                availableSources.forEachIndexed { index, source ->
                    SegmentedButton(
                        selected = selectedSource == source,
                        onClick = {
                            onClearFailure(model.id)
                            onSourceSelected(model.id, source)
                        },
                        enabled = !running && !anotherOperationRunning,
                        shape = SegmentedButtonDefaults.itemShape(index, availableSources.size),
                        colors = segmentedButtonColors,
                        icon = {},
                        label = { Text(source.displayName) }
                    )
                }
            }
        }

        when (operation) {
            null -> Unit
            is SubtitleModelOperation.Queued -> {
                Text(
                    stringResource(R.string.subtitle_model_waiting_download),
                    style = MaterialTheme.typography.bodySmall
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is SubtitleModelOperation.Downloading -> {
                Text(operation.stage.displayName, style = MaterialTheme.typography.bodySmall)
                if (operation.totalBytes > 0L) {
                    val progress = (operation.downloadedBytes.toFloat() / operation.totalBytes)
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            is SubtitleModelOperation.Verifying -> {
                Text(operation.stage.displayName, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is SubtitleModelOperation.Failed -> Text(
                text = operation.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        when {
            running -> FilledTonalButton(
                onClick = onCancelDownload,
                modifier = Modifier.fillMaxWidth(),
                colors = subtitleSettingsPrimaryTonalButtonColors()
            ) {
                Text(stringResource(R.string.subtitle_model_cancel_download))
            }
            installed && !isActive -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onSelect(model.id) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtitle_model_select_${model.id}"),
                    colors = subtitleSettingsPrimaryTonalButtonColors()
                ) {
                    Text(stringResource(R.string.subtitle_model_set_current))
                }
                FilledTonalButton(
                    onClick = { onDelete(model.id) },
                    colors = subtitleModelDeleteButtonColors()
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.subtitle_model_delete)
                    )
                }
            }
            installed -> FilledTonalButton(
                onClick = { onDelete(model.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = subtitleModelDeleteButtonColors()
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.subtitle_model_delete))
            }
            else -> FilledTonalButton(
                onClick = { onDownload(model.id, selectedSource) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subtitle_model_download_${model.id}"),
                enabled = deviceSupported && availableSources.contains(selectedSource) &&
                    !anotherOperationRunning,
                colors = subtitleSettingsPrimaryTonalButtonColors()
            ) {
                Text(
                    when {
                        !deviceSupported -> stringResource(R.string.subtitle_model_device_unsupported)
                        availableSources.isEmpty() -> stringResource(R.string.subtitle_model_source_unavailable)
                        anotherOperationRunning -> stringResource(R.string.subtitle_model_other_downloading)
                        operation is SubtitleModelOperation.Failed -> stringResource(R.string.subtitle_model_redownload)
                        else -> stringResource(R.string.subtitle_model_download)
                    }
                )
            }
        }
    }
}

@Composable
internal fun subtitleModelDeleteButtonColors(): ButtonColors =
    ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f),
        disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.48f)
    )

@Composable
internal fun subtitleSettingsPrimaryTonalButtonColors(): ButtonColors {
    val colorScheme = AsmrTheme.colorScheme
    val contentColor = if (colorScheme.isDark) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.primaryStrong
    }
    return ButtonDefaults.filledTonalButtonColors(
        containerColor = colorScheme.primarySoft,
        contentColor = contentColor,
        disabledContainerColor = colorScheme.primarySoft.copy(alpha = 0.48f),
        disabledContentColor = contentColor.copy(alpha = 0.48f)
    )
}
