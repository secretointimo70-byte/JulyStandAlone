package com.july.offline.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.core.error.AppError
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.EmergencyState
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.orchestrator.AudioCoordinator
import com.july.offline.domain.orchestrator.ConversationOrchestrator
import com.july.offline.domain.orchestrator.EmergencyCoordinator
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.domain.orchestrator.SessionCoordinator
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.port.SessionRepository
import com.july.offline.domain.state.ConversationStateHolder
import com.july.offline.domain.state.EmergencyStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val orchestrator: ConversationOrchestrator,
    private val stateHolder: ConversationStateHolder,
    private val healthMonitor: EngineHealthMonitor,
    private val sessionCoordinator: SessionCoordinator,
    private val sessionRepository: SessionRepository,
    private val wakeWordCoordinator: WakeWordCoordinator,
    private val preferencesDataStore: AppPreferencesDataStore,
    private val emergencyStateHolder: EmergencyStateHolder,
    private val emergencyCoordinator: EmergencyCoordinator,
    private val audioCoordinator: AudioCoordinator
) : ViewModel() {

    val emergencyState: StateFlow<EmergencyState> = emergencyStateHolder.emergencyState

    private val timeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    // Ticker que mide cuántos segundos lleva el estado VAD actual + nivel de energía
    private data class VadSnapshot(val voiceSeconds: Int, val silenceSeconds: Int, val energy: Int)

    private val vadSnapshot: StateFlow<VadSnapshot> = flow {
        var lastIsSilence: Boolean? = null
        var changedAtMs = System.currentTimeMillis()
        while (true) {
            delay(200)
            val isSilence = audioCoordinator.vadIsSilence.value
            val now = System.currentTimeMillis()
            if (isSilence != lastIsSilence) {
                changedAtMs = now
                lastIsSilence = isSilence
            }
            val elapsed = ((now - changedAtMs) / 1000).toInt()
            emit(VadSnapshot(
                voiceSeconds   = if (!isSilence) elapsed else 0,
                silenceSeconds = if (isSilence)  elapsed else 0,
                energy         = audioCoordinator.vadEnergy.value.toInt()
            ))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VadSnapshot(0, 0, 0))

    val uiState: StateFlow<ConversationUiState> = combine(
        combine(
            stateHolder.conversationState,
            healthMonitor.healthState,
            sessionRepository.getRecentSessions(1),
            preferencesDataStore.wakeWordEnabled
        ) { conversationState, healthState, recentSessions, wakeWordEnabled ->
            val messages = recentSessions.firstOrNull()?.messages?.map { msg ->
                MessageUiModel(
                    id = msg.id,
                    text = msg.content,
                    isUser = msg.role == MessageRole.USER,
                    timestamp = timeFormatter.format(msg.timestamp)
                )
            } ?: emptyList()
            mapToUiState(conversationState, healthState, messages, wakeWordEnabled)
        },
        vadSnapshot
    ) { base, snap ->
        base.copy(
            vadVoiceSeconds   = snap.voiceSeconds,
            vadSilenceSeconds = snap.silenceSeconds,
            vadEnergyLevel    = snap.energy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationUiState()
    )

    fun onMicPressed() = orchestrator.startConversationCycle()
    fun onCancelPressed() = orchestrator.cancelCurrentCycle()
    fun onPermissionDenied() {}
    fun onEmergencyButtonPressed() = emergencyCoordinator.activateByButton()

    /** Activa o desactiva el modo de escucha pasiva de wake-word. */
    fun onWakeWordToggled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setWakeWordEnabled(enabled)
            if (enabled) {
                wakeWordCoordinator.startWakeWordListening()
            } else {
                wakeWordCoordinator.stopWakeWordListening()
            }
        }
    }

    private fun mapToUiState(
        state: ConversationState,
        health: com.july.offline.domain.model.EngineHealthState,
        messages: List<MessageUiModel>,
        wakeWordEnabled: Boolean
    ): ConversationUiState {
        val engineHealthUi = EngineHealthUiState.from(health)
        return when (state) {
            is ConversationState.Idle -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                isMicButtonEnabled = engineHealthUi.llmReady && engineHealthUi.sttReady,
                errorMessage = buildEngineUnavailableMessage(engineHealthUi),
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.WakeWordListening -> ConversationUiState(
                phase = ConversationPhase.WAKE_WORD_LISTENING,
                engineHealth = engineHealthUi,
                isMicButtonEnabled = true,
                messages = messages,
                isWakeWordActive = true
            )
            is ConversationState.Listening -> ConversationUiState(
                phase = ConversationPhase.LISTENING,
                displayedText = "Escuchando...",
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Transcribing -> ConversationUiState(
                phase = ConversationPhase.TRANSCRIBING,
                displayedText = "Procesando audio...",
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Thinking -> ConversationUiState(
                phase = ConversationPhase.THINKING,
                displayedText = "Pensando...",
                transcriptText = state.transcript.text,
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Speaking -> ConversationUiState(
                phase = ConversationPhase.SPEAKING,
                displayedText = state.response.text,
                engineHealth = engineHealthUi,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Error -> ConversationUiState(
                phase = ConversationPhase.ERROR,
                errorMessage = mapErrorToUserMessage(state.error),
                engineHealth = engineHealthUi,
                isMicButtonEnabled = state.error !is AppError.Permission,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Cancelled -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
        }
    }

    private fun buildEngineUnavailableMessage(health: EngineHealthUiState): String? {
        if (health.sttReady && health.llmReady) return null
        val missing = buildList {
            if (!health.sttReady) add("whisper-small.bin")
            if (!health.llmReady) add("Llama-3.2-1B-Instruct-Q4_K_M.gguf")
        }
        return "Modelos no encontrados: ${missing.joinToString(", ")} — ejecuta install-models.bat"
    }

    private fun mapErrorToUserMessage(error: AppError): String = when (error) {
        is AppError.Permission -> "Se necesita permiso de micrófono"
        is AppError.Stt -> "No pude entenderte. Intenta de nuevo."
        is AppError.Llm -> "El asistente no está disponible."
        is AppError.Tts -> "No pude reproducir la respuesta."
        is AppError.Network -> "El servidor de IA no está disponible."
        is AppError.Cancelled -> ""
        is AppError.Unknown -> "Error inesperado."
    }
}
