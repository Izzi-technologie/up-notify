package com.wayscompany.webhookalarm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.wayscompany.webhookalarm.R
import com.wayscompany.webhookalarm.alarm.AlarmAudio
import com.wayscompany.webhookalarm.alarm.PlaybackRequest
import com.wayscompany.webhookalarm.utils.AlarmLogger

/**
 * Plays alarm audio from the service process.
 *
 * Volume is applied with [MediaPlayer.setVolume] on [AudioAttributes.USAGE_ALARM].
 * That scales only this player. The TV's system volume is left unchanged and remains the ceiling.
 */
class AlarmAudioPlayer(
    context: Context,
    private val logger: AlarmLogger,
    private val onPlaybackState: (Boolean) -> Unit,
) : AlarmAudio {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var generation = 0
    private var playsLeft = 0
    private var looping = false

    override fun play(request: PlaybackRequest, onFinished: () -> Unit) {
        onMain { playOnMain(request, onFinished) }
    }

    override fun stop() {
        onMain { stopOnMain(notify = true) }
    }

    fun release() {
        stop()
    }

    private fun playOnMain(request: PlaybackRequest, onFinished: () -> Unit) {
        stopOnMain(notify = false)
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
            val volume = request.volumePercent.coerceIn(0, 100) / 100f
            created.setVolume(volume, volume)
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
                    stopOnMain(notify = true)
                    onFinished()
                }
                true
            }
            created.prepare()
            created.start()
            requestFocus()
            onPlaybackState(true)
        } catch (error: Throwable) {
            logger.e("Audio playback error", error)
            stopOnMain(notify = true)
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
                stopOnMain(notify = true)
                onFinished()
            }
        } else {
            stopOnMain(notify = true)
            onFinished()
        }
    }

    private fun stopOnMain(notify: Boolean) {
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
}
