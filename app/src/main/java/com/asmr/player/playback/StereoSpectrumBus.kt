package com.asmr.player.playback

import androidx.media3.common.util.UnstableApi

@UnstableApi
object StereoSpectrumBus {
    const val DefaultBinCount: Int = 128
    val store: StereoSpectrumStore = StereoSpectrumStore(DefaultBinCount)
    @Volatile
    var playbackActive: Boolean = false
    @Volatile
    var captureActive: Boolean = false
        private set

    private var captureConsumerCount: Int = 0

    @Synchronized
    fun registerCaptureConsumer() {
        captureConsumerCount += 1
        captureActive = true
    }

    @Synchronized
    fun unregisterCaptureConsumer() {
        captureConsumerCount = (captureConsumerCount - 1).coerceAtLeast(0)
        captureActive = captureConsumerCount > 0
    }
}
