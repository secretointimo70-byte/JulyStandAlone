package com.july.offline.ai.stt

/**
 * Bridge JNI hacia libwhisper.so (Whisper.cpp).
 *
 * Convención de nombres JNI:
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperInit
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperTranscribe
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperFree
 */
object WhisperJNI {

    init {
        try {
            System.loadLibrary("ggml-base")
            System.loadLibrary("ggml-cpu")
            System.loadLibrary("ggml")
            System.loadLibrary("whisper")
            System.loadLibrary("july")
        } catch (e: Throwable) {
            android.util.Log.e("WhisperJNI", "Failed to load whisper libraries", e)
        }
    }

    /**
     * Inicializa el contexto de Whisper con el modelo en disco.
     * @param modelPath ruta absoluta al archivo .bin del modelo GGML
     * @param nThreads número de hilos de CPU para inferencia (recomendado: 4)
     * @return handle nativo (Long) al contexto whisper_context, o 0L si falla
     */
    external fun whisperInit(modelPath: String, nThreads: Int): Long

    /**
     * Transcribe audio PCM a texto.
     * @param contextHandle handle devuelto por whisperInit()
     * @param pcmSamples muestras de audio PCM 32-bit float, mono, 16kHz
     * @param language código de idioma ISO 639-1 ("es", "en", "auto")
     * @return texto transcrito, o cadena vacía si no se detectó voz
     */
    external fun whisperTranscribe(
        contextHandle: Long,
        pcmSamples: FloatArray,
        language: String
    ): String

    /**
     * Libera el contexto nativo de Whisper.
     */
    external fun whisperFree(contextHandle: Long)
}
