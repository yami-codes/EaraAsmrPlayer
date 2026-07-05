package com.asmr.player.shared.platform

actual fun currentPlatform(): PlatformKind = PlatformKind.Android

actual fun platformDisplayName(): String = "Android"
