package com.july.offline.ai.stt

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.OomHandler
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.port.SpeechToTextEngine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Adaptador STT con integración JNI real hacia Whisper.cpp.
 *
 * FASE 8: Integra OomHandler para captura de OutOfMemoryError durante JNI.
 * FASE 5: Integra ModelMemoryManager para gestión de ciclo de vida del modelo.
 *
 * Conversión de audio:
 * - Input: PCM 16-bit signed (ByteArray) a 16kHz mono
 * - Whisper requiere: FloatArray de muestras normalizadas [-1.0, 1.0]
 */
class WhisperSTTAdapter @Inject constructor(
    private val config: WhisperConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val oomHandler: OomHandler,
    private val logger: DiagnosticsLogger
) : SpeechToTextEngine {

    @Volatile private var contextHandle: Long = 0L
    private val initLock = Any()

    init {
        modelMemoryManager.registerSttReleaseCallback { releaseContext() }
    }

    fun ensureLoaded(): Boolean = initContext()

    fun releaseContext() {
        synchronized(initLock) {
            if (contextHandle != 0L) {
                WhisperJNI.whisperFree(contextHandle)
                contextHandle = 0L
                logger.logInfo("WhisperSTT", "Context released")
            }
        }
    }

    private fun initContext(): Boolean {
        if (contextHandle != 0L) return true
        synchronized(initLock) {
            if (contextHandle != 0L) return true
            if (!File(config.modelPath).exists()) {
                logger.logError("WhisperSTT", "Model not found: ${config.modelPath}")
                return false
            }
            logger.logInfo("WhisperSTT", "Loading model from ${config.modelPath}...")
            val handle = WhisperJNI.whisperInit(config.modelPath, config.threads)
            return if (handle != 0L) {
                contextHandle = handle
                logger.logInfo("WhisperSTT", "Model loaded successfully")
                true
            } else {
                logger.logError("WhisperSTT", "whisperInit() returned 0 — model load failed")
                false
            }
        }
    }

    override suspend fun transcribe(audio: ByteArray): JulyResult<Transcript> {
        return try {
            withTimeout(config.maxDurationMs) {
                if (!initContext()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Stt("Whisper context not initialized — model missing at ${config.modelPath}")
                    )
                }

                logger.logInfo("WhisperSTT", "Transcribing ${audio.size} bytes (${audio.size / 32}ms)")
                val startMs = System.currentTimeMillis()

                val floatSamples = convertPcm16ToFloat(audio)

                val rawText = oomHandler.safeJniCall(
                    engine = "WhisperSTT",
                    onOom = { "" }
                ) {
                    WhisperJNI.whisperTranscribe(
                        contextHandle = contextHandle,
                        pcmSamples = floatSamples,
                        language = config.language
                    )
                }

                val latencyMs = System.currentTimeMillis() - startMs
                val cleanText = filterHallucinations(rawText)
                logger.logEngineEvent("STT", "transcribed '${cleanText.take(40)}'", latencyMs)

                if (cleanText.isBlank()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Stt("No speech detected")
                    )
                }

                JulyResult.success(
                    Transcript(
                        text = cleanText,
                        confidence = -1f,
                        languageCode = config.language,
                        durationMs = audio.size.toLong() / 32L
                    )
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            JulyResult.failure(AppError.Stt("Whisper timeout after ${config.maxDurationMs}ms", e))
        } catch (e: Exception) {
            logger.logError("WhisperSTT", "Transcription failed", e)
            JulyResult.failure(AppError.Stt("Whisper error: ${e.message}", e))
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        return initContext()
    }

    private fun filterHallucinations(raw: String): String {
        val stripped = raw
            .replace(Regex("\\[[^\\]]{1,40}\\]"), "")   // [BLANK_AUDIO] [Music] etc.
            .replace(Regex("\\([^)]{1,40}\\)"), "")      // (music) (noise) etc.
            .replace(Regex("\\*[^*]{1,40}\\*"), "")      // *music* *applause*
            .trim()

        if (stripped.length < 2) return ""

        // Descarta texto genérico que Whisper emite en silencio/ruido
        val lower = stripped.lowercase()
        val genericNoise = listOf(
            "gracias por ver", "suscríbete", "subtítulos por", "subtitles by",
            "thank you for watching", "thanks for watching",
            "asistente de voz", "el usuario habla en español", " you"
        )
        if (genericNoise.any { lower.startsWith(it) }) return ""

        // Detecta repetición de palabras (síntoma de alucinación)
        val words = stripped.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size >= 4) {
            val maxRepeat = words.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            if (maxRepeat.toFloat() / words.size > 0.55f) return ""
        }

        return stripped
    }

    /**
     * Convierte PCM 16-bit signed little-endian a FloatArray normalizado [-1.0, 1.0].
     */
    private fun convertPcm16ToFloat(pcm: ByteArray): FloatArray {
        val sampleCount = pcm.size / 2
        val floats = FloatArray(sampleCount)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            floats[i] = buffer.short / 32768.0f
        }
        return floats
    }
}
