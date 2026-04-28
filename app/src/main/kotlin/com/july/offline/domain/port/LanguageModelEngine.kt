package com.july.offline.domain.port

import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.ModelInfo

/**
 * Contrato del motor de modelo de lenguaje.
 * Sin dependencias Android. Implementado en capa ai/.
 */
interface LanguageModelEngine {

    /**
     * Genera una respuesta dado un prompt y el historial de conversación.
     * @param prompt texto del turno actual del usuario (ya transcrito)
     * @param history mensajes previos de la sesión para contexto
     * @return LlmResponse con texto generado o AppError clasificado
     */
    suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse>

    /**
     * Verifica si el motor LLM está disponible.
     * No lanza excepciones.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Devuelve metadatos del modelo activo.
     * Puede devolver ModelInfo con valores vacíos si el motor no responde.
     */
    suspend fun getModelInfo(): ModelInfo
}
