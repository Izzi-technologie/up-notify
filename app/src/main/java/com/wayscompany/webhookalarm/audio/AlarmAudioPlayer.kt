package com.wayscompany.webhookalarm.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.wayscompany.webhookalarm.R
import com.wayscompany.webhookalarm.alarm.AlarmAudio
import com.wayscompany.webhookalarm.alarm.PlaybackRequest
import com.wayscompany.webhookalarm.utils.AlarmLogger

/**
 * Plays alarm audio from the service process.
 *
 * While an alert plays, [StreamVolumeControl] sets the alarm and media streams to the
 * alert's percentage of their maximum, then restores the previous TV volume.
 */
class AlarmAudioPlayer(
    context: Context,
    private val logger: AlarmLogger,
    private val onPlaybackState: (Boolean) -> Unit,
) : AlarmAudio {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val volumeControl = StreamVolumeControl(AndroidStreamVolumeDevice(audioManager, logger))
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var generation = 0
    private var playsLeft = 0
    private var looping = false
    private var volumeReceiverRegistered = false
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onMain { volumeControl.hold() }
        }
    }

    override fun play(request: PlaybackRequest, onFinished: () -> Unit) {
        onMain { playOnMain(request, onFinished) }
    }

    override fun stop() {
        onMain { stopOnMain(notify = true, restoreVolume = true) }
    }

    fun release() {
        stop()
    }

    private fun playOnMain(request: PlaybackRequest, onFinished: () -> Unit) {
        stopOnMain(notify = false, restoreVolume = false)
        val token = ++generation
        playsLeft = request.repeatCount.coerceAtLeast(1)
        looping = request.loop
        val created = MediaPlayer()
        player = created
        try {
            val resId = soundRes(request.soundName)
            val descriptor = appContext.resources.openRawResourceFd(resId)
            created.setAudioAttributes(alarmAttributes())
            created.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            descriptor.close()
            val level = playerVolume(request.volumePercent)
            created.setVolume(level, level)
            created.isLooping = request.loop
            created.setOnCompletionListener {
                onMain {
                    if (token != generation) return@onMain
                    handleCompletion(onFinished)
                }
            }
            created.setOnErrorListener { _, _, _ ->
                logger.e("Audio playback error")
                onMain {
                    if (token != generation) return@onMain
                    stopOnMain(notify = true, restoreVolume = true)
                    onFinished()
                }
                true
            }
            created.prepare()
            raiseVolume(request.volumePercent)
            created.start()
            requestFocus()
            onPlaybackState(true)
        } catch (error: Throwable) {
            logger.e("Audio playback error", error)
            stopOnMain(notify = true, restoreVolume = true)
            onFinished()
        }
    }

    private fun handleCompletion(onFinished: () -> Unit) {
        if (player == null || looping) return
        playsLeft -= 1
        if (playsLeft > 0) {
            try {
                player?.seekTo(0)
                player?.start()
            } catch (error: Throwable) {
                logger.e("Audio replay failed", error)
                stopOnMain(notify = true, restoreVolume = true)
                onFinished()
            }
        } else {
            stopOnMain(notify = true, restoreVolume = true)
            onFinished()
        }
    }

    private fun stopOnMain(notify: Boolean, restoreVolume: Boolean) {
        generation++
        val active = player
        player = null
        looping = false
        if (active != null) {
            try {
                active.setOnCompletionListener(null)
                active.setOnErrorListener(null)
                active.stop()
            } catch (_: Throwable) {
            }
            try {
                active.release()
            } catch (_: Throwable) {
            }
            abandonFocus()
            if (notify) onPlaybackState(false)
        }
        if (restoreVolume) restoreVolume()
    }

    private fun playerVolume(percent: Int): Float =
        if (audioManager.isVolumeFixed) percent.coerceIn(0, 100) / 100f else 1f

    private fun raiseVolume(percent: Int) {
        if (audioManager.isVolumeFixed) {
            logger.w("TV volume is fixed; alarm stays at the current level")
            return
        }
        val first = !volumeControl.isEngaged
        volumeControl.engage(percent)
        if (first) registerVolumeReceiver()
        logger.i("Alarm volume set to $percent% of maximum")
    }

    private fun restoreVolume() {
        if (!volumeControl.isEngaged) return
        unregisterVolumeReceiver()
        volumeControl.release()
        logger.i("TV volume restored")
    }

    private fun registerVolumeReceiver() {
        if (volumeReceiverRegistered) return
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        ContextCompat.registerReceiver(
            appContext,
            volumeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        volumeReceiverRegistered = true
    }

    private fun unregisterVolumeReceiver() {
        if (!volumeReceiverRegistered) return
        volumeReceiverRegistered = false
        runCatching { appContext.unregisterReceiver(volumeReceiver) }
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(alarmAttributes())
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun alarmAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun soundRes(name: String): Int = when (name) {
        "warning" -> R.raw.warning
        "critical" -> R.raw.critical
        else -> R.raw.info
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    private class AndroidStreamVolumeDevice(
        private val audioManager: AudioManager,
        private val logger: AlarmLogger,
    ) : StreamVolumeDevice {
        override val isFixed: Boolean get() = audioManager.isVolumeFixed

        override fun volume(stream: Int): Int = audioManager.getStreamVolume(stream)

        override fun min(stream: Int): Int =
            if (Build.VERSION.SDK_INT >= 28) audioManager.getStreamMinVolume(stream) else 0

        override fun max(stream: Int): Int = audioManager.getStreamMaxVolume(stream)

        override fun set(stream: Int, volume: Int) {
            try {
                audioManager.setStreamVolume(stream, volume, 0)
            } catch (error: Throwable) {
                logger.e("Unable to set alarm volume", error)
            }
        }
    }

    private companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
