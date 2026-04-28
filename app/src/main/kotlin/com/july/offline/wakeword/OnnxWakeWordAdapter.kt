package com.july.offline.wakeword

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.domain.port.WakeWordEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.nio.FloatBuffer
import javax.inject.Inject

/**
 * Motor de detección de wake-word usando openWakeWord + ONNX Runtime.
 *
 * Pipeline de detección (100% offline, sin API key):
 *
 *   AudioRecord (16kHz mono) ──► chunks de 1280 muestras (80ms)
 *        │
 *        ▼
 *   [embeddingSession]   wakeword_embedding.onnx
 *   input  [1, 1280] float  → normalizado a [-1, 1]
 *   output [1,   96] float  → embedding de audio
 *        │
 *        ▼
 *   embeddingBuffer (circular, [contextFrames × 96])
 *        │
 *        ▼
 *   [wakeWordSession]    wakeword_model.onnx
 *   input  [1, contextFrames, 96] float
 *   output [1] float  → probabilidad de detección
 *        │
 *        ▼ si prob > threshold
 *   emit WakeWordEvent.Detected
 *
 * IMPORTANTE: Este adaptador gestiona su propio AudioRecord.
 * El WakeWordCoordinator garantiza que se llama a stop() ANTES
 * de que AudioRecorderAdapter inicie grabación.
 *
 * Modelos requeridos en app/src/main/assets/:
 *   - wakeword_embedding.onnx  (~3 MB)
 *   - wakeword_model.onnx      (~50 KB)
 * Ver OnnxWakeWordConfig para instrucciones de descarga/entrenamiento.
 */
class OnnxWakeWordAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: OnnxWakeWordConfig,
    private val logger: DiagnosticsLogger
) : WakeWordEngine {

    companion object {
        private const val TAG = "OnnxWakeWord"
        private const val SAMPLE_RATE = 16_000
        /** Tamaño de chunk = 80ms a 16kHz, requerido por el modelo de embeddings. */
        private const val CHUNK_SAMPLES = 1280
        /** Dimensión del vector de embedding de salida. */
        private const val EMBEDDING_DIM = 96
    }

    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    @Volatile private var embeddingSession: OrtSession? = null
    @Volatile private var wakeWordSession: OrtSession? = null
    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var _isListening = false

    override val isListening: Boolean get() = _isListening

    override fun startListening(): Flow<WakeWordEvent> = flow {
        val (embSession, wwSession) = loadSessions() ?: run {
            emit(WakeWordEvent.Error(IllegalStateException(
                "No se pudieron cargar los modelos ONNX. " +
                "Verifica que existen en assets/: " +
                "${config.embeddingModelAsset}, ${config.wakeWordModelAsset}"
            )))
            return@flow
        }
        embeddingSession = embSession
        wakeWordSession = wwSession

        val recorder = buildAudioRecord() ?: run {
            embSession.close()
            wwSession.close()
            emit(WakeWordEvent.Error(IllegalStateException("No se pudo abrir AudioRecord")))
            return@flow
        }
        audioRecord = recorder
        recorder.startRecording()
        _isListening = true

        // Buffer circular de embeddings
        val embeddingBuffer = ArrayDeque<FloatArray>(config.contextFrames)

        val audioBuffer = ShortArray(CHUNK_SAMPLES)
        val floatBuffer = FloatArray(CHUNK_SAMPLES)

        logger.logInfo(TAG, "Escuchando wake-word (modelo=${config.wakeWordModelAsset}, threshold=${config.threshold})")

        try {
            while (currentCoroutineContext().isActive && _isListening) {
                val read = recorder.read(audioBuffer, 0, CHUNK_SAMPLES)
                if (read < CHUNK_SAMPLES) continue

                // Normalizar PCM 16-bit → float [-1, 1]
                for (i in 0 until CHUNK_SAMPLES) {
                    floatBuffer[i] = audioBuffer[i] / 32768f
                }

                // Paso 1: embedding de audio
                val embedding = runEmbedding(embSession, floatBuffer) ?: continue

                // Mantener buffer circular de contextFrames embeddings
                if (embeddingBuffer.size >= config.contextFrames) {
                    embeddingBuffer.removeFirst()
                }
                embeddingBuffer.addLast(embedding)

                // Paso 2: clasificación — solo cuando tenemos contexto completo
                if (embeddingBuffer.size < config.contextFrames) continue

                val probability = runWakeWord(wwSession, embeddingBuffer) ?: continue

                if (probability > config.threshold) {
                    logger.logInfo(TAG, "Wake-word detectado (prob=%.3f)".format(probability))
                    emit(WakeWordEvent.Detected(keywordIndex = 0))
                    break  // El coordinador decide si reanudar la escucha
                }
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Error en el loop de detección", e)
            emit(WakeWordEvent.Error(e))
        } finally {
            cleanup()
        }
    }.flowOn(Dispatchers.IO)

    override fun stop() {
        _isListening = false
        cleanup()
        logger.logInfo(TAG, "Detenido")
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            context.assets.open(config.embeddingModelAsset).close()
            context.assets.open(config.wakeWordModelAsset).close()
            true
        } catch (e: Exception) {
            logger.logWarning(TAG, "Modelos ONNX no disponibles en assets: ${e.message}")
            false
        }
    }

    // ── Carga de sesiones ONNX ─────────────────────────────────────────────

    private fun loadSessions(): Pair<OrtSession, OrtSession>? {
        return try {
            val embBytes = context.assets.open(config.embeddingModelAsset).readBytes()
            val wwBytes  = context.assets.open(config.wakeWordModelAsset).readBytes()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(1)   // wake-word corre en background; 1 hilo es suficiente
            }
            val embSession = ortEnv.createSession(embBytes, opts)
            val wwSession  = ortEnv.createSession(wwBytes,  opts)
            logger.logInfo(TAG, "Sesiones ONNX cargadas")
            embSession to wwSession
        } catch (e: Exception) {
            logger.logError(TAG, "Fallo al cargar sesiones ONNX", e)
            null
        }
    }

    // ── Inferencia ─────────────────────────────────────────────────────────

    /**
     * Ejecuta el modelo de embeddings sobre un chunk de audio.
     * Input:  [1, CHUNK_SAMPLES] float
     * Output: [EMBEDDING_DIM] float
     */
    private fun runEmbedding(session: OrtSession, audio: FloatArray): FloatArray? {
        return try {
            val tensor = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(audio),
                longArrayOf(1L, CHUNK_SAMPLES.toLong())
            )
            tensor.use {
                val result = session.run(mapOf(session.inputNames.first() to it))
                result.use {
                    @Suppress("UNCHECKED_CAST")
                    (result[0].value as Array<FloatArray>)[0]
                }
            }
        } catch (e: Exception) {
            logger.logWarning(TAG, "Error en embedding: ${e.message}")
            null
        }
    }

    /**
     * Ejecuta el clasificador de wake-word sobre el buffer de contexto.
     * Input:  [1, contextFrames, EMBEDDING_DIM] float
     * Output: probabilidad float en [0, 1]
     */
    private fun runWakeWord(session: OrtSession, buffer: ArrayDeque<FloatArray>): Float? {
        return try {
            val flat = FloatArray(config.contextFrames * EMBEDDING_DIM)
            buffer.forEachIndexed { i, emb ->
                emb.copyInto(flat, i * EMBEDDING_DIM)
            }
            val tensor = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(flat),
                longArrayOf(1L, config.contextFrames.toLong(), EMBEDDING_DIM.toLong())
            )
            tensor.use {
                val result = session.run(mapOf(session.inputNames.first() to it))
                result.use {
                    @Suppress("UNCHECKED_CAST")
                    (result[0].value as Array<FloatArray>)[0][0]
                }
            }
        } catch (e: Exception) {
            logger.logWarning(TAG, "Error en clasificación wake-word: ${e.message}")
            null
        }
    }

    // ── AudioRecord ────────────────────────────────────────────────────────

    private fun buildAudioRecord(): AudioRecord? {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuf, CHUNK_SAMPLES * 2 * 4)
        return try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            ).also { rec ->
                if (rec.state != AudioRecord.STATE_INITIALIZED) {
                    rec.release()
                    return null
                }
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Error al crear AudioRecord", e)
            null
        }
    }

    // ── Limpieza ───────────────────────────────────────────────────────────

    private fun cleanup() {
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null

        try { embeddingSession?.close() } catch (_: Exception) {}
        embeddingSession = null

        try { wakeWordSession?.close() } catch (_: Exception) {}
        wakeWordSession = null

        _isListening = false
    }
}
