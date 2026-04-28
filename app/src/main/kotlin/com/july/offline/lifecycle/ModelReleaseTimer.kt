package com.july.offline.lifecycle

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timer que libera los modelos JNI tras un periodo de inactividad en background.
 *
 * Funcionamiento:
 * - Se inicia cuando la app va a background (AppLifecycleObserver.onStop)
 * - Se cancela si la app vuelve a foreground antes de que expire (onStart)
 * - Si expira sin ser cancelado, llama a ModelMemoryManager.releaseModelsIfMemoryMode()
 *
 * El timer solo tiene efecto si ModelMode == MEMORY.
 * En modo SPEED, releaseModelsIfMemoryMode() es un no-op.
 */
@Singleton
class ModelReleaseTimer @Inject constructor(
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    companion object {
        const val RELEASE_DELAY_MS = 5 * 60 * 1_000L  // 5 minutos
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var timerJob: Job? = null

    /**
     * Inicia el timer de liberación.
     * Si ya hay un timer activo, no lo reinicia.
     */
    fun start() {
        if (timerJob?.isActive == true) return

        timerJob = scope.launch {
            logger.logInfo(
                "ModelReleaseTimer",
                "Timer started — models will be released in ${RELEASE_DELAY_MS / 1000}s if in MEMORY mode"
            )
            delay(RELEASE_DELAY_MS)
            logger.logInfo("ModelReleaseTimer", "Timer expired, releasing models")
            modelMemoryManager.releaseModelsIfMemoryMode()
        }
    }

    /**
     * Cancela el timer si estaba activo.
     * Llamado cuando la app vuelve a foreground.
     */
    fun cancel() {
        if (timerJob?.isActive == true) {
            timerJob?.cancel()
            logger.logInfo("ModelReleaseTimer", "Timer cancelled — app returned to foreground")
        }
        timerJob = null
    }

    val isRunning: Boolean get() = timerJob?.isActive == true
}
