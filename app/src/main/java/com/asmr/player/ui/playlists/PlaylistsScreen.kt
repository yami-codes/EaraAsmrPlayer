package com.asmr.player.ui.playlists

import androidx.compose.ui.res.stringResource
import com.asmr.player.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asmr.player.data.local.db.dao.PlaylistStatsRow
import com.asmr.player.data.local.db.entities.PlaylistEntity
import com.asmr.player.ui.common.AsmrAsyncImage

import androidx.compose.ui.text.font.FontWeight
import com.asmr.player.ui.theme.AsmrTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.asmr.player.ui.common.FlatActionDialog
import com.asmr.player.ui.common.FlatDialogAction
import com.asmr.player.ui.common.FlatDialogActionTone
import com.asmr.player.ui.common.FlatTextFieldDialog
import com.asmr.player.ui.common.LocalBottomOverlayPadding
import com.asmr.player.ui.common.StableWindowInsets
import com.asmr.player.ui.common.smoothScrollToTop
import com.asmr.player.ui.common.EaraBrandedEmptyState
import com.asmr.player.ui.common.thinScrollbar
import com.asmr.player.ui.common.withAddedBottomPadding

private val PlaylistsPageHorizontalPadding = 8.dp
private val PlaylistRowActionButtonSize = 34.dp
private val PlaylistRowActionIconSize = 18.dp

@Composable
fun PlaylistsScreen(
    windowSizeClass: WindowSizeClass,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    scrollToTopSignal: Long = 0L,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val colorScheme = AsmrTheme.colorScheme
    val listState = rememberLazyListState()
    var showCreate by remember { mutableStateOf(false) }
    val userPlaylists = remember(playlists) { playlists.filter { it.category == "user" } }

    // 屏幕尺寸判断
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        contentWindowInsets = StableWindowInsets.navigationBars,
        containerColor = Color.Transparent,
        contentColor = colorScheme.onBackground
    ) { padding ->
        LaunchedEffect(scrollToTopSignal) {
            if (scrollToTopSignal == 0L) return@LaunchedEffect
            listState.smoothScrollToTop()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentModifier = if (isCompact) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
            }
            Box(modifier = contentModifier) {
                if (userPlaylists.isEmpty()) {
                    EaraBrandedEmptyState(
                        sectionTitle = stringResource(R.string.nav_playlists),
                        headline = stringResource(R.string.str_898a30d0),
                        sectionIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current + 88.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().thinScrollbar(listState),
                        contentPadding = PaddingValues(horizontal = PlaylistsPageHorizontalPadding, vertical = 8.dp)
                            .withAddedBottomPadding(LocalBottomOverlayPadding.current + 72.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(userPlaylists, key = { it.id }) { playlist ->
                            val entity = remember(playlist.id, playlist.name, playlist.category, playlist.createdAt) {
                                PlaylistEntity(
                                    id = playlist.id,
                                    name = playlist.name,
                                    category = playlist.category,
                                    createdAt = playlist.createdAt
                                )
                            }
                            PlaylistRow(
                                playlist = playlist,
                                onClick = { onPlaylistClick(entity) },
                                onDelete = { viewModel.deletePlaylist(entity) },
                                onRename = { newName -> viewModel.renamePlaylist(entity.id, newName) }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = PlaylistsPageHorizontalPadding, bottom = LocalBottomOverlayPadding.current + 16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreate = false
            }
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistStatsRow,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val colorScheme = AsmrTheme.colorScheme
    val cover = remember(playlist.firstArtworkUri) { playlist.firstArtworkUri?.trim().orEmpty() }
    var showDeleteConfirm by remember(playlist.id) { mutableStateOf(false) }
    var showRename by remember(playlist.id) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surface.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsmrAsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholderCornerRadius = 12,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.str_58a42f85, playlist.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.textTertiary
                )
            }
            IconButton(
                onClick = { showRename = true },
                enabled = playlist.category != "system",
                modifier = Modifier.size(PlaylistRowActionButtonSize)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = if (playlist.category != "system") colorScheme.textSecondary else colorScheme.textTertiary,
                    modifier = Modifier.size(PlaylistRowActionIconSize)
                )
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                enabled = playlist.category != "system",
                modifier = Modifier.size(PlaylistRowActionButtonSize)
            ) {
                Icon(
                    Icons.Rounded.Delete, 
                    contentDescription = null, 
                    tint = if (playlist.category != "system") colorScheme.danger.copy(alpha = 0.7f) else colorScheme.textTertiary,
                    modifier = Modifier.size(PlaylistRowActionIconSize)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        FlatActionDialog(
            onDismissRequest = { showDeleteConfirm = false },
            message = stringResource(R.string.str_e0788e07, playlist.name),
            actions = listOf(
                FlatDialogAction(stringResource(R.string.str_625fb26b), onClick = { showDeleteConfirm = false }),
                FlatDialogAction(
                    text = stringResource(R.string.str_2f4aaddd),
                    tone = FlatDialogActionTone.Danger,
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                )
            )
        )
    }

    if (showRename) {
        RenamePlaylistDialog(
            initialName = playlist.name,
            onDismiss = { showRename = false },
            onRename = { newName ->
                showRename = false
                onRename(newName)
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    FlatTextFieldDialog(
        onDismissRequest = onDismiss,
        message = stringResource(R.string.str_451d9b45),
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.str_ade3663e),
        confirmText = stringResource(R.string.str_d9ac9228),
        confirmEnabled = name.trim().isNotBlank(),
        onConfirm = { onCreate(name.trim()) },
    )
}

@Composable
private fun RenamePlaylistDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    FlatTextFieldDialog(
        onDismissRequest = onDismiss,
        message = stringResource(R.string.str_0abc6c57),
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.str_ade3663e),
        confirmText = stringResource(R.string.str_38cf16f2),
        confirmEnabled = name.trim().isNotBlank() && name.trim() != initialName.trim(),
        onConfirm = { onRename(name.trim()) },
    )
}
