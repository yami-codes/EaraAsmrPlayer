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
}
