package com.asmr.player.ui.hotlistening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asmr.player.domain.model.Album
import com.asmr.player.ui.common.EaraBrandedEmptyState
import com.asmr.player.ui.common.EaraLogoLoadingIndicator
import com.asmr.player.ui.common.LocalBottomOverlayPadding
import com.asmr.player.ui.common.thinScrollbar
import com.asmr.player.ui.common.withAddedBottomPadding
import com.asmr.player.ui.library.AlbumGridItem
import com.asmr.player.ui.library.AlbumGridItemSpacing
import com.asmr.player.ui.library.AlbumItem
import com.asmr.player.ui.theme.AsmrTheme
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private fun stableAlbumKey(album: Album): String {
    val id = album.rjCode.ifBlank { album.workId }.trim()
    if (id.isNotEmpty()) return id
    val seed = "${album.coverUrl}|${album.title}|${album.circle}|${album.cv}"
    return "h${seed.hashCode().absoluteValue}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotListeningScreen(
    windowSizeClass: WindowSizeClass,
    onAlbumClick: (Album) -> Unit,
    scrollToTopSignal: Long = 0L,
    viewModel: HotListeningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val colorScheme = AsmrTheme.colorScheme
    val scope = rememberCoroutineScope()
    val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0, 0) }
    val gridState = rememberSaveable(saver = LazyStaggeredGridState.Saver) { LazyStaggeredGridState() }

    val periods = listOf("day" to "过去一天", "week" to "过去一周", "month" to "过去一月")
    val currentPeriod = (uiState as? HotListeningUiState.Success)?.period ?: "day"

    fun scrollToTop() {
        scope.launch {
            runCatching { listState.scrollToItem(0) }
            runCatching { gridState.scrollToItem(0) }
        }
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal == 0L) return@LaunchedEffect
        when (viewMode) {
            0 -> runCatching { listState.animateScrollToItem(0) }
            else -> runCatching { gridState.animateScrollToItem(0) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            periods.forEach { (period, label) ->
                FilterChip(
                    selected = currentPeriod == period,
                    onClick = {
                        viewModel.selectPeriod(period)
                        scrollToTop()
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = colorScheme.primary
                    )
                )
            }
        }

        when (val state = uiState) {
            is HotListeningUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EaraLogoLoadingIndicator(tint = colorScheme.primary)
            }

            is HotListeningUiState.Error -> EaraBrandedEmptyState(
                sectionTitle = "热门收听",
                headline = "数据加载失败",
                description = state.message,
                sectionIcon = Icons.Default.Whatshot,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current + 24.dp),
                footer = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("重试")
                    }
                }
            )

            is HotListeningUiState.Success -> {
                if (state.albums.isEmpty()) {
                    EaraBrandedEmptyState(
                        sectionTitle = "热门收听",
                        headline = "暂无排行数据",
                        description = "热门收听排行数据正在统计中，请稍后再来查看。",
                        sectionIcon = Icons.Default.Whatshot,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current + 24.dp)
                    )
                } else if (viewMode == 0) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .thinScrollbar(listState),
                        contentPadding = PaddingValues(bottom = 8.dp)
                            .withAddedBottomPadding(LocalBottomOverlayPadding.current)
                    ) {
                        lazyItems(
                            items = state.albums,
                            key = { album -> stableAlbumKey(album) },
                            contentType = { "album" }
                        ) { album ->
                            AlbumItem(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                emptyCoverUseShimmer = true
                            )
                        }
                    }
                } else {
                    val adaptiveCellSize = if (isCompactWidth) 150.dp else 200.dp
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(adaptiveCellSize),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .thinScrollbar(gridState),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 16.dp
                        ).withAddedBottomPadding(LocalBottomOverlayPadding.current),
                        horizontalArrangement = Arrangement.spacedBy(AlbumGridItemSpacing),
                        verticalItemSpacing = AlbumGridItemSpacing
                    ) {
                        items(
                            state.albums.size,
                            key = { index -> stableAlbumKey(state.albums[index]) },
                            contentType = { "albumGrid" }
                        ) { index ->
                            val album = state.albums[index]
                            AlbumGridItem(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                emptyCoverUseShimmer = true
                            )
                        }
                    }
                }
            }
        }
    }
}
