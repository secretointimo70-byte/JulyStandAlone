package com.july.offline.domain.state

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.RuntimeState
import com.july.offline.domain.model.Transcript
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FUENTE ÚNICA DE VERDAD del estado del sistema.
 *
 * Singleton inyectado por Hilt. Solo ConversationOrchestrator debe llamar
 * a los métodos de mutación. La UI y ViewModel solo leen los StateFlow.
 *
 * Thread-safe: MutableStateFlow garantiza visibilidad y atomicidad de escrituras.
 */
@Singleton
class ConversationStateHolder @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    private val _runtimeState = MutableStateFlow(RuntimeState())
    val runtimeState: StateFlow<RuntimeState> = _runtimeState.asStateFlow()

    // ── Transiciones de estado conversacional ─────────────────────────────

    internal fun transitionToListening(sessionId: String) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Listening(sessionId)
        logger.logStateTransition(previous.javaClass.simpleName, "Listening", "user_input")
    }

    internal fun transitionToTranscribing(sessionId: String, audioLengthMs: Long) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Transcribing(sessionId, audioLengthMs)
        logger.logStateTransition(previous.javaClass.simpleName, "Transcribing", "vad_end")
    }

    internal fun transitionToThinking(sessionId: String, transcript: Transcript) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Thinking(sessionId, transcript)
        logger.logStateTransition(previous.javaClass.simpleName, "Thinking", "transcript_ready")
    }

    internal fun transitionToSpeaking(sessionId: String, response: LlmResponse) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Speaking(sessionId, response)
        logger.logStateTransition(previous.javaClass.simpleName, "Speaking", "llm_response")
    }

    internal fun transitionToError(error: AppError) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Error(error, previous)
        _runtimeState.update { it.copy(lastError = error, lastErrorAt = Instant.now()) }
        logger.logStateTransition(previous.javaClass.simpleName, "Error", error.message)
    }

    internal fun transitionToCancelled() {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Cancelled
        logger.logStateTransition(previous.javaClass.simpleName, "Cancelled", "user_cancel")
    }

    internal fun resetToIdle() {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Idle
        logger.logStateTransition(previous.javaClass.simpleName, "Idle", "reset")
    }

    // ── FASE 4 — Wake-word transitions ───────────────────────────────────

    /**
     * Transición al estado de escucha pasiva de wake-word.
     * Solo válida desde Idle. Desde cualquier otro estado se ignora.
     */
    internal fun transitionToWakeWordListening() {
        val current = _conversationState.value
        if (current !is ConversationState.Idle) {
            logger.logWarning(
                "StateHolder",
                "transitionToWakeWordListening() called from $current, ignoring"
            )
            return
        }
        _conversationState.value = ConversationState.WakeWordListening
        logger.logStateTransition("Idle", "WakeWordListening", "wake_word_enabled")
    }

    /**
     * Vuelve a WakeWordListening tras completar un ciclo de conversación,
     * si el modo wake-word sigue activo.
     */
    internal fun resetToWakeWordListening() {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.WakeWordListening
        logger.logStateTransition(
            previous.javaClass.simpleName,
            "WakeWordListening",
            "cycle_complete_wake_word_active"
        )
    }

    // ── Mutaciones de RuntimeState ────────────────────────────────────────

    internal fun setCurrentSession(sessionId: String?) {
        _runtimeState.update { it.copy(currentSessionId = sessionId) }
    }

    internal fun incrementCycleCount() {
        _runtimeState.update { it.copy(cycleCount = it.cycleCount + 1) }
    }
}
