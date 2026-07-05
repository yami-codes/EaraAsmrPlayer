package com.asmr.player.shared.platform

actual fun currentPlatform(): PlatformKind {
    val os = System.getProperty("os.name")?.lowercase().orEmpty()
    return when {
        os.contains("windows") -> PlatformKind.Windows
        os.contains("linux") -> PlatformKind.Linux
        os.contains("mac") || os.contains("darwin") -> PlatformKind.MacOs
        else -> PlatformKind.Unknown
    }
}

actual fun platformDisplayName(): String = when (currentPlatform()) {
    PlatformKind.Windows -> "Windows"
    PlatformKind.Linux -> "Linux"
    PlatformKind.MacOs -> "macOS"
    else -> System.getProperty("os.name").orEmpty()
}
