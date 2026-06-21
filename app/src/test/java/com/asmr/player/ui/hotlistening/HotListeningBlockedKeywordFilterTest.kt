package com.asmr.player.ui.hotlistening

import com.asmr.player.domain.model.Album
import com.asmr.player.hotlistening.HotListeningSortMode
import org.junit.Assert.assertEquals
import org.junit.Test

class HotListeningBlockedKeywordFilterTest {
    @Test
    fun filterHotListeningEntries_keepsAllEntriesWhenBlockedKeywordsAreBlank() {
        val entries = listOf(
            entry(title = "雨音诊所", rjCode = "RJ100001"),
            entry(title = "深夜耳语", rjCode = "RJ100002")
        )

        val result = filterHotListeningEntries(
            entries = entries,
            blockedKeywords = listOf("", " - ")
        )

        assertEquals(entries, result.visibleEntries)
        assertEquals(emptyList<HotListeningEntry>(), result.blockedEntries)
    }

    @Test
    fun filterHotListeningEntries_blocksByAlbumMetadataIgnoringCase() {
        val visible = entry(title = "雨音诊所", circle = "青空工房", rjCode = "RJ100001")
        val blockedByTitle = entry(title = "猫娘咖啡厅", rjCode = "RJ100002")
        val blockedByCircle = entry(title = "深夜散步", circle = "Voice Lab", rjCode = "RJ100003")
        val blockedByCv = entry(title = "耳语练习", cv = "小铃", rjCode = "RJ100004")
        val blockedByTag = entry(title = "安眠向导", tags = listOf("催眠"), rjCode = "RJ100005")
        val blockedByRj = entry(title = "夏日回声", rjCode = "RJ999999")

        val result = filterHotListeningEntries(
            entries = listOf(
                visible,
                blockedByTitle,
                blockedByCircle,
                blockedByCv,
                blockedByTag,
                blockedByRj
            ),
            blockedKeywords = listOf("猫娘", "voice", "小铃", "催眠", "rj999999")
        )

        assertEquals(listOf(visible), result.visibleEntries)
        assertEquals(
            listOf(blockedByTitle, blockedByCircle, blockedByCv, blockedByTag, blockedByRj),
            result.blockedEntries
        )
    }

    @Test
    fun filterHotListeningEntries_trimsDeduplicatesAndNormalizesLeadingMinus() {
        val visible = entry(title = "雨音诊所", rjCode = "RJ100001")
        val blocked = entry(title = "猫娘咖啡厅", rjCode = "RJ100002")

        val result = filterHotListeningEntries(
            entries = listOf(visible, blocked),
            blockedKeywords = listOf("  -猫娘  ", "猫娘", "CAT")
        )

        assertEquals(listOf(visible), result.visibleEntries)
        assertEquals(listOf(blocked), result.blockedEntries)
    }

    private fun entry(
        title: String,
        circle: String = "",
        cv: String = "",
        tags: List<String> = emptyList(),
        rjCode: String = ""
    ): HotListeningEntry {
        return HotListeningEntry(
            album = Album(
                title = title,
                path = "",
                circle = circle,
                cv = cv,
                tags = tags,
                rjCode = rjCode
            ),
            playCount = 10,
            listenDurationMs = 60_000L,
            sortMode = HotListeningSortMode.PlayCount
        )
    }
}
