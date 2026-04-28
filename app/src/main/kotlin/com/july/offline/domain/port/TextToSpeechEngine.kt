package com.july.offline.domain.port

import com.july.offline.core.result.JulyResult

/**
 * Contrato del motor de síntesis de voz.
 * Sin dependencias Android. Implementado en capa ai/.
 */
interface TextToSpeechEngine {

    /**
     * Sintetiza texto a audio PCM.
     * @param text texto a sintetizar
     * @return ByteArray con audio PCM 16-bit listo para AudioPlayerAdapter, o AppError
     */
    suspend fun synthesize(text: String): JulyResult<ByteArray>

    /**
     * Verifica si el motor TTS está disponible (binario Piper cargado, modelo presente).
     * No lanza excepciones.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Códigos de idioma soportados por el modelo TTS cargado (ej: ["es", "en"]).
     */
    fun getSupportedLanguages(): List<String>
}
