package com.july.offline.domain.model

import java.time.Instant

/** Estado de disponibilidad de un motor individual. */
enum class EngineStatus {
    /** Motor disponible y respondiendo correctamente. */
    READY,
    /** Motor no disponible (proceso caído, binario no encontrado, red inalcanzable). */
    UNAVAILABLE,
    /** Motor disponible pero con degradación (latencia alta, errores intermitentes). */
    DEGRADED,
    /** Estado no verificado aún (arranque de la app). */
    UNKNOWN
}

/**
 * Estado de salud de los tres motores del sistema.
 * Emitido por EngineHealthMonitor cada 30 segundos o ante fallo detectado.
 */
data class EngineHealthState(
    val sttStatus: EngineStatus = EngineStatus.UNKNOWN,
    val llmStatus: EngineStatus = EngineStatus.UNKNOWN,
    val ttsStatus: EngineStatus = EngineStatus.UNKNOWN,
    val lastCheckedAt: Instant = Instant.now()
) {
    val allReady: Boolean
        get() = sttStatus == EngineStatus.READY &&
                llmStatus == EngineStatus.READY &&
                ttsStatus == EngineStatus.READY

    val anyUnavailable: Boolean
        get() = sttStatus == EngineStatus.UNAVAILABLE ||
                llmStatus == EngineStatus.UNAVAILABLE ||
                ttsStatus == EngineStatus.UNAVAILABLE
}
