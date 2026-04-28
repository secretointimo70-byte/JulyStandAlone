package com.july.offline.ai.tts

/**
 * Bridge JNI hacia libpiper.so + libonnxruntime.so (Piper TTS).
 *
 * Convención de nombres JNI:
 * Java_com_july_offline_ai_tts_PiperJNI_piperInit
 * Java_com_july_offline_ai_tts_PiperJNI_piperSynthesize
 * Java_com_july_offline_ai_tts_PiperJNI_piperFree
 *
 * ONNX Runtime debe cargarse antes que Piper.
 */
object PiperJNI {

    init {
        try {
            System.loadLibrary("onnxruntime")  // debe cargarse primero
            System.loadLibrary("piper")
        } catch (e: Throwable) {
            android.util.Log.e("PiperJNI", "Failed to load piper libraries", e)
        }
    }

    /**
     * Inicializa el contexto de Piper.
     * @param modelPath ruta absoluta al archivo .onnx del modelo de voz
     * @param modelConfigPath ruta al archivo .onnx.json de configuración
     * @return handle nativo al contexto Piper, o 0L si falla
     */
    external fun piperInit(modelPath: String, modelConfigPath: String): Long

    /**
     * Sintetiza texto a audio PCM 16-bit.
     * @param contextHandle handle devuelto por piperInit()
     * @param text texto a sintetizar
     * @param speakerId ID del hablante para modelos multi-speaker (0 para mono)
     * @return ShortArray con muestras PCM 16-bit al sampleRate del modelo (22050 Hz)
     */
    external fun piperSynthesize(
        contextHandle: Long,
        text: String,
        speakerId: Int
    ): ShortArray

    /**
     * Libera el contexto nativo de Piper.
     */
    external fun piperFree(contextHandle: Long)
}
