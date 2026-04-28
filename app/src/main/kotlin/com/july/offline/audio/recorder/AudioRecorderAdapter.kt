package com.july.offline.audio.recorder

import android.media.AudioRecord
import android.media.AudioFormat
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.AudioCapturePort
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AudioRecorderAdapter @Inject constructor(
    private val config: AudioRecorderConfig,
    private val logger: DiagnosticsLogger
) : AudioCapturePort {

    @Volatile
    private var _isRecording = false
    override val isRecording: Boolean get() = _isRecording

    private val bufferSize: Int by lazy {
        val minBuffer = AudioRecord.getMinBufferSize(
            config.sampleRate,
            config.channelConfig,
            config.audioEncoding
        )
        val chunkBytes = config.sampleRate * 2 * config.chunkSizeMs / 1000
        maxOf(minBuffer, chunkBytes) * 4
    }

    private var audioRecord: AudioRecord? = null
    private val fullAudioBuffer = mutableListOf<Byte>()

    override fun startRecording(): Flow<ByteArray> = flow {
        val recorder = AudioRecord(
            config.audioSource,
            config.sampleRate,
            config.channelConfig,
            config.audioEncoding,
            bufferSize
        )

        audioRecord = recorder
        fullAudioBuffer.clear()
        recorder.startRecording()
        _isRecording = true
        logger.logInfo("AudioRecorder", "Recording started at ${config.sampleRate}Hz")

        val chunkBuffer = ByteArray(bufferSize / 4)

        try {
            while (_isRecording) {
                // READ_NON_BLOCKING returns immediately so _isRecording is checked frequently.
                // This makes cancel() / stop() work reliably on all devices including emulators
                // where blocking read() may never return after stop() is called cross-thread.
                val bytesRead = recorder.read(chunkBuffer, 0, chunkBuffer.size, AudioRecord.READ_NON_BLOCKING)
                if (bytesRead > 0) {
                    val chunk = chunkBuffer.copyOf(bytesRead)
                    fullAudioBuffer.addAll(chunk.toList())
                    emit(chunk)
                } else {
                    // No data yet — yield briefly so other coroutines can run
                    delay(10)
                }
            }
        } finally {
            _isRecording = false
            // cancel() may have already stopped/released; guard against double-release
            try { recorder.stop() } catch (_: Exception) {}
            try { recorder.release() } catch (_: Exception) {}
            audioRecord = null
            logger.logInfo("AudioRecorder", "Recording stopped. Total: ${fullAudioBuffer.size} bytes")
        }
    }

    override suspend fun stopRecording(): ByteArray {
        _isRecording = false
        return fullAudioBuffer.toByteArray()
    }

    override fun cancel() {
        _isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        fullAudioBuffer.clear()
    }
}
