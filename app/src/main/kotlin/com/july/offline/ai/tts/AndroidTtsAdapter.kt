package com.july.offline.ai.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.domain.port.TextToSpeechEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class TtsVoiceOption(
    val name: String,
    val displayName: String,
    val quality: Int
)

@Singleton
class AndroidTtsAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val logger: DiagnosticsLogger
) : TextToSpeechEngine {

    companion object {
        private const val TAG = "AndroidTts"
        private const val UTTERANCE_ID = "july_utterance"
        private const val INIT_TIMEOUT_MS = 5_000L
    }

    private val adapterScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tts: TextToSpeech? = null
    private val initResult = CompletableDeferred<Boolean>()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                val locale = Locale("es", "ES")
                val langResult = tts?.setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.setLanguage(Locale("es"))
                    logger.logWarning(TAG, "es_ES no disponible, usando es genérico")
                }
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.95f)
                logger.logInfo(TAG, "Android TTS inicializado")

                adapterScope.launch {
                    populateVoices()
                    val savedVoice = preferencesDataStore.ttsVoiceName.first()
                    if (savedVoice.isNotBlank()) applyVoice(savedVoice)
                }
            } else {
                logger.logError(TAG, "Android TTS falló al inicializar (status=$status)", null)
            }
            initResult.complete(ok)
        }
    }

    private fun populateVoices() {
        val voices = tts?.voices
            ?.filter { voice ->
                voice.locale.language == "es" && !voice.isNetworkConnectionRequired
            }
            ?.map { voice -> TtsVoiceOption(voice.name, formatVoiceName(voice), voice.quality) }
            ?.sortedByDescending { it.quality }
            ?: emptyList()
        _availableVoices.value = voices
    }

    fun applyVoice(voiceName: String) {
        val voice = tts?.voices?.find { it.name == voiceName }
        if (voice != null) {
            tts?.voice = voice
            logger.logInfo(TAG, "Voz aplicada: $voiceName")
        }
    }

    private fun formatVoiceName(voice: Voice): String {
        val id = voice.name
            .removePrefix("es-es-x-")
            .removePrefix("es_ES-")
            .removeSuffix("-local")
            .removeSuffix("-embedded")
            .removeSuffix("-network")
        val stars = when {
            voice.quality >= Voice.QUALITY_VERY_HIGH -> "★★★"
            voice.quality >= Voice.QUALITY_HIGH -> "★★"
            else -> "★"
        }
        return "$id  $stars"
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
            val ssml = toSsml(text)
            val result = tts?.speak(ssml, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)

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

    private fun toSsml(text: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val withPauses = escaped
            .replace(Regex("([.!?])\\s+"), "$1<break time=\"400ms\"/> ")
            .replace(Regex(",\\s+"), ",<break time=\"150ms\"/> ")
        return "<speak>$withPauses</speak>"
    }
}
