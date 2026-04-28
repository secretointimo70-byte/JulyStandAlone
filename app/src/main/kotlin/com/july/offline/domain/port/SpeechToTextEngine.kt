package com.july.offline.domain.port

import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript

/**
 * Contrato del motor de reconocimiento de voz.
 * Sin dependencias Android. Implementado en capa ai/.
 */
interface SpeechToTextEngine {

    /**
     * Transcribe audio PCM 16-bit a texto.
     * @param audio bytes de audio en formato PCM 16-bit, mono, 16kHz
     * @return Transcript con texto reconocido o AppError clasificado
     */
    suspend fun transcribe(audio: ByteArray): JulyResult<Transcript>

    /**
     * Verifica si el motor está disponible (binario cargado, modelo en memoria).
     * Llamado por EngineHealthMonitor. No lanza excepciones.
     */
    suspend fun isAvailable(): Boolean
}
