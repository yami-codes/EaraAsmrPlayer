package com.asmr.player.ui.playlists

import com.asmr.player.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.asmr.player.data.local.db.dao.PlaylistStatsRow
import com.asmr.player.data.local.db.entities.PlaylistEntity
import com.asmr.player.data.repository.PlaylistAddSummary
import com.asmr.player.data.repository.PlaylistRepository
import com.asmr.player.data.repository.RenamePlaylistResult
import com.asmr.player.util.MessageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val messageManager: MessageManager
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistStatsRow>> = playlistRepository.observePlaylistsWithStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            playlistRepository.ensureSystemPlaylists()
        }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val id = playlistRepository.createUserPlaylist(trimmed)
            if (id != null) {
                messageManager.showSuccess(R.string.str_1cf66abb)
            } else {
                messageManager.showError(R.string.str_6e1bdee8)
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
            messageManager.showInfo(R.string.str_d1c6154e)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        viewModelScope.launch {
            when (playlistRepository.renamePlaylist(playlistId, trimmed)) {
                RenamePlaylistResult.RENAMED -> messageManager.showSuccess(R.string.str_e6f61e54)
                RenamePlaylistResult.DUPLICATE -> messageManager.showError(R.string.str_6e1bdee8)
                RenamePlaylistResult.INVALID -> messageManager.showError(R.string.str_16ec0511)
                RenamePlaylistResult.NOT_FOUND -> messageManager.showError(R.string.str_ca72ca5e)
            }
        }
    }

    suspend fun addItemToPlaylist(playlistId: Long, item: MediaItem): Boolean {
        return addItemsToPlaylist(playlistId, listOf(item)).addedCount > 0
    }

    fun addItemsToFavoritesInBackground(items: List<MediaItem>) {
        viewModelScope.launch {
            try {
                addItemsToFavorites(items)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                messageManager.showError(R.string.str_a17fe9f2)
            }
        }
    }

    fun addItemsToPlaylistInBackground(
        playlistId: Long,
        items: List<MediaItem>,
        onComplete: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                addItemsToPlaylist(playlistId, items)
                onComplete()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                messageManager.showError(R.string.str_94ebd9da)
                onFailure(t)
            }
        }
    }

    suspend fun addItemsToFavorites(items: List<MediaItem>): PlaylistAddSummary {
        val favoritesId = playlistRepository.getOrCreateFavoritesPlaylistId()
        val summary = playlistRepository.addItemsToPlaylist(favoritesId, items)
        showAddSummary(PlaylistRepository.PLAYLIST_FAVORITES, summary)
        return summary
    }

    suspend fun addItemsToPlaylist(playlistId: Long, items: List<MediaItem>): PlaylistAddSummary {
        val summary = playlistRepository.addItemsToPlaylist(playlistId, items)
        val name = playlistRepository.getPlaylistById(playlistId)?.name.orEmpty()
        showAddSummary(name, summary)
        return summary
    }

    private fun showAddSummary(playlistName: String, summary: PlaylistAddSummary) {
        when {
            summary.addedCount > 0 && summary.skippedCount > 0 ->
                messageManager.showSuccess(R.string.str_26abe922)
            summary.addedCount > 0 ->
                messageManager.showSuccess(R.string.str_69b6fdea)
            summary.totalCount > 0 ->
                messageManager.showInfo(R.string.str_b71144cb)
            else -> Unit
        }
    }
}
