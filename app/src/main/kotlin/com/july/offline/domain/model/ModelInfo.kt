package com.july.offline.domain.model

/**
 * Metadatos del modelo LLM activo.
 */
data class ModelInfo(
    val name: String,
    val version: String = "",
    val parameterCount: String = "",
    val contextLength: Int = 0
)
