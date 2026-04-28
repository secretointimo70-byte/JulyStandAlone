package com.july.offline.ai.stt

/**
 * Configuración del motor Whisper.cpp.
 * @param modelPath ruta absoluta al archivo .bin del modelo (ej: /data/data/.../whisper-tiny.bin)
 * @param language código ISO 639-1 del idioma ("es", "en", "auto")
 * @param threads número de hilos para inferencia (recomendado: 4)
 * @param maxDurationMs duración máxima de audio a procesar antes de timeout
 */
data class WhisperConfig(
    val modelPath: String,
    val language: String = "es",
    val threads: Int = 4,
    val maxDurationMs: Long = 30_000L
)
