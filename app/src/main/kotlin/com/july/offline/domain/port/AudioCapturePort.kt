package com.july.offline.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de captura de audio del micrófono.
 * Implementado en capa audio/. Consume AudioRecord del sistema.
 */
interface AudioCapturePort {

    /**
     * Inicia la grabación y emite chunks de audio PCM en tiempo real.
     * El Flow completa cuando se llama stopRecording() o cancel().
     * @return Flow de chunks ByteArray PCM 16-bit
     */
    fun startRecording(): Flow<ByteArray>

    /**
     * Detiene la grabación y devuelve el audio completo concatenado.
     * Idempotente: llamadas adicionales devuelven ByteArray vacío.
     */
    suspend fun stopRecording(): ByteArray

    /** Cancela la grabación sin devolver audio. Libera recursos inmediatamente. */
    fun cancel()

    /** true si hay grabación activa en este momento. */
    val isRecording: Boolean
}
