package com.wayscompany.webhookalarm

import android.media.AudioManager
import com.wayscompany.webhookalarm.audio.StreamVolumeControl
import com.wayscompany.webhookalarm.audio.StreamVolumeDevice
import com.wayscompany.webhookalarm.audio.targetStreamVolume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StreamVolumeControlTest {
    @Test
    fun engageUsesPercentOfMaximumThenRestores() {
        val device = FakeStreams(alarm = 2, music = 3)
        val control = StreamVolumeControl(device)

        control.engage(100)

        assertEquals(15, device.volume(AudioManager.STREAM_ALARM))
        assertEquals(25, device.volume(AudioManager.STREAM_MUSIC))
        control.release()
        assertEquals(2, device.volume(AudioManager.STREAM_ALARM))
        assertEquals(3, device.volume(AudioManager.STREAM_MUSIC))
        assertFalse(control.isEngaged)
    }

    @Test
    fun secondEngageKeepsTheOriginalVolume() {
        val device = FakeStreams(alarm = 2, music = 3)
        val control = StreamVolumeControl(device)

        control.engage(100)
        control.engage(40)
        control.release()

        assertEquals(2, device.volume(AudioManager.STREAM_ALARM))
        assertEquals(3, device.volume(AudioManager.STREAM_MUSIC))
    }

    @Test
    fun holdPutsBackAVolumeLoweredDuringTheAlarm() {
        val device = FakeStreams(alarm = 2, music = 1)
        val control = StreamVolumeControl(device)

        control.engage(100)
        device.set(AudioManager.STREAM_MUSIC, 1)
        device.set(AudioManager.STREAM_ALARM, 1)
        control.hold()

        assertEquals(15, device.volume(AudioManager.STREAM_ALARM))
        assertEquals(25, device.volume(AudioManager.STREAM_MUSIC))
    }

    @Test
    fun fixedVolumeIsLeftAlone() {
        val device = FakeStreams(alarm = 2, music = 3, isFixed = true)
        val control = StreamVolumeControl(device)

        control.engage(100)
        control.hold()

        assertEquals(2, device.volume(AudioManager.STREAM_ALARM))
        assertEquals(3, device.volume(AudioManager.STREAM_MUSIC))
        assertFalse(control.isEngaged)
    }

    @Test
    fun targetUsesTheStreamRange() {
        assertEquals(0, targetStreamVolume(min = 0, max = 15, percent = 0))
        assertEquals(6, targetStreamVolume(min = 0, max = 15, percent = 40))
        assertEquals(11, targetStreamVolume(min = 0, max = 15, percent = 75))
        assertEquals(15, targetStreamVolume(min = 0, max = 15, percent = 100))
        assertEquals(1, targetStreamVolume(min = 1, max = 15, percent = 0))
    }
}

private class FakeStreams(
    alarm: Int,
    music: Int,
    override val isFixed: Boolean = false,
) : StreamVolumeDevice {
    private val volumes = mutableMapOf(
        AudioManager.STREAM_ALARM to alarm,
        AudioManager.STREAM_MUSIC to music,
    )

    override fun volume(stream: Int): Int = volumes.getValue(stream)

    override fun min(stream: Int): Int = 0

    override fun max(stream: Int): Int = if (stream == AudioManager.STREAM_ALARM) 15 else 25

    override fun set(stream: Int, volume: Int) {
        volumes[stream] = volume
    }
}
