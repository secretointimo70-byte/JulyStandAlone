package com.july.offline.ui.conversation

import com.july.offline.domain.model.EngineHealthState
import com.july.offline.domain.model.EngineStatus

data class ConversationUiState(
    val phase: ConversationPhase = ConversationPhase.IDLE,
    val displayedText: String = "",
    val transcriptText: String = "",
    val errorMessage: String? = null,
    val engineHealth: EngineHealthUiState = EngineHealthUiState(),
    val isCancelVisible: Boolean = false,
    val isMicButtonEnabled: Boolean = true,
    val messages: List<MessageUiModel> = emptyList(),
    val isWakeWordActive: Boolean = false,
    val vadVoiceSeconds: Int = 0,
    val vadSilenceSeconds: Int = 0,
    val vadEnergyLevel: Int = 0
)

enum class ConversationPhase {
    IDLE,
    WAKE_WORD_LISTENING,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING,
    ERROR,
    CANCELLED
}

data class EngineHealthUiState(
    val sttReady: Boolean = false,
    val llmReady: Boolean = false,
    val ttsReady: Boolean = false,
    val showWarning: Boolean = false
) {
    companion object {
        fun from(health: EngineHealthState) = EngineHealthUiState(
            sttReady = health.sttStatus == EngineStatus.READY,
            llmReady = health.llmStatus == EngineStatus.READY,
            ttsReady = health.ttsStatus == EngineStatus.READY,
            showWarning = health.anyUnavailable
        )
    }
}

data class MessageUiModel(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)
