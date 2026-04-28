package com.july.offline.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Contrato del motor de detección de wake-word.
 * Sin dependencias Android. Implementado en capa wakeword/.
 *
 * El motor escucha audio del micrófono de forma continua y emite
 * un evento cuando detecta la palabra clave configurada.
 */
interface WakeWordEngine {

    /**
     * Inicia la escucha pasiva y emite eventos cuando se detecta el wake-word.
     * El Flow permanece activo hasta que se llama a stop().
     *
     * Garantías:
     * - No lanza excepciones al detectar; los errores se emiten como WakeWordEvent.Error
     * - Es responsabilidad del WakeWordCoordinator suscribirse y cancelar el Flow
     * - El motor gestiona su propio AudioRecord interno (separado del de grabación)
     */
    fun startListening(): Flow<WakeWordEvent>

    /** Detiene la escucha y libera el AudioRecord interno. Idempotente. */
    fun stop()

    /** true si el motor está actualmente escuchando. */
    val isListening: Boolean

    /** Verifica si el motor está disponible (clave API válida, modelo presente). */
    suspend fun isAvailable(): Boolean
}

/** Eventos emitidos por WakeWordEngine. */
sealed class WakeWordEvent {
    /** Wake-word detectado. `keywordIndex` identifica cuál keyword si hay varios. */
    data class Detected(val keywordIndex: Int = 0) : WakeWordEvent()
    /** Error durante la escucha. El coordinador debe decidir si reintentar. */
    data class Error(val cause: Throwable) : WakeWordEvent()
}
