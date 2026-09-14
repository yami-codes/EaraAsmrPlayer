package com.asmr.player.ui.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Search
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
    val mode: SearchFilterMode
) {
    Collected(
        labelRes = R.string.included,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_collected_library),
        mode = SearchFilterMode.CollectedOnly
    ),
    ChineseTranslated(
        labelRes = R.string.chinese_works,
        icon = SearchFilterIcon.Drawable(R.drawable.ic_search_chinese_book),
        mode = SearchFilterMode.ChineseTranslated
    ),
    Standard(
        labelRes = R.string.all_works,
        icon = SearchFilterIcon.Vector(Icons.Rounded.Search),
        mode = SearchFilterMode.Standard
    ),
    Presale(
        labelRes = R.string.pre_order,
        icon = SearchFilterIcon.Vector(Icons.Rounded.CalendarMonth),
        mode = SearchFilterMode.PresaleOnly
    ),
    PurchasedOnly(
        labelRes = R.string.purchased,
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

    val supportsWorkFilters: Boolean
        get() = mode == SearchFilterMode.CollectedOnly || mode == SearchFilterMode.Standard

    val supportsSortAndLanguageOptions: Boolean
        get() = mode != SearchFilterMode.PurchasedOnly && mode != SearchFilterMode.PresaleOnly

    companion object {
        fun fromState(
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
                else -> Standard
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
