package com.asmr.player.shared.platform

actual fun currentPlatform(): PlatformKind = PlatformKind.IOS

actual fun platformDisplayName(): String = "iOS"
