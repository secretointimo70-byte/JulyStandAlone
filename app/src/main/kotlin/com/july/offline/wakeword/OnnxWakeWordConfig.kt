package com.july.offline.wakeword

/**
 * Configuración del motor de wake-word basado en openWakeWord + ONNX Runtime.
 *
 * El pipeline usa dos modelos ONNX que deben colocarse en app/src/main/assets/:
 *
 *   1. [embeddingModelAsset] — Modelo de embeddings de audio (Google MobileNet-based).
 *      Convierte chunks de 1280 muestras (80ms a 16kHz) en vectores de 96 dimensiones.
 *      Archivo: wakeword_embedding.onnx (~3 MB, compartido entre todos los wake words)
 *      Fuente: https://github.com/dscripka/openWakeWord/releases
 *
 *   2. [wakeWordModelAsset] — Clasificador de la frase clave específica.
 *      Recibe una secuencia de [contextFrames] embeddings y devuelve probabilidad [0,1].
 *      Archivo: wakeword_hey_july.onnx (~50 KB) — entrenar con openWakeWord
 *              o usar modelo preentrenado (hey_jarvis, alexa, etc.) como placeholder.
 *      Fuente: https://github.com/dscripka/openWakeWord#pre-trained-models
 *
 * Para entrenar un modelo "Oye July" personalizado:
 *   pip install openwakeword
 *   python train.py --phrase "oye julio" --output wakeword_hey_july.onnx
 *
 * @param embeddingModelAsset nombre del asset con el modelo de embeddings
 * @param wakeWordModelAsset nombre del asset con el modelo clasificador
 * @param threshold probabilidad mínima para considerar detección [0.0, 1.0]
 * @param contextFrames número de frames de embedding que el clasificador recibe como contexto (16 = ~1.3s)
 */
data class OnnxWakeWordConfig(
    val embeddingModelAsset: String = "wakeword_embedding.onnx",
    val wakeWordModelAsset: String = "wakeword_model.onnx",
    val threshold: Float = 0.5f,
    val contextFrames: Int = 16
) {
    init {
        require(threshold in 0f..1f) { "threshold must be in [0.0, 1.0]" }
        require(contextFrames > 0) { "contextFrames must be > 0" }
    }
}
