package com.wayscompany.webhookalarm.audio

import android.media.AudioManager

interface StreamVolumeDevice {
    val isFixed: Boolean
    fun volume(stream: Int): Int
    fun min(stream: Int): Int
    fun max(stream: Int): Int
    fun set(stream: Int, volume: Int)
}

/**
 * Raises alarm and media streams to the alert's own level, then restores the TV volume.
 * The level is a percentage of the stream range, so a lowered TV volume does not cap it.
 */
class StreamVolumeControl(
    private val device: StreamVolumeDevice,
    private val streams: IntArray = intArrayOf(AudioManager.STREAM_ALARM, AudioManager.STREAM_MUSIC),
) {
    private var saved: Map<Int, Int>? = null
    private var percent: Int? = null

    val isEngaged: Boolean get() = saved != null

    fun engage(volumePercent: Int) {
        if (device.isFixed) return
        if (saved == null) {
            saved = streams.associateWith { stream -> device.volume(stream) }
        }
        percent = volumePercent.coerceIn(0, 100)
        apply()
    }

    fun hold() {
        if (!isEngaged || device.isFixed) return
        apply()
    }

    fun release() {
        val previous = saved ?: return
        saved = null
        percent = null
        previous.forEach { (stream, volume) -> device.set(stream, volume) }
    }

    private fun apply() {
        val level = percent ?: return
        streams.forEach { stream ->
            val target = targetStreamVolume(device.min(stream), device.max(stream), level)
            if (device.volume(stream) != target) {
                device.set(stream, target)
            }
        }
    }
}

fun targetStreamVolume(min: Int, max: Int, percent: Int): Int {
    val low = min.coerceAtLeast(0)
    val high = max.coerceAtLeast(low)
    if (percent <= 0) return low
    if (high == low) return high
    val scaled = low + (high - low) * percent.coerceIn(0, 100) / 100
    return scaled.coerceIn(low, high)
}
