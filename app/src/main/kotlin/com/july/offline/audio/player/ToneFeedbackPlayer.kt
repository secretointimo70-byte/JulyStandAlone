package com.july.offline.audio.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class ToneFeedbackPlayer @Inject constructor() {

    private val sampleRate = 22_050

    fun playStartTone() {
        // Ascending two-note ping: 880 Hz then 1100 Hz (80ms each)
        playPcm(generateTone(880.0, 80) + generateTone(1100.0, 80))
    }

    fun playStopTone() {
        // Single lower ping: 660 Hz (120ms)
        playPcm(generateTone(660.0, 120))
    }

    private fun generateTone(frequencyHz: Double, durationMs: Int): ByteArray {
        val numSamples = sampleRate * durationMs / 1000
        val result = ByteArray(numSamples * 2)
        val amplitude = Short.MAX_VALUE * 0.4

        for (i in 0 until numSamples) {
            val fade = when {
                i < numSamples * 0.15 -> i / (numSamples * 0.15)
                i > numSamples * 0.85 -> (numSamples - i) / (numSamples * 0.15)
                else -> 1.0
            }
            val sample = (amplitude * fade * sin(2.0 * PI * frequencyHz * i / sampleRate)).toInt()
            result[i * 2]     = (sample and 0xFF).toByte()
            result[i * 2 + 1] = (sample shr 8).toByte()
        }
        return result
    }

    private fun playPcm(pcm: ByteArray) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        // MODE_STATIC on some devices never transitions playState to STOPPED after playback.
        // Sleep for the exact audio duration instead of polling playState.
        val durationMs = pcm.size.toLong() * 1000L / (sampleRate * 2)
        Thread.sleep(durationMs + 30)
        track.stop()
        track.release()
    }
}
