package com.asmr.player.ui.nav

import com.asmr.player.domain.model.Album

/**
 * 轻量内存存储：在列表点击进入专辑详情时，记录列表已知的首屏信息，
 * 供详情页在网络解析完成前先种入标题、RJ、社团、CV、标签、评分与封面。
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
            cv = null,
            tags = emptyList(),
            coverUrl = coverUrl
        )
    }

    fun record(albumId: Long?, rjCode: String?, title: String?, circle: String?, coverUrl: String?) {
        record(
            albumId = albumId,
            rjCode = rjCode,
            title = title,
            circle = circle,
            cv = null,
            tags = emptyList(),
            coverUrl = coverUrl
        )
    }

    fun record(
        albumId: Long?,
        rjCode: String?,
        title: String?,
        circle: String?,
        cv: String?,
        coverUrl: String?,
        tags: List<String> = emptyList(),
        ratingValue: Double? = null,
        ratingCount: Int = 0,
        releaseDate: String? = null,
        dlCount: Int = 0,
        priceJpy: Int = 0,
        hasAsmrOne: Boolean = false,
        description: String? = null,
        hasResolvedDlsiteInfo: Boolean = false
    ) {
        val hint = AlbumCoverHint(
            title = title?.trim().orEmpty(),
            rjCode = rjCode?.trim().orEmpty().uppercase(),
            circle = circle?.trim().orEmpty(),
            cv = cv?.trim().orEmpty(),
            tags = tags
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct(),
            coverUrl = coverUrl?.trim().orEmpty().takeIf { it.startsWith("http", ignoreCase = true) }.orEmpty(),
            ratingValue = ratingValue?.takeIf { it > 0.0 },
            ratingCount = ratingCount.coerceAtLeast(0),
            releaseDate = releaseDate?.trim().orEmpty(),
            dlCount = dlCount.coerceAtLeast(0),
            priceJpy = priceJpy.coerceAtLeast(0),
            hasAsmrOne = hasAsmrOne,
            description = description?.trim().orEmpty(),
            hasResolvedDlsiteInfo = hasResolvedDlsiteInfo
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
    val cv: String,
    val tags: List<String>,
    val coverUrl: String,
    val ratingValue: Double?,
    val ratingCount: Int,
    val releaseDate: String,
    val dlCount: Int,
    val priceJpy: Int,
    val hasAsmrOne: Boolean,
    val description: String,
    val hasResolvedDlsiteInfo: Boolean
) {
    fun isBlank(): Boolean {
        return title.isBlank() &&
            rjCode.isBlank() &&
            circle.isBlank() &&
            cv.isBlank() &&
            tags.isEmpty() &&
            coverUrl.isBlank() &&
            ratingValue == null &&
            ratingCount <= 0 &&
            releaseDate.isBlank() &&
            dlCount <= 0 &&
            priceJpy <= 0 &&
            !hasAsmrOne &&
            description.isBlank()
    }
}

internal fun albumFromCoverHint(rj: String, hint: AlbumCoverHint?): Album {
    val normalizedRj = rj.ifBlank { hint?.rjCode.orEmpty() }
    return Album(
        title = hint?.title?.ifBlank { normalizedRj }.orEmpty().ifBlank { "专辑" },
        path = "",
        workId = normalizedRj,
        rjCode = normalizedRj,
        circle = hint?.circle.orEmpty(),
        cv = hint?.cv.orEmpty(),
        tags = hint?.tags.orEmpty(),
        coverUrl = hint?.coverUrl.orEmpty(),
        ratingValue = hint?.ratingValue,
        ratingCount = hint?.ratingCount ?: 0,
        releaseDate = hint?.releaseDate.orEmpty(),
        dlCount = hint?.dlCount ?: 0,
        priceJpy = hint?.priceJpy ?: 0,
        hasAsmrOne = hint?.hasAsmrOne == true,
        description = hint?.description.orEmpty()
    )
}
