package com.july.offline.ai.llm.embedded

/**
 * Bridge JNI hacia libllama.so + libggml.so (llama.cpp).
 *
 * ORDEN DE CARGA CRÍTICO:
 * libggml.so debe cargarse ANTES que libllama.so.
 */
object LlamaCppJNI {

    init {
        try {
            System.loadLibrary("ggml-base")
            System.loadLibrary("ggml-cpu")
            System.loadLibrary("ggml")
            System.loadLibrary("llama")
            System.loadLibrary("july")   // JNI bridge (no-op if WhisperJNI loaded first)
        } catch (e: Throwable) {
            android.util.Log.e("LlamaCppJNI", "Failed to load llama libraries", e)
        }
    }

    /**
     * Inicializa el contexto llama.cpp cargando el modelo GGUF en memoria.
     *
     * @param modelPath ruta absoluta al archivo .gguf
     * @param contextSize ventana de contexto en tokens
     * @param threads número de hilos de CPU
     * @param gpuLayers capas a offloadear a GPU (0 = solo CPU)
     * @return handle nativo (Long) al llama_context, o 0L si falla
     */
    external fun llamaInit(
        modelPath: String,
        contextSize: Int,
        threads: Int,
        gpuLayers: Int
    ): Long

    /**
     * Genera texto dado un prompt completo (incluyendo historial formateado).
     *
     * @param contextHandle handle devuelto por llamaInit()
     * @param prompt texto completo incluyendo system prompt e historial
     * @param maxTokens máximo de tokens a generar
     * @param temperature temperatura de muestreo
     * @param topP nucleus sampling
     * @param repeatPenalty penalización por repetición
     * @return texto generado, o cadena vacía si falla
     */
    external fun llamaGenerate(
        contextHandle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        repeatPenalty: Float
    ): String

    /**
     * Devuelve el número de tokens en un texto según el tokenizador del modelo.
     */
    external fun llamaTokenize(contextHandle: Long, text: String): Int

    /**
     * Libera el contexto llama.cpp y el modelo de memoria.
     */
    external fun llamaFree(contextHandle: Long)

    /**
     * Devuelve información del modelo cargado (nombre, parámetros, arquitectura).
     */
    external fun llamaModelInfo(contextHandle: Long): String
}
