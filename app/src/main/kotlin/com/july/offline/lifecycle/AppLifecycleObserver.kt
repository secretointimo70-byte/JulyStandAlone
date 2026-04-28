package com.july.offline.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.state.ConversationStateHolder
import com.july.offline.domain.model.ConversationState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observer del ciclo de vida a nivel de proceso (no Activity).
 *
 * Ventaja de ProcessLifecycleOwner sobre ActivityLifecycleCallbacks:
 * - onStop no se dispara durante rotaciones de pantalla
 * - onStop solo se dispara cuando TODA la app va a background
 * - Elimina falsos positivos de "app en background" durante config changes
 *
 * Comportamiento:
 * - onStart (app visible): cancela el timer de liberación, reanuda Porcupine si procede
 * - onStop (app en background): pausa Porcupine, inicia el timer de liberación
 *
 * Registro: llamado desde JulyApplication.onCreate() via ProcessLifecycleOwner.
 * No necesita desregistrarse — vive mientras el proceso vive.
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val wakeWordCoordinator: WakeWordCoordinator,
    private val stateHolder: ConversationStateHolder,
    private val modelReleaseTimer: ModelReleaseTimer,
    private val logger: DiagnosticsLogger
) : DefaultLifecycleObserver {

    /**
     * Registra este observer en el ProcessLifecycleOwner.
     * Llamar una sola vez desde JulyApplication.onCreate().
     */
    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        logger.logInfo("AppLifecycleObserver", "Registered with ProcessLifecycleOwner")
    }

    /**
     * App vuelve a foreground (o se inicia).
     * - Cancela el timer de liberación de modelos
     * - Reanuda Porcupine si el modo wake-word estaba activo
     */
    override fun onStart(owner: LifecycleOwner) {
        logger.logInfo("AppLifecycleObserver", "App → foreground")

        // Cancelar timer de liberación de modelos
        modelReleaseTimer.cancel()

        // Reanudar Porcupine si el modo wake-word estaba activo antes de ir a background
        // Solo si el estado actual es WakeWordListening (fue pausado, no desactivado)
        val currentState = stateHolder.conversationState.value
        if (currentState is ConversationState.WakeWordListening &&
            !wakeWordCoordinator.isActive) {
            logger.logInfo("AppLifecycleObserver", "Resuming Porcupine after foreground return")
            wakeWordCoordinator.resumeAfterCycle()
        }
    }

    /**
     * App va a background.
     * - Pausa Porcupine (libera su AudioRecord para no bloquear el micrófono)
     * - Inicia el timer de liberación de modelos JNI
     *
     * NOTA: No transiciona el ConversationState. Si Porcupine estaba en
     * WakeWordListening, el estado permanece así para que onStart() sepa
     * que debe reanudarlo al volver.
     */
    override fun onStop(owner: LifecycleOwner) {
        logger.logInfo("AppLifecycleObserver", "App → background")

        // Pausar Porcupine para liberar el AudioRecord
        if (wakeWordCoordinator.isActive) {
            logger.logInfo("AppLifecycleObserver", "Pausing Porcupine (app to background)")
            wakeWordCoordinator.pauseForBackground()
        }

        // Iniciar timer de liberación de modelos JNI (solo actúa en modo MEMORY)
        modelReleaseTimer.start()
    }
}
