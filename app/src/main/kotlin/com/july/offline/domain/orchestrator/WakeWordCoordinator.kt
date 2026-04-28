package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.domain.port.WakeWordEvent
import com.july.offline.domain.state.ConversationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinador del motor de wake-word.
 *
 * Responsabilidades:
 * - Iniciar y detener la escucha pasiva de Porcupine
 * - Transicionar el estado a WakeWordListening cuando se activa
 * - Notificar al ConversationOrchestrator cuando se detecta el wake-word
 * - Manejar errores del motor
 *
 * FASE 5: añade pauseForBackground() para suspender cuando la app va a background.
 */
@Singleton
class WakeWordCoordinator @Inject constructor(
    private val wakeWordEngine: WakeWordEngine,
    private val stateHolder: ConversationStateHolder,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var listeningJob: Job? = null

    @Volatile private var _pausedForBackground = false
    val wasActiveBeforeBackground: Boolean get() = _pausedForBackground

    /** Callback invocado cuando Porcupine detecta el wake-word. */
    var onWakeWordDetected: (() -> Unit)? = null

    /**
     * Activa la escucha pasiva.
     * Si ya está activa, no hace nada.
     */
    fun startWakeWordListening() {
        if (wakeWordEngine.isListening) {
            logger.logDebug("WakeWordCoordinator", "Already listening, ignoring startWakeWordListening()")
            return
        }

        stateHolder.transitionToWakeWordListening()

        listeningJob = scope.launch {
            logger.logInfo("WakeWordCoordinator", "Starting Porcupine wake-word listening")

            wakeWordEngine.startListening()
                .catch { cause ->
                    logger.logError("WakeWordCoordinator", "Wake-word stream error", cause)
                    kotlinx.coroutines.delay(1_000L)
                    emit(WakeWordEvent.Error(cause))
                }
                .collect { event ->
                    when (event) {
                        is WakeWordEvent.Detected -> {
                            logger.logInfo(
                                "WakeWordCoordinator",
                                "Wake-word detected (keyword index: ${event.keywordIndex})"
                            )
                            wakeWordEngine.stop()
                            onWakeWordDetected?.invoke()
                        }
                        is WakeWordEvent.Error -> {
                            logger.logError(
                                "WakeWordCoordinator",
                                "Wake-word engine error: ${event.cause.message}"
                            )
                        }
                    }
                }
        }
    }

    /**
     * Desactiva la escucha pasiva y vuelve a IDLE.
     * Llamado cuando el usuario desactiva el toggle de wake-word.
     */
    fun stopWakeWordListening() {
        listeningJob?.cancel()
        listeningJob = null
        wakeWordEngine.stop()
        stateHolder.resetToIdle()
        logger.logInfo("WakeWordCoordinator", "Wake-word listening stopped")
    }

    /**
     * Reinicia la escucha tras completar un ciclo de conversación.
     * Llamado por ConversationOrchestrator si el modo wake-word estaba activo.
     */
    fun resumeAfterCycle() {
        if (!wakeWordEngine.isListening) {
            startWakeWordListening()
        }
    }

    /**
     * Pausa la escucha cuando la app va a background (FASE 5).
     * Guarda el estado para restaurarlo al volver a foreground.
     */
    fun pauseForBackground() {
        if (wakeWordEngine.isListening || listeningJob?.isActive == true) {
            _pausedForBackground = true
            listeningJob?.cancel()
            listeningJob = null
            wakeWordEngine.stop()
            logger.logInfo("WakeWordCoordinator", "Paused for background")
        }
    }

    /**
     * Restaura la escucha cuando la app vuelve a foreground.
     */
    fun resumeFromBackground() {
        if (_pausedForBackground) {
            _pausedForBackground = false
            startWakeWordListening()
            logger.logInfo("WakeWordCoordinator", "Resumed from background")
        }
    }

    val isActive: Boolean get() = wakeWordEngine.isListening || listeningJob?.isActive == true
}
