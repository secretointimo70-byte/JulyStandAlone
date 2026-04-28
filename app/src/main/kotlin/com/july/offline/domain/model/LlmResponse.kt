package com.july.offline.domain.model

/**
 * Respuesta del motor LLM.
 * @param text texto generado por el modelo
 * @param tokenCount número de tokens generados (-1 si no disponible)
 * @param latencyMs tiempo total de generación en milisegundos
 * @param modelName nombre del modelo que generó la respuesta
 */
data class LlmResponse(
    val text: String,
    val tokenCount: Int = -1,
    val latencyMs: Long = 0L,
    val modelName: String = ""
) {
    val isEmpty: Boolean get() = text.isBlank()
}
