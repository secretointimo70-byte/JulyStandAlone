package com.july.offline.ai.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.port.TextToSpeechEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Implementación de TextToSpeechEngine usando Android TTS (sin .so, sin modelos).
 *
 * Usa el motor TTS del sistema (Google TTS u otro instalado en el dispositivo).
 * Funciona offline si el paquete de voz en español está descargado.
 *
 * Para garantizar voz offline en español:
 *   Ajustes → Accesibilidad → Texto a voz → Google TTS → Descargar idioma: Español
 *
 * Nota sobre el pipeline de audio:
 *   Android TTS reproduce directamente por el speaker del sistema vía speak().
 *   synthesize() devuelve ByteArray vacío; AudioPlayerAdapter lo omite (ya maneja
 *   arrays vacíos). El contrato suspend se mantiene: la función no retorna hasta
 *   que el habla termina, por lo que el orquestador puede volver a escuchar
 *   inmediatamente después.
 *
 * Para cambiar a Piper cuando estén disponibles los .so:
 *   Cambiar el @Binds en EngineModule de AndroidTtsAdapter a PiperTTSAdapter.
 */
class AndroidTtsAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) : TextToSpeechEngine {

    companion object {
        private const val TAG = "AndroidTts"
        private const val UTTERANCE_ID = "july_utterance"
        private const val INIT_TIMEOUT_MS = 5_000L
    }

    private var tts: TextToSpeech? = null

    /**
     * Se completa con true/false cuando Android TTS termina de inicializarse.
     * La inicialización es asíncrona — no bloquea el hilo de Hilt.
     */
    private val initResult = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                val locale = Locale("es", "ES")
                val langResult = tts?.setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Fallback a es genérico
                    tts?.setLanguage(Locale("es"))
                    logger.logWarning(TAG, "es_ES no disponible, usando es genérico")
                }
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
                logger.logInfo(TAG, "Android TTS inicializado")
            } else {
                logger.logError(TAG, "Android TTS falló al inicializar (status=$status)", null)
            }
            initResult.complete(ok)
        }
    }

    override suspend fun synthesize(text: String): JulyResult<ByteArray> {
        val ready = withTimeoutOrNull(INIT_TIMEOUT_MS) { initResult.await() } ?: false
        if (!ready) {
            return JulyResult.failure(AppError.Tts("Android TTS no pudo inicializarse"))
        }
        if (text.isBlank()) {
            return JulyResult.success(ByteArray(0))
        }

        return suspendCancellableCoroutine { cont ->
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    logger.logInfo(TAG, "Hablando: \"${text.take(40)}${if (text.length > 40) "…" else ""}\"")
                }

                override fun onDone(utteranceId: String?) {
                    if (cont.isActive) cont.resume(JulyResult.success(ByteArray(0)))
                }

                @Deprecated("Deprecated in API 21")
                override fun onError(utteranceId: String?) {
                    if (cont.isActive) cont.resume(
                        JulyResult.failure(AppError.Tts("Error durante síntesis TTS"))
                    )
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (cont.isActive) cont.resume(
                        JulyResult.failure(AppError.Tts("Error TTS código $errorCode"))
                    )
                }
            })

            val params = Bundle()
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)

            if (result != TextToSpeech.SUCCESS) {
                if (cont.isActive) cont.resume(
                    JulyResult.failure(AppError.Tts("TextToSpeech.speak() falló"))
                )
            }

            cont.invokeOnCancellation {
                tts?.stop()
            }
        }
    }

    override suspend fun isAvailable(): Boolean {
        val ready = withTimeoutOrNull(INIT_TIMEOUT_MS) { initResult.await() } ?: false
        return ready
    }

    override fun getSupportedLanguages(): List<String> = listOf("es", "es_ES")
}
