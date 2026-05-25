package com.asmr.player.ui.search

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.asmr.player.R

sealed class SearchFilterIcon {
    data class Vector(val imageVector: ImageVector) : SearchFilterIcon()

    data class Drawable(@DrawableRes val resId: Int) : SearchFilterIcon()
}

enum class SearchFilterOption(
    val label: String,
    val icon: SearchFilterIcon,
    val mode: SearchFilterMode,
    val sortOption: SearchSortOption? = null
) {
    PurchasedOnly(
        label = "已购",
        icon = SearchFilterIcon.Vector(Icons.Default.ShoppingBag),
        mode = SearchFilterMode.PurchasedOnly
    ),
    ChineseTranslated(
        label = "中文作品",
        icon = SearchFilterIcon.Vector(Icons.Default.AutoStories),
        mode = SearchFilterMode.ChineseTranslated
    ),
    Presale(
        label = "预售",
        icon = SearchFilterIcon.Vector(Icons.Default.CalendarMonth),
        mode = SearchFilterMode.PresaleOnly
    ),
    Trend(
        label = SearchSortOption.Trend.label,
        icon = SearchFilterIcon.Vector(Icons.Default.LocalFireDepartment),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.Trend
    ),
    ReleaseNew(
        label = SearchSortOption.ReleaseNew.label,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_new_releases_new),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.ReleaseNew
    ),
    DLCount(
        label = SearchSortOption.DLCount.label,
        icon = SearchFilterIcon.Vector(Icons.Default.EmojiEvents),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.DLCount
    ),
    PriceHigh(
        label = SearchSortOption.PriceHigh.label,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_badge_japanese_yen),
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.PriceHigh
    );

    val isPurchasedOnly: Boolean
        get() = mode == SearchFilterMode.PurchasedOnly

    val isPresaleOnly: Boolean
        get() = mode == SearchFilterMode.PresaleOnly

    val isChineseTranslated: Boolean
        get() = mode == SearchFilterMode.ChineseTranslated

    companion object {
        fun fromState(
            order: SearchSortOption,
            purchasedOnly: Boolean,
            presaleOnly: Boolean,
            chineseTranslatedOnly: Boolean
        ): SearchFilterOption {
            return when {
                purchasedOnly -> PurchasedOnly
                chineseTranslatedOnly -> ChineseTranslated
                presaleOnly -> Presale
                else -> values().firstOrNull { it.sortOption == order } ?: Trend
            }
        }
    }
}

enum class SearchFilterMode {
    Standard,
    PurchasedOnly,
    PresaleOnly,
    ChineseTranslated
}
