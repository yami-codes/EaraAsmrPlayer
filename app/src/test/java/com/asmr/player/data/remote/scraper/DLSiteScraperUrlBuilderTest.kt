package com.asmr.player.data.remote.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DLSiteScraperUrlBuilderTest {
    @Test
    fun buildDlsiteSearchUrls_includesPresaleFlagForPresaleOption() {
        val urls = buildDlsiteSearchUrls(
            keyword = "耳",
            page = 2,
            order = "trend",
            locale = "ja_JP",
            presaleOnly = true
        )

        assertEquals(2, urls.size)
        assertEquals(
            "https://www.dlsite.com/maniax/fsr/=/ana_flg/on/order/trend/work_type_category%5B0%5D/audio/keyword/%E8%80%B3/page/2",
            urls.first()
        )
        assertTrue(urls.all { it.contains("ana_flg/on") })
    }

    @Test
    fun buildDlsiteSearchUrls_usesExistingModernAndLegacyTemplatesForNormalSearch() {
        val urls = buildDlsiteSearchUrls(
            keyword = "耳",
            page = 2,
            order = "trend",
            locale = "ja_JP",
            presaleOnly = false
        )

        assertEquals(2, urls.size)
        assertTrue(urls.first().contains("order%5B0%5D/trend"))
        assertTrue(urls.first().contains("/keyword/%E8%80%B3/page/2"))
        assertTrue(urls.last().contains("/without_order/1/order/trend"))
        assertTrue(urls.none { it.contains("ana_flg/on") })
    }
}
