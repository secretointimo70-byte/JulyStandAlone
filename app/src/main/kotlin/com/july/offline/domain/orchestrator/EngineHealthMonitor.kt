package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.EngineHealthState
import com.july.offline.domain.model.EngineStatus
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor de salud de los tres motores del sistema.
 * Verifica disponibilidad cada 30 segundos de forma continua.
 * Expone EngineHealthState como StateFlow observable por la UI.
 */
@Singleton
class EngineHealthMonitor @Inject constructor(
    private val sttEngine: SpeechToTextEngine,
    private val llmEngine: LanguageModelEngine,
    private val ttsEngine: TextToSpeechEngine,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    companion object {
        private const val CHECK_INTERVAL_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _healthState = MutableStateFlow(EngineHealthState())
    val healthState: StateFlow<EngineHealthState> = _healthState.asStateFlow()

    /** Inicia el ciclo de monitoreo. Llamado desde JulyApplication.onCreate(). */
    fun startMonitoring() {
        scope.launch {
            while (true) {
                checkAll()
                delay(CHECK_INTERVAL_MS)
            }
        }
        logger.logInfo("EngineHealthMonitor", "Monitoring started")
    }

    /** Fuerza una verificación inmediata. Útil tras un error de motor. */
    suspend fun forceCheck() {
        checkAll()
    }

    private suspend fun checkAll() {
        val sttStatus = checkEngine("STT") { sttEngine.isAvailable() }
        val llmStatus = checkEngine("LLM") { llmEngine.isAvailable() }
        val ttsStatus = checkEngine("TTS") { ttsEngine.isAvailable() }

        val newHealth = EngineHealthState(
            sttStatus = sttStatus,
            llmStatus = llmStatus,
            ttsStatus = ttsStatus,
            lastCheckedAt = Instant.now()
        )

        _healthState.value = newHealth

        if (newHealth.anyUnavailable) {
            logger.logWarning(
                "EngineHealthMonitor",
                "Degraded health — STT:$sttStatus LLM:$llmStatus TTS:$ttsStatus"
            )
        }
    }

    private suspend fun checkEngine(name: String, check: suspend () -> Boolean): EngineStatus {
        return try {
            if (check()) EngineStatus.READY else EngineStatus.UNAVAILABLE
        } catch (e: Throwable) {
            logger.logError("EngineHealthMonitor", "$name check failed (likely missing .so or model)", e)
            EngineStatus.UNAVAILABLE
        }
    }
}
