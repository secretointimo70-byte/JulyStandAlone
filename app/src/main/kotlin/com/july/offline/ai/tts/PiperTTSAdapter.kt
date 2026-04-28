package com.july.offline.ai.tts

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.OomHandler
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.port.TextToSpeechEngine
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Adaptador TTS con integración JNI real hacia Piper + ONNX Runtime.
 *
 * FASE 8: Integra OomHandler para captura de OutOfMemoryError durante JNI.
 * FASE 5: Integra ModelMemoryManager para gestión de ciclo de vida del modelo.
 *
 * Conversión de audio:
 * - Piper devuelve: ShortArray PCM 16-bit a 22050 Hz mono
 * - AudioPlayerAdapter espera: ByteArray PCM 16-bit little-endian
 */
class PiperTTSAdapter @Inject constructor(
    private val config: PiperConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val oomHandler: OomHandler,
    private val logger: DiagnosticsLogger
) : TextToSpeechEngine {

    @Volatile private var contextHandle: Long = 0L
    private val initLock = Any()

    init {
        modelMemoryManager.registerTtsReleaseCallback { releaseContext() }
    }

    fun ensureLoaded(): Boolean = initContext()

    fun releaseContext() {
        synchronized(initLock) {
            if (contextHandle != 0L) {
                PiperJNI.piperFree(contextHandle)
                contextHandle = 0L
                logger.logInfo("PiperTTS", "Context released")
            }
        }
    }

    private fun initContext(): Boolean {
        if (contextHandle != 0L) return true
        synchronized(initLock) {
            if (contextHandle != 0L) return true
            if (!File(config.modelPath).exists()) {
                logger.logError("PiperTTS", "Model not found: ${config.modelPath}")
                return false
            }
            if (!File(config.modelConfigPath).exists()) {
                logger.logError("PiperTTS", "Config not found: ${config.modelConfigPath}")
                return false
            }
            logger.logInfo("PiperTTS", "Loading model from ${config.modelPath}...")
            val handle = PiperJNI.piperInit(config.modelPath, config.modelConfigPath)
            return if (handle != 0L) {
                contextHandle = handle
                logger.logInfo("PiperTTS", "Piper model loaded successfully")
                true
            } else {
                logger.logError("PiperTTS", "piperInit() returned 0 — model load failed")
                false
            }
        }
    }

    override suspend fun synthesize(text: String): JulyResult<ByteArray> {
        return try {
            if (!initContext()) {
                return JulyResult.failure(
                    AppError.Tts("Piper context not initialized — model missing at ${config.modelPath}")
                )
            }

            if (text.isBlank()) {
                return JulyResult.success(ByteArray(0))
            }

            logger.logInfo("PiperTTS", "Synthesizing: '${text.take(60)}...'")
            val startMs = System.currentTimeMillis()

            val shorts = oomHandler.safeJniCall(
                engine = "PiperTTS",
                onOom = { ShortArray(0) }
            ) {
                PiperJNI.piperSynthesize(
                    contextHandle = contextHandle,
                    text = text,
                    speakerId = config.speakerId
                )
            }

            val latencyMs = System.currentTimeMillis() - startMs
            logger.logEngineEvent("TTS", "synthesized ${shorts.size} samples", latencyMs)

            if (shorts.isEmpty()) {
                return JulyResult.failure(
                    AppError.Tts("Piper returned empty audio for input: '${text.take(40)}'")
                )
            }

            JulyResult.success(convertShortsToPcm(shorts))

        } catch (e: Exception) {
            logger.logError("PiperTTS", "Synthesis failed", e)
            JulyResult.failure(AppError.Tts("Piper error: ${e.message}", e))
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        if (!File(config.modelConfigPath).exists()) return false
        return initContext()
    }

    override fun getSupportedLanguages(): List<String> = listOf("es")

    /**
     * Convierte ShortArray PCM 16-bit a ByteArray little-endian
     * para que AudioPlayerAdapter pueda reproducirlo con AudioTrack.
     */
    private fun convertShortsToPcm(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { buffer.putShort(it) }
        return bytes
    }
}
