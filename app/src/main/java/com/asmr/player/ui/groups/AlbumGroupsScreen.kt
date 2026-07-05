package com.asmr.player.ui.groups

import androidx.compose.ui.res.stringResource
import com.asmr.player.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asmr.player.data.local.db.dao.AlbumGroupStatsRow
import com.asmr.player.data.local.db.entities.AlbumGroupEntity
import com.asmr.player.ui.common.AsmrAsyncImage
import com.asmr.player.ui.common.EaraBrandedEmptyState
import com.asmr.player.ui.common.FlatActionDialog
import com.asmr.player.ui.common.FlatDialogAction
import com.asmr.player.ui.common.FlatDialogActionTone
import com.asmr.player.ui.common.FlatTextFieldDialog
import com.asmr.player.ui.common.LocalBottomOverlayPadding
import com.asmr.player.ui.common.StableWindowInsets
import com.asmr.player.ui.common.smoothScrollToTop
import com.asmr.player.ui.common.thinScrollbar
import com.asmr.player.ui.common.withAddedBottomPadding
import com.asmr.player.ui.theme.AsmrTheme

private val AlbumGroupsPageHorizontalPadding = 8.dp
private val AlbumGroupRowActionButtonSize = 34.dp
private val AlbumGroupRowActionIconSize = 18.dp

@Composable
fun AlbumGroupsScreen(
    windowSizeClass: WindowSizeClass,
    onGroupClick: (AlbumGroupEntity) -> Unit,
    scrollToTopSignal: Long = 0L,
    viewModel: AlbumGroupsViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val colorScheme = AsmrTheme.colorScheme
    val listState = rememberLazyListState()
    var showCreate by remember { mutableStateOf(false) }

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
                if (groups.isEmpty()) {
                    EaraBrandedEmptyState(
                        sectionTitle = stringResource(R.string.nav_groups),
                        headline = stringResource(R.string.no_groups_created_yet),
                        sectionIcon = Icons.Rounded.Folder,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current + 88.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().thinScrollbar(listState),
                        contentPadding = PaddingValues(horizontal = AlbumGroupsPageHorizontalPadding, vertical = 8.dp)
                            .withAddedBottomPadding(LocalBottomOverlayPadding.current + 72.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groups, key = { it.id }) { group ->
                            val entity = remember(group.id, group.name, group.createdAt) {
                                AlbumGroupEntity(
                                    id = group.id,
                                    name = group.name,
                                    createdAt = group.createdAt
                                )
                            }
                            AlbumGroupRow(
                                group = group,
                                onClick = { onGroupClick(entity) },
                                onDelete = { viewModel.deleteGroup(entity) },
                                onRename = { newName -> viewModel.renameGroup(entity.id, newName) }
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
                        .padding(end = AlbumGroupsPageHorizontalPadding, bottom = LocalBottomOverlayPadding.current + 16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
        }
    }

    if (showCreate) {
        CreateAlbumGroupDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                viewModel.createGroup(name)
                showCreate = false
            }
        )
    }
}

@Composable
private fun AlbumGroupRow(
    group: AlbumGroupStatsRow,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val colorScheme = AsmrTheme.colorScheme
    val cover = remember(group.firstArtworkUri) { group.firstArtworkUri?.trim().orEmpty() }
    var showDeleteConfirm by remember(group.id) { mutableStateOf(false) }
    var showRename by remember(group.id) { mutableStateOf(false) }

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
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.albums_tracks, group.albumCount, group.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.textTertiary
                )
            }
            IconButton(onClick = { showRename = true }, modifier = Modifier.size(AlbumGroupRowActionButtonSize)) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = colorScheme.textSecondary,
                    modifier = Modifier.size(AlbumGroupRowActionIconSize)
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(AlbumGroupRowActionButtonSize)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = colorScheme.danger.copy(alpha = 0.7f),
                    modifier = Modifier.size(AlbumGroupRowActionIconSize)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        FlatActionDialog(
            onDismissRequest = { showDeleteConfirm = false },
            message = stringResource(R.string.you_sure_you_want_delete_group, group.name),
            actions = listOf(
                FlatDialogAction(stringResource(R.string.cancel), onClick = { showDeleteConfirm = false }),
                FlatDialogAction(
                    text = stringResource(R.string.delete),
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
        RenameAlbumGroupDialog(
            initialName = group.name,
            onDismiss = { showRename = false },
            onRename = { newName ->
                showRename = false
                onRename(newName)
            }
        )
    }
}

@Composable
private fun CreateAlbumGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    FlatTextFieldDialog(
        onDismissRequest = onDismiss,
        message = stringResource(R.string.enter_new_group_name),
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.group_name),
        confirmText = stringResource(R.string.create),
        confirmEnabled = name.trim().isNotBlank(),
        onConfirm = { onCreate(name.trim()) },
    )
}

@Composable
private fun RenameAlbumGroupDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    FlatTextFieldDialog(
        onDismissRequest = onDismiss,
        message = stringResource(R.string.enter_new_group_name_2),
        value = name,
        onValueChange = { name = it },
        placeholder = stringResource(R.string.group_name),
        confirmText = stringResource(R.string.dialog_confirm),
        confirmEnabled = name.trim().isNotBlank(),
        onConfirm = { onRename(name.trim()) },
    )
}
