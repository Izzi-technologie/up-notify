package com.wayscompany.webhookalarm.alarm

data class PlaybackRequest(
    val soundName: String,
    val volumePercent: Int,
    val loop: Boolean,
    val repeatCount: Int,
)

interface AlarmAudio {
    fun play(request: PlaybackRequest, onFinished: () -> Unit)
    fun stop()
}
