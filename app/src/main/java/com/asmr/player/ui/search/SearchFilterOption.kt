package com.asmr.player.ui.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
import androidx.compose.ui.graphics.vector.ImageVector

enum class SearchFilterOption(
    val label: String,
    val icon: ImageVector,
    val mode: SearchFilterMode,
    val sortOption: SearchSortOption? = null
) {
    PurchasedOnly(
        label = "已购",
        icon = Icons.Default.Lock,
        mode = SearchFilterMode.PurchasedOnly
    ),
    ChineseTranslated(
        label = "中文作品",
        icon = Icons.Default.AutoStories,
        mode = SearchFilterMode.ChineseTranslated
    ),
    Presale(
        label = "预售",
        icon = Icons.Default.Schedule,
        mode = SearchFilterMode.PresaleOnly
    ),
    Trend(
        label = SearchSortOption.Trend.label,
        icon = Icons.Default.LocalFireDepartment,
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.Trend
    ),
    ReleaseNew(
        label = SearchSortOption.ReleaseNew.label,
        icon = Icons.Default.NewReleases,
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.ReleaseNew
    ),
    DLCount(
        label = SearchSortOption.DLCount.label,
        icon = Icons.Default.Sell,
        mode = SearchFilterMode.Standard,
        sortOption = SearchSortOption.DLCount
    ),
    PriceHigh(
        label = SearchSortOption.PriceHigh.label,
        icon = Icons.Default.Payments,
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
