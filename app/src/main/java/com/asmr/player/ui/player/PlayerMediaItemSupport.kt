package com.asmr.player.ui.player

import androidx.media3.common.MediaItem

internal fun MediaItem?.isOnlineMedia(): Boolean {
    val item = this ?: return false
    val uri = item.localConfiguration?.uri?.toString().orEmpty().trim()
    val mediaId = item.mediaId.trim()
    return uri.startsWith("http://", ignoreCase = true) ||
        uri.startsWith("https://", ignoreCase = true) ||
        mediaId.startsWith("http://", ignoreCase = true) ||
        mediaId.startsWith("https://", ignoreCase = true)
}
