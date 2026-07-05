package com.asmr.player.shared.platform

enum class PlatformKind {
    Android,
    Windows,
    Linux,
    MacOs,
    IOS,
    Unknown
}

expect fun currentPlatform(): PlatformKind

expect fun platformDisplayName(): String

fun isDesktopPlatform(): Boolean = when (currentPlatform()) {
    PlatformKind.Windows, PlatformKind.Linux, PlatformKind.MacOs -> true
    else -> false
}
