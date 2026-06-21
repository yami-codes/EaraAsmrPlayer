package com.asmr.player.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import com.asmr.player.ui.common.AsmrAsyncImage
import com.asmr.player.ui.theme.AsmrTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class MiniPlayerDisplayMode {
    CoverOnly,
    Expanded
}

private data class MiniPlayerProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val fraction: Float
        get() = if (durationMs > 0L) {
            (positionMs.toDouble() / durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

private fun sameMiniPlayerMediaItem(old: MediaItem?, new: MediaItem?): Boolean {
    if (old === new) return true
    if (old == null || new == null) return false
    return old.mediaId == new.mediaId &&
        old.localConfiguration?.uri == new.localConfiguration?.uri &&
        old.mediaMetadata.artworkUri == new.mediaMetadata.artworkUri &&
        old.mediaMetadata.title?.toString() == new.mediaMetadata.title?.toString() &&
        old.mediaMetadata.artist?.toString() == new.mediaMetadata.artist?.toString()
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MiniPlayer(
    displayMode: MiniPlayerDisplayMode,
    onDisplayModeChange: (MiniPlayerDisplayMode) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    largeLayout: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentItemState by remember(viewModel) {
        viewModel.playback
            .map { it.currentMediaItem }
            .distinctUntilChanged(::sameMiniPlayerMediaItem)
    }.collectAsState(initial = null)
    val isPlaying by remember(viewModel) {
        viewModel.playback
            .map { it.isPlaying }
            .distinctUntilChanged()
    }.collectAsState(initial = false)
    val progressState by remember(viewModel, displayMode) {
        if (displayMode == MiniPlayerDisplayMode.Expanded) {
            viewModel.playback
                .map { MiniPlayerProgress(it.positionMs, it.durationMs) }
                .distinctUntilChanged()
        } else {
            flowOf(MiniPlayerProgress())
        }
    }.collectAsState(initial = MiniPlayerProgress())
    val isFavorite by viewModel.isFavorite.collectAsState()
    val item = currentItemState ?: return
    val metadata = item.mediaMetadata
    val colorScheme = AsmrTheme.colorScheme
    val currentMediaId = item.mediaId
    val barHeight = if (largeLayout) 64.dp else 56.dp
    val coverSize = if (largeLayout) 60.dp else 52.dp
    val coverInset = 2.dp
    val miniPlayerBorderColor = if (colorScheme.isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        colorScheme.primaryStrong.copy(alpha = 0.14f)
    }

    var optimisticIsPlaying by remember { mutableStateOf<Boolean?>(null) }
    var stableMediaId by remember { mutableStateOf(currentMediaId) }
    val isPlayingEffective = optimisticIsPlaying ?: isPlaying

    LaunchedEffect(currentMediaId) {
        if (currentMediaId != stableMediaId) {
            stableMediaId = currentMediaId
            optimisticIsPlaying = null
        }
    }

    LaunchedEffect(optimisticIsPlaying) {
        if (optimisticIsPlaying != null) {
            delay(1_500)
            optimisticIsPlaying = null
        }
    }

    val progress = progressState.fraction

    AnimatedContent(
        targetState = displayMode,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) togetherWith fadeOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) using SizeTransform(clip = false)
        },
        label = "miniPlayerMode"
    ) { mode ->
        when (mode) {
            MiniPlayerDisplayMode.CoverOnly -> {
                MiniPlayerCoverOnly(
                    artworkModel = metadata.artworkUri,
                    isPlaying = isPlayingEffective,
                    onExpand = { onDisplayModeChange(MiniPlayerDisplayMode.Expanded) },
                    largeLayout = largeLayout
                )
            }

            MiniPlayerDisplayMode.Expanded -> {
                BoxWithConstraints {
                    val widthProgress = ((maxWidth.value - 220f) / 280f).coerceIn(0f, 1f)
                    val controlsSpacing = ((if (largeLayout) 4f else 2f) + (if (largeLayout) 6f else 5f) * widthProgress).dp
                    val controlsButtonSize = ((if (largeLayout) 32f else 28f) + (if (largeLayout) 8f else 6f) * widthProgress).dp
                    val favoriteIconSize = if (largeLayout) 19.dp else 17.dp
                    val playbackIconSize = if (largeLayout) 22.dp else 20.dp
                    val queueIconSize = if (largeLayout) 20.dp else 18.dp

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight),
                        shape = RoundedCornerShape(
                            topStart = coverSize / 2,
                            bottomStart = coverSize / 2,
                            topEnd = if (largeLayout) 26.dp else 22.dp,
                            bottomEnd = if (largeLayout) 26.dp else 22.dp
                        ),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = lerp(
                                colorScheme.surface,
                                colorScheme.primarySoft,
                                if (colorScheme.isDark) 0.05f else 0.08f
                            ).copy(alpha = if (colorScheme.isDark) 0.93f else 0.95f)
                                .compositeOver(colorScheme.background)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = coverInset, top = coverInset)
                                        .size(coverSize)
                                        .clip(CircleShape)
                                        .background(colorScheme.surfaceVariant)
                                        .clickable { onDisplayModeChange(MiniPlayerDisplayMode.CoverOnly) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsmrAsyncImage(
                                        model = metadata.artworkUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        placeholderCornerRadius = 52,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = if (largeLayout) 12.dp else 10.dp, end = 8.dp)
                                        .clickable(onClick = onOpenNowPlaying),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = metadata.title?.toString().orEmpty().ifBlank { "未播放" },
                                        modifier = Modifier.basicMarquee(),
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.textPrimary
                                    )
                                    Text(
                                        text = metadata.artist?.toString().orEmpty(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(controlsSpacing)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite() },
                                        modifier = Modifier.size(controlsButtonSize)
                                    ) {
                                        Icon(
                                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) Color.Red else colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(favoriteIconSize)
                                    )
                                }
                                    IconButton(
                                        onClick = {
                                            optimisticIsPlaying = !(optimisticIsPlaying ?: isPlaying)
                                            viewModel.togglePlayPause()
                                        },
                                        modifier = Modifier.size(controlsButtonSize)
                                    ) {
                                        Icon(
                                        imageVector = if (isPlayingEffective) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(playbackIconSize)
                                    )
                                }
                                    IconButton(
                                        onClick = onOpenQueue,
                                        modifier = Modifier.size(controlsButtonSize)
                                    ) {
                                        Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = colorScheme.onSurface,
                                        modifier = Modifier.size(queueIconSize)
                                    )
                                }
                            }
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (largeLayout) 3.dp else 2.dp),
                                color = colorScheme.primary,
                                trackColor = colorScheme.primary.copy(alpha = 0.1f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = 1.dp,
                                color = miniPlayerBorderColor,
                                shape = RoundedCornerShape(
                                    topStart = coverSize / 2,
                                    bottomStart = coverSize / 2,
                                    topEnd = if (largeLayout) 26.dp else 22.dp,
                                    bottomEnd = if (largeLayout) 26.dp else 22.dp
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerCoverOnly(
    artworkModel: Any?,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    largeLayout: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = AsmrTheme.colorScheme
    val compactMinWidth = if (largeLayout) 76.dp else 64.dp
    val barHeight = if (largeLayout) 64.dp else 56.dp
    val coverSize = if (largeLayout) 60.dp else 52.dp
    val miniPlayerBorderColor = if (colorScheme.isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        colorScheme.primaryStrong.copy(alpha = 0.14f)
    }
    Box(
        modifier = modifier
            .widthIn(min = compactMinWidth)
            .height(barHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(CircleShape)
                .background(colorScheme.surface)
                .border(width = 1.dp, color = miniPlayerBorderColor, shape = CircleShape)
                .clickable(onClick = onExpand),
            contentAlignment = Alignment.Center
        ) {
            AsmrAsyncImage(
                model = artworkModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholderCornerRadius = if (largeLayout) 60 else 52,
                modifier = Modifier.fillMaxSize()
            )
            MiniPlayerActivityBars(
                isPlaying = isPlaying,
                modifier = Modifier.align(Alignment.Center),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MiniPlayerActivityBars(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "miniPlayerBars")
    val heights = listOf(0, 60, 120, 180, 240, 300).map { delayMs ->
        transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 520,
                    delayMillis = delayMs,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "miniPlayerBar$delayMs"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, height ->
            val maxHeight = 8.2f + (index % 3) * 1.8f + index * 0.35f
            Box(
                modifier = Modifier
                    .size(
                        width = 2.6.dp,
                        height = (if (isPlaying) 4f + height.value * maxHeight else 4f).dp
                    )
                    .clip(RoundedCornerShape(99.dp))
                    .background(tint)
            )
        }
    }
}
