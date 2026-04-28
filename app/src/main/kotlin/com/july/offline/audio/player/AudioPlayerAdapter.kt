package com.july.offline.audio.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.july.offline.core.logging.DiagnosticsLogger
import javax.inject.Inject

class AudioPlayerAdapter @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    companion object {
        private const val SAMPLE_RATE = 22_050
    }

    @Volatile private var audioTrack: AudioTrack? = null

    fun play(audio: ByteArray) {
        if (audio.isEmpty()) {
            logger.logWarning("AudioPlayer", "Empty audio, skipping playback")
            return
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, audio.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        track.write(audio, 0, audio.size)
        track.play()
        logger.logInfo("AudioPlayer", "Playing ${audio.size} bytes")

        while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            Thread.sleep(50)
        }

        track.release()
        audioTrack = null
    }

    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
