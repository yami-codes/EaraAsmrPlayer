package com.asmr.player.ui.search

import androidx.annotation.StringRes
import com.asmr.player.R

enum class SearchSortOption(
    @StringRes val labelRes: Int,
    val dlsiteOrder: String
) {
    Trend(R.string.search_sort_trend, "trend"),
    ReleaseNew(R.string.search_sort_release_new, "release_d"),
    DLCount(R.string.search_sort_dl_count, "dl_d"),
    PriceHigh(R.string.search_sort_price_high, "price_d")
}

enum class SearchCollectedSortOption(
    @StringRes val labelRes: Int,
    val backendSort: String
) {
    ReleaseNew(R.string.search_sort_release_new, "release"),
    RatingHigh(R.string.search_sort_rating_high, "rating");

    companion object {
        fun fromName(name: String?): SearchCollectedSortOption {
            return values().firstOrNull { it.name == name } ?: ReleaseNew
        }
    }
}
