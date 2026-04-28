package com.july.offline.core.memory

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor de OutOfMemoryError para operaciones JNI.
 *
 * Los modelos JNI (Whisper ~466MB, Piper ~300MB, llama.cpp ~2GB) pueden
 * causar OOM si el sistema tiene presión de memoria. Un OOM no capturado
 * en un hilo de IO mata el proceso sin posibilidad de limpieza.
 *
 * Estrategia:
 * 1. Capturar OOM en el adaptador JNI
 * 2. Notificar a OomHandler
 * 3. OomHandler llama a ModelMemoryManager.releaseModelsIfMemoryMode()
 *    independientemente del modo actual (liberación de emergencia)
 * 4. El adaptador devuelve AppError para que el orquestador lo gestione
 *
 * IMPORTANTE: OOM en JNI puede dejar el heap nativo en estado inconsistente.
 * Después de un OOM siempre liberamos todos los contextos JNI y pedimos
 * al usuario que reinicie la sesión.
 */
@Singleton
class OomHandler @Inject constructor(
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger
) {

    private val scope = CoroutineScope(SupervisorJob())

    /**
     * Maneja un OOM ocurrido durante una operación JNI.
     * Libera todos los modelos de forma asíncrona y registra el evento.
     *
     * @param engine nombre del motor que causó el OOM (para logging)
     * @param oom el error capturado
     */
    fun handle(engine: String, oom: OutOfMemoryError) {
        logger.logError(
            tag = "OomHandler",
            message = "OOM in $engine — releasing all JNI contexts immediately",
            cause = oom
        )

        // Liberar todos los modelos independientemente del modo configurado
        scope.launch {
            try {
                // Forzar liberación aunque estemos en modo SPEED
                modelMemoryManager.currentMode = ModelMode.MEMORY
                modelMemoryManager.releaseModelsIfMemoryMode()
                // No restaurar el modo — el usuario debe reiniciar la sesión
                logger.logWarning(
                    "OomHandler",
                    "All JNI models released after OOM. App requires session restart."
                )
            } catch (e: Exception) {
                logger.logError("OomHandler", "Failed to release models after OOM", e)
            }
        }
    }

    /**
     * Wrapper seguro para operaciones JNI propensas a OOM.
     * Usa inline para evitar overhead de lambda en el hot path de inferencia.
     */
    inline fun <T> safeJniCall(
        engine: String,
        onOom: () -> T,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (oom: OutOfMemoryError) {
            handle(engine, oom)
            onOom()
        }
    }
}
