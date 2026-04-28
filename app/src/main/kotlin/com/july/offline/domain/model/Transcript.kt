package com.july.offline.domain.model

/**
 * Resultado del motor STT.
 * @param text texto reconocido
 * @param confidence valor entre 0.0 y 1.0 (-1.0 si el motor no lo provee)
 * @param languageCode código ISO 639-1 del idioma detectado (ej: "es", "en")
 * @param durationMs duración del audio procesado en milisegundos
 */
data class Transcript(
    val text: String,
    val confidence: Float = -1f,
    val languageCode: String = "es",
    val durationMs: Long = 0L
) {
    val isEmpty: Boolean get() = text.isBlank()
}
