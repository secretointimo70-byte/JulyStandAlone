package com.july.offline.domain.orchestrator

import com.july.offline.audio.player.AudioPlayerAdapter
import com.july.offline.audio.player.ToneFeedbackPlayer
import com.july.offline.audio.vad.VADProcessor
import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.AudioCapturePort
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCoordinator @Inject constructor(
    private val audioCapturePort: AudioCapturePort,
    private val audioPlayerAdapter: AudioPlayerAdapter,
    private val toneFeedbackPlayer: ToneFeedbackPlayer,
    private val vadProcessor: VADProcessor,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val _vadIsSilence = MutableStateFlow(false)
    val vadIsSilence: StateFlow<Boolean> = _vadIsSilence.asStateFlow()

    private val _vadEnergy = MutableStateFlow(0.0)
    val vadEnergy: StateFlow<Double> = _vadEnergy.asStateFlow()

    /**
     * Graba hasta detectar silencio via VAD.
     * El timeout duro funciona llamando cancel() en un job separado, lo que detiene
     * AudioRecord.read() desde otro hilo (withTimeoutOrNull no puede interrumpir
     * llamadas nativas bloqueantes).
     */
    suspend fun recordUntilSilence(): Pair<ByteArray, Long>? =
        withContext(dispatchers.io) {
            val startTime = System.currentTimeMillis()
            val audioBuffer = mutableListOf<Byte>()
            var detectedVoice = false
            var timeoutJob: Job? = null

            vadProcessor.reset()
            _vadIsSilence.value = false
            _vadEnergy.value = 0.0
            toneFeedbackPlayer.playStartTone()

            try {
                // Job separado: llama cancel() para desbloquear AudioRecord.read()
                timeoutJob = launch {
                    delay(vadProcessor.maxRecordingMs)
                    logger.logInfo("AudioCoordinator", "Timeout ${vadProcessor.maxRecordingMs}ms — stopping recording")
                    audioCapturePort.cancel()
                }

                audioCapturePort
                    .startRecording()
                    .takeWhile { chunk ->
                        audioBuffer.addAll(chunk.toList())
                        val silence = vadProcessor.isSilence(chunk)
                        if (vadProcessor.isVoiceActive()) detectedVoice = true
                        _vadIsSilence.value = silence
                        _vadEnergy.value = vadProcessor.lastEnergyLevel
                        !(silence && detectedVoice)
                    }
                    .collect {}

            } finally {
                timeoutJob?.cancel()
                audioCapturePort.stopRecording()
                _vadIsSilence.value = false
                _vadEnergy.value = 0.0
                toneFeedbackPlayer.playStopTone()
            }

            val audioBytes = audioBuffer.toByteArray()
            val durationMs = System.currentTimeMillis() - startTime

            if (audioBytes.size < 3200) {
                logger.logWarning("AudioCoordinator", "Audio too short: ${audioBytes.size} bytes (${durationMs}ms)")
                return@withContext null
            }

            logger.logInfo("AudioCoordinator", "Captured ${audioBytes.size} bytes in ${durationMs}ms (voice: $detectedVoice)")
            Pair(audioBytes, durationMs)
        }

    suspend fun playAudio(audio: ByteArray) = withContext(dispatchers.io) {
        if (audio.isEmpty()) return@withContext
        audioPlayerAdapter.play(audio)
    }

    fun cancel() {
        audioCapturePort.cancel()
        audioPlayerAdapter.stop()
        vadProcessor.reset()
        _vadIsSilence.value = false
        _vadEnergy.value = 0.0
        logger.logInfo("AudioCoordinator", "Cancelled")
    }
}
