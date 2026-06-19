package com.asmr.player.ui.search

import com.asmr.player.domain.model.Album
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchAlbumDetailMergeTest {
    @Test
    fun mergeSearchAlbumDetail_prefersCompleteDetailCvOverPartialListCv() {
        val base = Album(
            title = "作品",
            path = "",
            rjCode = "RJ123456",
            cv = "Alice",
            tags = listOf("耳语")
        )
        val detail = base.copy(
            cv = "Alice, Bob",
            tags = listOf("耳语", "安眠"),
            ratingValue = 4.8,
            ratingCount = 42,
            releaseDate = "2026-06-19"
        )

        val merged = mergeSearchAlbumDetail(base, detail)

        assertEquals("Alice, Bob", merged.cv)
        assertEquals(listOf("耳语"), merged.tags)
        assertEquals(4.8, merged.ratingValue ?: 0.0, 0.0)
        assertEquals(42, merged.ratingCount)
        assertEquals("2026-06-19", merged.releaseDate)
    }
}
