package com.asmr.player.ui.groups

import com.asmr.player.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asmr.player.data.local.db.dao.AlbumGroupStatsRow
import com.asmr.player.data.local.db.entities.AlbumGroupEntity
import com.asmr.player.data.repository.AlbumGroupRepository
import com.asmr.player.data.repository.RenameAlbumGroupResult
import com.asmr.player.util.MessageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumGroupsViewModel @Inject constructor(
    private val groupRepository: AlbumGroupRepository,
    private val messageManager: MessageManager
) : ViewModel() {
    val groups: StateFlow<List<AlbumGroupStatsRow>> = groupRepository.observeGroupsWithStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val id = groupRepository.createGroup(trimmed)
            if (id != null) {
                messageManager.showSuccess(R.string.str_945ee611)
            } else {
                messageManager.showError(R.string.str_27ac29f2)
            }
        }
    }

    fun deleteGroup(group: AlbumGroupEntity) {
        viewModelScope.launch {
            groupRepository.deleteGroup(group)
            messageManager.showInfo(R.string.str_eb4aff77)
        }
    }

    fun renameGroup(groupId: Long, newName: String) {
        val trimmed = newName.trim()
        viewModelScope.launch {
            when (groupRepository.renameGroup(groupId, trimmed)) {
                RenameAlbumGroupResult.RENAMED -> messageManager.showSuccess(R.string.str_e6f61e54)
                RenameAlbumGroupResult.DUPLICATE -> messageManager.showError(R.string.str_27ac29f2)
                RenameAlbumGroupResult.INVALID -> messageManager.showError(R.string.str_fb7cf353)
                RenameAlbumGroupResult.NOT_FOUND -> messageManager.showError(R.string.str_c52a40c6)
            }
        }
    }

    fun addAlbumToGroupInBackground(
        groupId: Long,
        albumId: Long,
        onComplete: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                addAlbumToGroup(groupId, albumId)
                onComplete()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                messageManager.showError(R.string.str_32911c8a)
                onFailure(t)
            }
        }
    }

    suspend fun addAlbumToGroup(groupId: Long, albumId: Long) {
        if (groupId <= 0L || albumId <= 0L) return
        groupRepository.addAlbumToGroup(groupId, albumId)
        val group = groupRepository.getGroupById(groupId)
        val name = group?.name.orEmpty()
        messageManager.showSuccess(R.string.str_20bb9cd8)
    }
}
