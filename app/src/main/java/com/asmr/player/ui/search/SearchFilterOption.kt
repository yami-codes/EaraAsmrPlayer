package com.asmr.player.ui.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.asmr.player.R

sealed class SearchFilterIcon {
    data class Vector(val imageVector: ImageVector) : SearchFilterIcon()

    data class Drawable(@DrawableRes val resId: Int) : SearchFilterIcon()
}

enum class SearchFilterOption(
    @StringRes val labelRes: Int,
    val icon: SearchFilterIcon,
    val mode: SearchFilterMode,
    val sortOption: SearchSortOption? = null
) {
    Collected(
        labelRes = R.string.str_b811f9b1,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_collected_library),
        mode = SearchFilterMode.CollectedOnly
    ),
    ChineseTranslated(
        labelRes = R.string.str_1ba04371,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_chinese_book),
        mode = SearchFilterMode.ChineseTranslated
    ),
    Trend(
        labelRes = SearchSortOption.Trend.labelRes,
        icon = SearchFilterIcon.Vector(Icons.Rounded.LocalFireDepartment),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.Trend
    ),
    ReleaseNew(
        labelRes = SearchSortOption.ReleaseNew.labelRes,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_new_releases_new),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.ReleaseNew
    ),
    DLCount(
        labelRes = SearchSortOption.DLCount.labelRes,
        icon = SearchFilterIcon.Vector(Icons.Rounded.EmojiEvents),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.DLCount
    ),
    PriceHigh(
        labelRes = SearchSortOption.PriceHigh.labelRes,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_badge_japanese_yen),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.PriceHigh
    ),
    Presale(
        labelRes = R.string.str_8d863b73,
        icon = SearchFilterIcon.Vector(Icons.Rounded.CalendarMonth),
        mode = SearchFilterMode.PresaleOnly
    ),
    PurchasedOnly(
        labelRes = R.string.str_5af67104,
        icon = SearchFilterIcon.Vector(Icons.Rounded.ShoppingBag),
        mode = SearchFilterMode.PurchasedOnly
    );

    val isPurchasedOnly: Boolean
        get() = mode == SearchFilterMode.PurchasedOnly

    val isPresaleOnly: Boolean
        get() = mode == SearchFilterMode.PresaleOnly

    val isChineseTranslated: Boolean
        get() = mode == SearchFilterMode.ChineseTranslated

    val isCollectedOnly: Boolean
        get() = mode == SearchFilterMode.CollectedOnly

    companion object {
        fun fromState(
            order: SearchSortOption,
            purchasedOnly: Boolean,
            presaleOnly: Boolean,
            chineseTranslatedOnly: Boolean,
            collectedOnly: Boolean
        ): SearchFilterOption {
            return when {
                purchasedOnly -> PurchasedOnly
                chineseTranslatedOnly -> ChineseTranslated
                presaleOnly -> Presale
                collectedOnly -> Collected
                else -> values().firstOrNull { it.sortOption == order } ?: Trend
            }
        }
    }
}

enum class SearchFilterMode {
    Standard,
    PurchasedOnly,
    PresaleOnly,
    ChineseTranslated,
    CollectedOnly
}
