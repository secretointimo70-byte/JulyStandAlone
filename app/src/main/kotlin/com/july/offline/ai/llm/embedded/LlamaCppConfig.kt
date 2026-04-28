package com.july.offline.ai.llm.embedded

/**
 * Configuración del motor llama.cpp embebido.
 *
 * @param modelPath ruta absoluta al archivo .gguf en el dispositivo
 * @param contextSize ventana de contexto en tokens (4096 recomendado para 3B)
 * @param threads hilos de CPU para inferencia (recomendado: nCores - 1)
 * @param gpuLayers capas a offloadear a GPU (0 = solo CPU, recomendado en Android)
 * @param maxTokens máximo de tokens a generar por respuesta
 * @param temperature temperatura de muestreo [0.0, 2.0]
 * @param topP nucleus sampling (0.9 recomendado)
 * @param repeatPenalty penalización por repetición (1.1 recomendado)
 * @param systemPrompt prompt de sistema enviado en cada sesión
 * @param inferenceTimeoutMs timeout de inferencia en milisegundos
 */
data class LlamaCppConfig(
    val modelPath: String,
    val contextSize: Int = 1024,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val repeatPenalty: Float = 1.1f,
    val systemPrompt: String =
        "Eres July, un asistente de voz offline. " +
        "Responde de forma concisa y natural. " +
        "Máximo 3 oraciones por respuesta.",
    val inferenceTimeoutMs: Long = 120_000L
) {
    init {
        require(contextSize in 512..32768) { "contextSize must be in [512, 32768]" }
        require(threads in 1..16) { "threads must be in [1, 16]" }
        require(maxTokens in 1..4096) { "maxTokens must be in [1, 4096]" }
        require(temperature in 0f..2f) { "temperature must be in [0.0, 2.0]" }
    }
}
