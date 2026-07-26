package com.tamapoke.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Android equivalent of audio.cpp's non-blocking SFX queue + task: a single
 * background thread pulls queued [Sfx] ids and writes their synthesized PCM
 * to a persistent [AudioTrack] via blocking writes, exactly like the
 * original's blocking i2s.write() calls - never touches the caller's thread.
 */
class SfxPlayer {
    private val mode = AtomicReference(SoundMode.FULL)
    private val queue = LinkedBlockingQueue<Sfx>(8)

    private val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(ChiptuneSynth.SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(
            maxOf(
                AudioTrack.getMinBufferSize(
                    ChiptuneSynth.SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ),
                4096,
            ),
        )
        .build()

    init {
        track.play()
        thread(name = "tamapoke-sfx", isDaemon = true) { workerLoop() }
    }

    fun play(sfx: Sfx) {
        if (mode.get() != SoundMode.OFF) queue.offer(sfx)
    }

    fun setMode(newMode: SoundMode) {
        mode.set(newMode)
        track.setVolume(newMode.gain)
    }

    private fun workerLoop() {
        while (true) {
            val sfx = queue.take()
            if (mode.get() == SoundMode.OFF) continue
            val pcm = ChiptuneSynth.renderSequence(SfxTable[sfx])
            track.write(pcm, 0, pcm.size)
        }
    }
}
