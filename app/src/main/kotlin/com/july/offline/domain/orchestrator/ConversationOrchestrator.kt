package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.error.AppError
import com.july.offline.core.error.ErrorAction
import com.july.offline.core.error.ErrorHandler
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.model.EngineHealthState
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import com.july.offline.domain.state.ConversationStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestador central del ciclo de conversación.
 *
 * ÚNICO actor autorizado a llamar métodos de mutación de ConversationStateHolder.
 * Coordina: VAD_END → STT → LLM → TTS → IDLE (o WakeWordListening).
 * Integra WakeWordCoordinator para modo pasivo de detección (FASE 4).
 */
@Singleton
class ConversationOrchestrator @Inject constructor(
    private val stateHolder: ConversationStateHolder,
    private val audioCoordinator: AudioCoordinator,
    private val sessionCoordinator: SessionCoordinator,
    private val wakeWordCoordinator: WakeWordCoordinator,
    private val emergencyCoordinator: EmergencyCoordinator,
    private val healthMonitor: EngineHealthMonitor,
    private val sttEngine: SpeechToTextEngine,
    private val llmEngine: LanguageModelEngine,
    private val ttsEngine: TextToSpeechEngine,
    private val errorHandler: ErrorHandler,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private var activeJob: Job? = null

    private val _continuousMode = MutableStateFlow(false)
    val continuousMode: StateFlow<Boolean> = _continuousMode.asStateFlow()

    init {
        wakeWordCoordinator.onWakeWordDetected = { startConversationCycle() }
        emergencyCoordinator.onCancelConversation = {
            activeJob?.cancel()
            activeJob = null
            audioCoordinator.cancel()
        }
        emergencyCoordinator.onResumeConversation = {
            stateHolder.resetToIdle()
        }
        scope.launch {
            healthMonitor.healthState.first { it.allReady }
            logger.logInfo("Orchestrator", "All engines ready — playing greeting")
            speakFeedback("Hola, soy July. Estoy lista.")
        }
    }

    fun startConversationCycle() {
        if (emergencyCoordinator.isEmergencyActive()) {
            logger.logWarning("Orchestrator", "Emergency active, ignoring conversation cycle")
            return
        }
        val currentState = stateHolder.conversationState.value
        val validStartStates = setOf(
            ConversationState.Idle::class,
            ConversationState.WakeWordListening::class,
            ConversationState.Cancelled::class,
            ConversationState.Error::class
        )
        if (currentState::class !in validStartStates) {
            logger.logWarning("Orchestrator", "startConversationCycle() from $currentState, ignoring")
            return
        }

        activeJob = scope.launch { runCycle() }
        activeJob?.let { emergencyCoordinator.registerActiveJob(it) }
    }

    fun toggleContinuousMode() {
        _continuousMode.value = !_continuousMode.value
        logger.logInfo("Orchestrator", "Continuous mode: ${_continuousMode.value}")
    }

    fun cancelCurrentCycle() {
        _continuousMode.value = false
        activeJob?.cancel()
        activeJob = null
        audioCoordinator.cancel()
        stateHolder.transitionToCancelled()

        scope.launch {
            delay(100)
            if (wakeWordCoordinator.isActive) {
                wakeWordCoordinator.resumeAfterCycle()
            } else {
                stateHolder.resetToIdle()
            }
        }
        logger.logInfo("Orchestrator", "Cycle cancelled")
    }

    private suspend fun runCycle() = withContext(dispatchers.main) {
        val sessionId = sessionCoordinator.ensureActiveSession()
        stateHolder.setCurrentSession(sessionId)

        // ── 1. LISTENING ───────────────────────────────────────────────
        stateHolder.transitionToListening(sessionId)
        val audioResult = try {
            audioCoordinator.recordUntilSilence()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.logError("Orchestrator", "Recording error: ${e.message}", e)
            stateHolder.transitionToCancelled()
            delay(100)
            resumeOrIdle()
            return@withContext
        }

        if (audioResult == null) {
            stateHolder.transitionToCancelled()
            delay(100)
            resumeOrIdle()
            return@withContext
        }

        val (audioBytes, audioLengthMs) = audioResult

        // ── 2. TRANSCRIBING ────────────────────────────────────────────
        stateHolder.transitionToTranscribing(sessionId, audioLengthMs)
        val startStt = System.currentTimeMillis()

        val transcript: Transcript = when (val result = sttEngine.transcribe(audioBytes)) {
            is JulyResult.Success -> {
                logger.logEngineEvent("STT", "transcribed", System.currentTimeMillis() - startStt)
                result.data
            }
            is JulyResult.Failure -> {
                handleError(result.error)
                return@withContext
            }
        }

        if (transcript.isEmpty) {
            logger.logInfo("Orchestrator", "Empty transcript, resuming")
            resumeOrIdle()
            return@withContext
        }

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = transcript.text
        )
        sessionCoordinator.addMessage(sessionId, userMessage)

        // ── 3. THINKING ────────────────────────────────────────────────
        stateHolder.transitionToThinking(sessionId, transcript)
        val history = sessionCoordinator.getHistory(sessionId)
        val startLlm = System.currentTimeMillis()

        val llmResponse: LlmResponse = when (val result = llmEngine.generate(transcript.text, history)) {
            is JulyResult.Success -> {
                logger.logEngineEvent("LLM", "generated", System.currentTimeMillis() - startLlm)
                result.data
            }
            is JulyResult.Failure -> {
                if (result.error is AppError.Llm && result.error.retryable) {
                    delay(500L)
                    when (val retry = llmEngine.generate(transcript.text, history)) {
                        is JulyResult.Success -> retry.data
                        is JulyResult.Failure -> {
                            handleError(retry.error)
                            return@withContext
                        }
                    }
                } else {
                    handleError(result.error)
                    return@withContext
                }
            }
        }

        val assistantMessage = Message(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            content = llmResponse.text
        )
        sessionCoordinator.addMessage(sessionId, assistantMessage)

        // ── 4. SPEAKING ────────────────────────────────────────────────
        stateHolder.transitionToSpeaking(sessionId, llmResponse)
        val startTts = System.currentTimeMillis()

        when (val ttsResult = ttsEngine.synthesize(cleanForTts(llmResponse.text))) {
            is JulyResult.Success -> {
                logger.logEngineEvent("TTS", "synthesized", System.currentTimeMillis() - startTts)
                audioCoordinator.playAudio(ttsResult.data)
            }
            is JulyResult.Failure -> {
                logger.logWarning("Orchestrator", "TTS failed, showing text fallback")
                delay(2_000L)
            }
        }

        // ── 5. RESET ───────────────────────────────────────────────────
        stateHolder.incrementCycleCount()
        resumeOrIdle()
    }

    private fun resumeOrIdle() {
        when {
            _continuousMode.value -> {
                stateHolder.resetToIdle()
                startConversationCycle()
            }
            wakeWordCoordinator.isActive -> wakeWordCoordinator.resumeAfterCycle()
            else -> stateHolder.resetToIdle()
        }
    }

    private suspend fun handleError(error: AppError) {
        stateHolder.transitionToError(error)
        when (errorHandler.handle(error)) {
            is ErrorAction.ResetToIdle,
            is ErrorAction.FallbackToText,
            is ErrorAction.Retry -> {
                if (error is AppError.Stt) {
                    speakFeedback("No te escuché. Intenta de nuevo.")
                } else {
                    delay(1_500L)
                }
                resumeOrIdle()
            }
            is ErrorAction.ShowPermissionRationale -> { /* UI gestiona via estado Error */ }
        }
    }

    private fun cleanForTts(text: String): String {
        return text
            .replace(Regex("<\\|[^|]*\\|>"), "")   // <|system|> <|end|> etc.
            .replace(Regex("<[^>]+>"), "")           // <cualquier tag HTML/XML>
            .replace(Regex("[*#`_~<>]"), "")         // markdown + símbolos < > sueltos
            .replace(Regex("^\\s*[-•]\\s*", RegexOption.MULTILINE), "") // viñetas al inicio de línea
            .replace(Regex("\\s{2,}"), " ")          // espacios múltiples
            .trim()
    }

    private suspend fun speakFeedback(text: String) {
        try {
            when (val result = ttsEngine.synthesize(text)) {
                is JulyResult.Success -> audioCoordinator.playAudio(result.data)
                is JulyResult.Failure -> delay(1_500L)
            }
        } catch (e: Exception) {
            delay(1_500L)
        }
    }
}
