package com.asmr.player.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asmr.player.R
import com.asmr.player.subtitle.SubtitleItemState
import com.asmr.player.subtitle.SubtitleTaskItemUi
import com.asmr.player.subtitle.SubtitleTaskMode
import com.asmr.player.subtitle.SubtitleTaskUi
import com.asmr.player.subtitle.normalizedSubtitleAlbumKey
import com.asmr.player.ui.common.AsmrAsyncImage
import com.asmr.player.ui.common.DiscPlaceholder
import com.asmr.player.ui.common.albumCoverImageModel
import com.asmr.player.ui.common.rememberCalmScrollableFlingBehavior
import com.asmr.player.ui.theme.AsmrTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.layout.ContentScale

internal enum class DownloadManagementMode {
    Downloads,
    Translations
}

@Composable
internal fun DownloadManagementModeTabs(
    selected: DownloadManagementMode,
    activeDownloadFileCount: Int,
    activeTranslationTaskCount: Int,
    onSelected: (DownloadManagementMode) -> Unit
) {
    val colors = AsmrTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.55f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DownloadManagementMode.entries.forEach { mode ->
            val isSelected = selected == mode
            val activeTaskCount = when (mode) {
                DownloadManagementMode.Downloads -> activeDownloadFileCount
                DownloadManagementMode.Translations -> activeTranslationTaskCount
            }
            val label = when (mode) {
                DownloadManagementMode.Downloads -> stringResource(R.string.download_mgmt_downloads_tab)
                DownloadManagementMode.Translations -> stringResource(R.string.download_mgmt_translations_tab)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colors.primarySoft else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelected(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (mode) {
                            DownloadManagementMode.Downloads -> Icons.Rounded.Download
                            DownloadManagementMode.Translations -> Icons.Rounded.Translate
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) colors.primaryStrong else colors.textSecondary
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) colors.primaryStrong else colors.textSecondary
                    )
                    if (activeTaskCount > 0) {
                        Badge(
                            containerColor = colors.primaryStrong,
                            contentColor = colors.onPrimary
                        ) {
                            Text(activeTaskCount.toString())
                        }
                    }
                }
            }
        }
    }
}

internal fun countActiveSubtitleTaskItems(tasks: List<SubtitleTaskUi>): Int {
    return tasks.sumOf { task ->
        task.items.count { item -> item.state.isActivelyRunning() }
    }
}

@Composable
internal fun TranslationManagementContent(
    normalizedQuery: String,
    listState: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    subtitleGroups: List<TranslationSubtitleGroupUi>,
    subtitleTasks: List<SubtitleTaskUi>,
    polishingRjCodes: Set<String>,
    loadAlbumCovers: suspend (List<Long>) -> Map<Long, TaskAlbumCoverUi>,
    onDeleteSubtitle: (trackId: Long, title: String) -> Unit,
    onDeleteSubtitleGroup: (rjCode: String, trackIds: List<Long>) -> Unit,
    onRetrySubtitle: (trackId: Long, title: String) -> Unit,
    onPolishAlbum: (rjCode: String) -> Unit,
    onPauseItem: (String) -> Unit,
    onResumeItem: (String) -> Unit,
    onCancelItem: (String) -> Unit,
    onRetryItem: (String) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit
) {
    val context = LocalContext.current
    val displayedTasks = remember(subtitleTasks) {
        subtitleTasks.flatMap { task -> task.items.map { item -> item.toTranslationTaskUi(task, context) } }
    }
    val taskTrackIds = remember(displayedTasks) { displayedTasks.map(TranslationTaskUi::trackId).distinct() }
    val taskAlbumCovers by produceState<Map<Long, TaskAlbumCoverUi>>(
        initialValue = emptyMap(),
        key1 = taskTrackIds
    ) {
        value = loadAlbumCovers(taskTrackIds)
    }
    val unknownWorkId = stringResource(R.string.unknown_work_id)
    val groups = remember(
        displayedTasks,
        subtitleGroups,
        taskAlbumCovers,
        polishingRjCodes,
        normalizedQuery,
        unknownWorkId
    ) {
        val tasksByRj = displayedTasks
            .sortedWith(
                compareBy<TranslationTaskUi> { it.state.translationTaskSortPriority() }
                    .thenByDescending(TranslationTaskUi::createdAtMillis)
            )
            .groupBy { it.rjCode }
        val subtitleGroupsByRj = subtitleGroups.associateBy(TranslationSubtitleGroupUi::rjCode)
        (tasksByRj.keys + subtitleGroupsByRj.keys)
            .asSequence()
            .filter { rjCode ->
                normalizedQuery.isBlank() || rjCode.equals(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(compareBy<String> { it == unknownWorkId }.thenBy { it.lowercase() })
            .map { rjCode ->
                val subtitleGroup = subtitleGroupsByRj[rjCode]
                val tasks = tasksByRj[rjCode].orEmpty()
                val albumCover = sequenceOf(
                    subtitleGroup?.albumCover,
                    tasks.asSequence()
                        .mapNotNull { task -> taskAlbumCovers[task.trackId] }
                        .firstOrNull { it.hasSource() }
                ).filterNotNull()
                    .firstOrNull { it.hasSource() }
                    ?: TaskAlbumCoverUi()
                TranslationTaskGroupUi(
                    rjCode = rjCode,
                    title = subtitleGroup?.title?.takeIf { it.isNotBlank() }
                        ?: tasks.firstOrNull { it.title.isNotBlank() }?.title.orEmpty(),
                    albumCover = albumCover,
                    subtitles = subtitleGroup?.subtitles.orEmpty(),
                    tasks = tasks,
                    isPolishing = polishingRjCodes.contains(rjCode.normalizedSubtitleAlbumKey())
                )
            }
            .filter { it.subtitles.isNotEmpty() || it.tasks.isNotEmpty() }
            .toList()
    }
    val expandedGroups = remember { mutableStateListOf<String>() }

    if (groups.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (normalizedQuery.isBlank()) {
                    stringResource(R.string.no_translation_tasks)
                } else {
                    stringResource(R.string.task_not_found, normalizedQuery)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberCalmScrollableFlingBehavior(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding + 6.dp)
    ) {
        items(groups, key = { it.rjCode }) { group ->
            TranslationTaskGroupCard(
                group = group,
                expanded = expandedGroups.contains(group.rjCode),
                onToggleExpanded = {
                    if (expandedGroups.contains(group.rjCode)) expandedGroups.remove(group.rjCode)
                    else expandedGroups.add(group.rjCode)
                },
                onDeleteSubtitle = onDeleteSubtitle,
                onDeleteSubtitleGroup = onDeleteSubtitleGroup,
                onRetrySubtitle = onRetrySubtitle,
                onPolishAlbum = onPolishAlbum,
                onPauseItem = onPauseItem,
                onResumeItem = onResumeItem,
                onCancelItem = onCancelItem,
                onRetryItem = onRetryItem,
                onPauseTask = onPauseTask,
                onResumeTask = onResumeTask,
                onCancelTask = onCancelTask
            )
        }
    }
}

private data class TranslationTaskGroupUi(
    val rjCode: String,
    val title: String,
    val albumCover: TaskAlbumCoverUi,
    val subtitles: List<TranslationSubtitleUi>,
    val tasks: List<TranslationTaskUi>,
    val isPolishing: Boolean = false
)

internal data class TranslationTaskUi(
    val itemId: String,
    val taskId: String,
    val createdAtMillis: Long,
    val trackId: Long,
    val rjCode: String,
    val title: String,
    val state: String,
    val progress: Float?,
    val progressLabel: String,
    val completedLines: Int,
    val totalLines: Int,
    val stage: String,
    val message: String
)

private fun TaskAlbumCoverUi.hasImageSource(): Boolean = hasSource()

@Composable
private fun TranslationTaskGroupCard(
    group: TranslationTaskGroupUi,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDeleteSubtitle: (trackId: Long, title: String) -> Unit,
    onDeleteSubtitleGroup: (rjCode: String, trackIds: List<Long>) -> Unit,
    onRetrySubtitle: (trackId: Long, title: String) -> Unit,
    onPolishAlbum: (rjCode: String) -> Unit,
    onPauseItem: (String) -> Unit,
    onResumeItem: (String) -> Unit,
    onCancelItem: (String) -> Unit,
    onRetryItem: (String) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onCancelTask: (String) -> Unit
) {
    val colors = AsmrTheme.colorScheme
    val activeTask = remember(group.tasks) { group.tasks.firstOrNull { it.state.isActivelyRunning() } }
    val controlledTask = remember(group.tasks, activeTask) { activeTask ?: group.tasks.firstOrNull() }
    val summary = remember(group.subtitles, group.tasks, activeTask, group.isPolishing) {
        when {
            group.isPolishing -> "polishing"
            activeTask != null -> activeTask.stage
            group.subtitles.isNotEmpty() -> "subtitles"
            else -> "empty"
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.rjCode, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (group.title.isNotBlank()) {
                        Text(group.title, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
                    }
                }
                Text(
                    text = when (summary) {
                        "polishing" -> stringResource(R.string.translation_polishing)
                        "subtitles" -> stringResource(R.string.translation_subtitle_count, group.subtitles.size)
                        "empty" -> stringResource(R.string.translation_no_subtitles)
                        else -> summary
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activeTask != null || group.isPolishing) colors.primary else colors.textSecondary
                )
                TaskGroupCover(group.albumCover)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                controlledTask?.let { task ->
                    when (task.state) {
                        SubtitleItemState.PAUSED, SubtitleItemState.INTERRUPTED, SubtitleItemState.FAILED -> {
                            IconButton(onClick = { onResumeTask(task.taskId) }, enabled = !group.isPolishing) {
                                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.subtitle_task_resume))
                            }
                        }
                        else -> {
                            IconButton(onClick = { onPauseTask(task.taskId) }, enabled = !group.isPolishing) {
                                Icon(Icons.Rounded.Pause, stringResource(R.string.subtitle_task_pause))
                            }
                        }
                    }
                    IconButton(onClick = { onCancelTask(task.taskId) }, enabled = !group.isPolishing) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.subtitle_task_cancel))
                    }
                } ?: run {
                    IconButton(onClick = { onPolishAlbum(group.rjCode) }, enabled = !group.isPolishing) {
                        Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.translation_polish_album))
                    }
                    IconButton(
                        onClick = { onDeleteSubtitleGroup(group.rjCode, group.subtitles.map { it.trackId }) },
                        enabled = !group.isPolishing
                    ) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.translation_delete_all_subtitles))
                    }
                }
            }
            if (expanded) {
                val taskByTrackId = group.tasks.associateBy(TranslationTaskUi::trackId)
                group.subtitles.forEach { subtitle ->
                    TranslationSubtitleRow(
                        subtitle = subtitle,
                        task = taskByTrackId[subtitle.trackId],
                        actionsEnabled = !group.isPolishing,
                        onDelete = { onDeleteSubtitle(subtitle.trackId, subtitle.title) },
                        onRetry = { onRetrySubtitle(subtitle.trackId, subtitle.title) },
                        onPause = taskByTrackId[subtitle.trackId]?.takeIf { it.state.isActivelyRunning() }?.let { { onPauseItem(it.itemId) } },
                        onResume = taskByTrackId[subtitle.trackId]?.takeIf {
                            it.state in setOf(SubtitleItemState.PAUSED, SubtitleItemState.INTERRUPTED)
                        }?.let { { onResumeItem(it.itemId) } },
                        onCancel = taskByTrackId[subtitle.trackId]?.let { { onCancelItem(it.itemId) } },
                        onRetryTask = taskByTrackId[subtitle.trackId]?.takeIf { it.state == SubtitleItemState.FAILED }
                            ?.let { { onRetryItem(it.itemId) } }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = colors.onSurfaceVariant.copy(alpha = 0.2f))
                }
                group.tasks.filter { task -> group.subtitles.none { it.trackId == task.trackId } }.forEach { task ->
                    TranslationTaskRow(
                        task = task,
                        actionsEnabled = !group.isPolishing,
                        onPause = { onPauseItem(task.itemId) },
                        onResume = { onResumeItem(task.itemId) },
                        onCancel = { onCancelItem(task.itemId) },
                        onRetry = { onRetryItem(task.itemId) }
                    )
                }
            }
        }
        if (group.isPolishing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun TaskGroupCover(albumCover: TaskAlbumCoverUi) {
    val coverModel = remember(albumCover.coverThumbPath, albumCover.coverPath, albumCover.coverUrl) {
        albumCoverImageModel(
            coverThumbPath = albumCover.coverThumbPath,
            coverPath = albumCover.coverPath,
            coverUrl = albumCover.coverUrl
        )
    }
    if (coverModel == null) {
        DiscPlaceholder(cornerRadius = 8, modifier = Modifier.size(48.dp))
    } else {
        AsmrAsyncImage(
            model = coverModel,
            contentDescription = stringResource(R.string.player_view_cover),
            contentScale = ContentScale.Crop,
            placeholderCornerRadius = 8,
            fadeIn = false,
            peekAnySizeForInitial = true,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun TranslationSubtitleRow(
    subtitle: TranslationSubtitleUi,
    task: TranslationTaskUi?,
    actionsEnabled: Boolean,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPause: (() -> Unit)?,
    onResume: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onRetryTask: (() -> Unit)?
) {
    val colors = AsmrTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Subtitles, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(subtitle.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = task?.message?.ifBlank { task.stage }
                    ?: stringResource(R.string.translation_subtitle_lines, subtitle.subtitleCount),
                style = MaterialTheme.typography.labelSmall,
                color = if (task?.state == SubtitleItemState.FAILED) colors.danger else colors.textTertiary,
                maxLines = 2
            )
            task?.progress?.let { progress ->
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
        onResume?.let { IconButton(onClick = it, enabled = actionsEnabled) { Icon(Icons.Rounded.PlayArrow, null) } }
        onPause?.let { IconButton(onClick = it, enabled = actionsEnabled) { Icon(Icons.Rounded.Pause, null) } }
        onRetryTask?.let { IconButton(onClick = it, enabled = actionsEnabled) { Icon(Icons.Rounded.Refresh, null) } }
        onCancel?.let { IconButton(onClick = it, enabled = actionsEnabled) { Icon(Icons.Rounded.Close, null) } }
        if (task == null) {
            IconButton(onClick = onRetry, enabled = actionsEnabled) { Icon(Icons.Rounded.Refresh, null) }
            IconButton(onClick = onDelete, enabled = actionsEnabled) { Icon(Icons.Rounded.Delete, null) }
        }
    }
}

@Composable
internal fun TranslationTaskRow(
    task: TranslationTaskUi,
    actionsEnabled: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = AsmrTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Subtitles, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (task.state in setOf(SubtitleItemState.TRANSCRIBING, SubtitleItemState.TRANSLATING) && task.progress == null) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                }
                Text(
                    task.message.ifBlank { task.stage },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.state == SubtitleItemState.FAILED) colors.danger else colors.textTertiary,
                    maxLines = 2
                )
            }
            task.progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
        }
        when (task.state) {
            SubtitleItemState.PAUSED, SubtitleItemState.INTERRUPTED -> IconButton(onClick = onResume, enabled = actionsEnabled) {
                Icon(Icons.Rounded.PlayArrow, null)
            }
            SubtitleItemState.FAILED -> IconButton(onClick = onRetry, enabled = actionsEnabled) {
                Icon(Icons.Rounded.Refresh, null)
            }
            else -> IconButton(onClick = onPause, enabled = actionsEnabled) {
                Icon(Icons.Rounded.Pause, null)
            }
        }
        IconButton(onClick = onCancel, enabled = actionsEnabled) { Icon(Icons.Rounded.Close, null) }
    }
}

private fun SubtitleTaskItemUi.toTranslationTaskUi(task: SubtitleTaskUi, context: android.content.Context): TranslationTaskUi {
    val usesTranscriptionProgress = translationTotal <= 0 && mode == SubtitleTaskMode.GENERATED
    val fraction = when {
        translationTotal > 0 -> translationCursor.toFloat() / translationTotal.toFloat()
        usesTranscriptionProgress -> transcriptionProgress / 100f
        else -> null
    }?.coerceIn(0f, 1f)
    return TranslationTaskUi(
        itemId = id,
        taskId = task.id,
        createdAtMillis = createdAt,
        trackId = trackId,
        rjCode = task.rjCode,
        title = title,
        state = state,
        progress = fraction,
        progressLabel = when {
            usesTranscriptionProgress -> "$transcriptionProgress%"
            translationTotal > 0 -> context.getString(R.string.translation_progress_confirmed, translationCursor, translationTotal)
            else -> ""
        },
        completedLines = translationCursor,
        totalLines = translationTotal,
        stage = subtitleItemStage(this, context),
        message = errorMessage.takeIf { state == SubtitleItemState.FAILED }.orEmpty()
    )
}

private fun subtitleItemStage(item: SubtitleTaskItemUi, context: android.content.Context): String = when (item.state) {
    SubtitleItemState.QUEUED_TRANSCRIPTION -> context.getString(R.string.subtitle_stage_queued_transcription)
    SubtitleItemState.TRANSCRIBING -> context.getString(R.string.subtitle_stage_transcribing)
    SubtitleItemState.QUEUED_TRANSLATION -> context.getString(R.string.subtitle_stage_queued_translation)
    SubtitleItemState.WAITING_SLOT -> context.getString(R.string.subtitle_stage_waiting_slot)
    SubtitleItemState.WAITING_NETWORK -> context.getString(R.string.subtitle_stage_waiting_network)
    SubtitleItemState.TRANSLATING -> context.getString(R.string.subtitle_stage_translating)
    SubtitleItemState.RETRY_WAIT -> context.getString(R.string.subtitle_stage_retry_wait, item.attempt + 1, item.errorMessage.asStageReason())
    SubtitleItemState.PAUSE_REQUESTED -> context.getString(R.string.subtitle_stage_pause_requested)
    SubtitleItemState.PAUSED -> context.getString(R.string.subtitle_stage_paused)
    SubtitleItemState.INTERRUPTED -> context.getString(R.string.subtitle_stage_interrupted, item.errorMessage.asStageReason())
    SubtitleItemState.CANCEL_REQUESTED -> context.getString(R.string.subtitle_stage_cancel_requested)
    SubtitleItemState.FAILED -> context.getString(R.string.subtitle_stage_failed)
    SubtitleItemState.SUCCEEDED -> context.getString(R.string.subtitle_stage_succeeded)
    SubtitleItemState.CANCELED -> context.getString(R.string.subtitle_stage_canceled)
    else -> item.state
}

private fun String.asStageReason(): String = trim().takeIf(String::isNotEmpty)?.let { ": $it" }.orEmpty()

private fun String.isActivelyRunning(): Boolean = this !in setOf(
    SubtitleItemState.PAUSED,
    SubtitleItemState.INTERRUPTED,
    SubtitleItemState.FAILED,
    SubtitleItemState.SUCCEEDED,
    SubtitleItemState.CANCELED
)

private fun String.translationTaskSortPriority(): Int = when (this) {
    SubtitleItemState.TRANSCRIBING, SubtitleItemState.TRANSLATING -> 0
    SubtitleItemState.QUEUED_TRANSCRIPTION,
    SubtitleItemState.QUEUED_TRANSLATION,
    SubtitleItemState.WAITING_SLOT,
    SubtitleItemState.WAITING_NETWORK,
    SubtitleItemState.RETRY_WAIT -> 1
    SubtitleItemState.PAUSE_REQUESTED, SubtitleItemState.CANCEL_REQUESTED -> 2
    SubtitleItemState.PAUSED, SubtitleItemState.INTERRUPTED -> 3
    SubtitleItemState.FAILED -> 4
    else -> 5
}
