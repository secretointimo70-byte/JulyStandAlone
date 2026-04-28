package com.july.offline.ai.tts

/**
 * Configuración del motor Piper TTS.
 * @param modelPath ruta al archivo .onnx del modelo de voz
 * @param modelConfigPath ruta al archivo .json de configuración del modelo
 * @param sampleRate frecuencia de muestreo de salida (por defecto 22050 Hz)
 * @param speakerId ID del speaker para modelos multi-hablante (0 para modelos mono)
 */
data class PiperConfig(
    val modelPath: String,
    val modelConfigPath: String,
    val sampleRate: Int = 22050,
    val speakerId: Int = 0
)
