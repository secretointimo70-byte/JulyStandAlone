package com.july.offline.audio.recorder

import android.media.AudioFormat
import android.media.MediaRecorder

data class AudioRecorderConfig(
    val sampleRate: Int = 16_000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    val audioSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION,
    val chunkSizeMs: Int = 100
)
