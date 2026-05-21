package com.asmr.player.ui.search

enum class SearchFilterOption(
    val label: String,
    val isPurchasedOnly: Boolean = false,
    val isPresaleOnly: Boolean = false,
    val sortOption: SearchSortOption? = null
) {
    PurchasedOnly(label = "仅已购", isPurchasedOnly = true),
    Presale(label = "预售", isPresaleOnly = true),
    Trend(label = SearchSortOption.Trend.label, sortOption = SearchSortOption.Trend),
    ReleaseNew(label = SearchSortOption.ReleaseNew.label, sortOption = SearchSortOption.ReleaseNew),
    ReleaseOld(label = SearchSortOption.ReleaseOld.label, sortOption = SearchSortOption.ReleaseOld),
    DLCount(label = SearchSortOption.DLCount.label, sortOption = SearchSortOption.DLCount),
    PriceLow(label = SearchSortOption.PriceLow.label, sortOption = SearchSortOption.PriceLow),
    PriceHigh(label = SearchSortOption.PriceHigh.label, sortOption = SearchSortOption.PriceHigh),
    Rating(label = SearchSortOption.Rating.label, sortOption = SearchSortOption.Rating),
    ReviewCount(label = SearchSortOption.ReviewCount.label, sortOption = SearchSortOption.ReviewCount);

    companion object {
        fun fromState(
            order: SearchSortOption,
            purchasedOnly: Boolean,
            presaleOnly: Boolean
        ): SearchFilterOption {
            return when {
                purchasedOnly -> PurchasedOnly
                presaleOnly -> Presale
                else -> values().firstOrNull { it.sortOption == order } ?: Trend
            }
        }
    }
}
