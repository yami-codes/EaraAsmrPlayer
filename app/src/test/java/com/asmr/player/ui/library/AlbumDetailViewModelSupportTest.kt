package com.asmr.player.ui.library

import com.asmr.player.domain.model.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailViewModelSupportTest {
    @Test
    fun buildDisplayAlbum_keepsFallbackCvWhenDlsiteInfoHasNoCv() {
        val localAlbum = Album(
            title = "作品",
            path = "",
            cv = "かの仔",
            workId = "RJ01572724",
            rjCode = "RJ01572724"
        )

        val result = buildDisplayAlbum(
            rjCode = "RJ01572724",
            localAlbum = localAlbum,
            dlsiteInfo = localAlbum.copy(cv = ""),
            asmrOneWorkId = null,
            fallbackCv = "かの仔",
            fallbackCoverUrl = ""
        )

        assertEquals("かの仔", result.cv)
        assertTrue(result.rjCode.isNotBlank())
    }
    @Test
    fun mergeDetailHeaderAlbum_preservesListMetadataWhenRequested() {
        val listAlbum = Album(
            title = "列表标题",
            path = "",
            rjCode = "RJ123456",
            cv = "列表CV A, 列表CV B",
            tags = listOf("列表标签"),
            coverUrl = "https://example.com/list.jpg"
        )
        val fetched = listAlbum.copy(
            title = "抓取标题",
            cv = "抓取CV",
            tags = listOf("抓取标签"),
            coverUrl = "https://example.com/fetched.jpg"
        )

        val result = mergeDetailHeaderAlbum(
            currentDisplayAlbum = listAlbum,
            localAlbum = null,
            fetchedDlsiteInfo = fetched,
            rjCode = "RJ123456",
            asmrOneWorkId = "789",
            preserveHeaderAlbumMetadata = true
        )

        assertEquals("列表标题", result.title)
        assertEquals("列表CV A, 列表CV B", result.cv)
        assertEquals(listOf("列表标签"), result.tags)
        assertEquals("https://example.com/list.jpg", result.coverUrl)
        assertEquals("789", result.workId)
    }

    @Test
    fun mergeDetailHeaderAlbum_allowsFetchedMetadataWhenNotPreserved() {
        val current = Album(
            title = "RJ123456",
            path = "",
            rjCode = "RJ123456",
            cv = "",
            tags = emptyList()
        )
        val fetched = current.copy(
            title = "抓取标题",
            cv = "抓取CV",
            tags = listOf("抓取标签")
        )

        val result = mergeDetailHeaderAlbum(
            currentDisplayAlbum = current,
            localAlbum = null,
            fetchedDlsiteInfo = fetched,
            rjCode = "RJ123456",
            asmrOneWorkId = null,
            preserveHeaderAlbumMetadata = false
        )

        assertEquals("抓取标题", result.title)
        assertEquals("抓取CV", result.cv)
        assertEquals(listOf("抓取标签"), result.tags)
    }
}
