package com.asmr.player.ui.search

enum class SearchSortOption(
    val label: String,
    val dlsiteOrder: String
) {
    Trend("人气顺序", "trend"),
    ReleaseNew("最新发售", "release_d"),
    DLCount("销量最高", "dl_d"),
    PriceHigh("价格最高", "price_d")
}

