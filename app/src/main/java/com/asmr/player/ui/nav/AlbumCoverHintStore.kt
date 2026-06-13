package com.asmr.player.ui.nav

import java.util.concurrent.ConcurrentHashMap

/**
 * 轻量内存存储：在列表点击进入专辑详情时，记录列表已加载的封面 URL，
 * 供详情页在网络解析完成前先种入 [com.asmr.player.domain.model.Album.coverUrl]。
 *
 * 这样 hero 封面与列表卡片使用完全相同的图片 model（同一 coverUrl + 同一 keyTag），
 * 跨尺寸缓存复用（peekAnySize）即可命中，避免重复发起网络请求。
 *
 * 仅用于在线来源（搜索/热门收听）——本地专辑已带 coverPath，无需提示。
 */
object AlbumCoverHintStore {
    private const val MAX_ENTRIES = 64
    private val hints = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
            return size > MAX_ENTRIES
        }
    }
    private val lock = Any()

    fun record(albumId: Long?, rjCode: String?, coverUrl: String?) {
        val url = coverUrl?.trim().orEmpty()
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) return
        val rj = rjCode?.trim().orEmpty().uppercase()
        val id = albumId ?: 0L
        synchronized(lock) {
            if (rj.isNotBlank()) hints["rj:$rj"] = url
            if (id > 0L) hints["id:$id"] = url
        }
    }

    fun peek(albumId: Long?, rjCode: String?): String? {
        val rj = rjCode?.trim().orEmpty().uppercase()
        val id = albumId ?: 0L
        synchronized(lock) {
            if (rj.isNotBlank()) hints["rj:$rj"]?.let { return it }
            if (id > 0L) hints["id:$id"]?.let { return it }
        }
        return null
    }
}
