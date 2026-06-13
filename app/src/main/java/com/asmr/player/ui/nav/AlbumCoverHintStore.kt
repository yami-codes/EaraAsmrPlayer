package com.asmr.player.ui.nav

/**
 * 轻量内存存储：在列表点击进入专辑详情时，记录列表已知的首屏信息，
 * 供详情页在网络解析完成前先种入标题、RJ、社团与封面。
 *
 * 这样 hero 封面与列表卡片使用完全相同的图片 model（同一 coverUrl + 同一 keyTag），
 * 跨尺寸缓存复用（peekAnySize）即可命中，避免重复发起网络请求。
 */
object AlbumCoverHintStore {
    private const val MAX_ENTRIES = 64
    private val hints = object : LinkedHashMap<String, AlbumCoverHint>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AlbumCoverHint>): Boolean {
            return size > MAX_ENTRIES
        }
    }
    private val lock = Any()

    fun record(albumId: Long?, rjCode: String?, coverUrl: String?) {
        record(
            albumId = albumId,
            rjCode = rjCode,
            title = null,
            circle = null,
            coverUrl = coverUrl
        )
    }

    fun record(albumId: Long?, rjCode: String?, title: String?, circle: String?, coverUrl: String?) {
        val hint = AlbumCoverHint(
            title = title?.trim().orEmpty(),
            rjCode = rjCode?.trim().orEmpty().uppercase(),
            circle = circle?.trim().orEmpty(),
            coverUrl = coverUrl?.trim().orEmpty().takeIf { it.startsWith("http", ignoreCase = true) }.orEmpty()
        )
        if (hint.isBlank()) return
        val rj = hint.rjCode
        val id = albumId ?: 0L
        synchronized(lock) {
            if (rj.isNotBlank()) hints["rj:$rj"] = hint
            if (id > 0L) hints["id:$id"] = hint
        }
    }

    fun peek(albumId: Long?, rjCode: String?): String? {
        return peekHint(albumId, rjCode)?.coverUrl
    }

    fun peekHint(albumId: Long?, rjCode: String?): AlbumCoverHint? {
        val rj = rjCode?.trim().orEmpty().uppercase()
        val id = albumId ?: 0L
        synchronized(lock) {
            if (rj.isNotBlank()) hints["rj:$rj"]?.let { return it }
            if (id > 0L) hints["id:$id"]?.let { return it }
        }
        return null
    }
}

data class AlbumCoverHint(
    val title: String,
    val rjCode: String,
    val circle: String,
    val coverUrl: String
) {
    fun isBlank(): Boolean {
        return title.isBlank() && rjCode.isBlank() && circle.isBlank() && coverUrl.isBlank()
    }
}
