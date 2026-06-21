package com.asmr.player.data.remote.scraper

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

class DLSiteScraperCvParserTest {
    @Test
    fun parseCvNames_supportsTraditionalChineseVoiceActorLabel() {
        val doc = Jsoup.parse(
            """
            <div id="work_outline">
                <table>
                    <tr><th>聲優</th><td><a>かの仔</a></td></tr>
                </table>
            </div>
            """.trimIndent()
        )

        assertEquals(listOf("かの仔"), parseCvNames(doc))
    }

    @Test
    fun parseCvNames_supportsPlainTextNames() {
        val doc = Jsoup.parse(
            """
            <div id="work_outline">
                <table>
                    <tr><th>声優</th><td>かの仔、別名義</td></tr>
                </table>
            </div>
            """.trimIndent()
        )

        assertEquals(listOf("かの仔", "別名義"), parseCvNames(doc))
    }
}
