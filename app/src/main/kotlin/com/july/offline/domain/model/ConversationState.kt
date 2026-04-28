package com.july.offline.domain.model

sealed class ConversationState {

    object Idle : ConversationState()

    /**
     * FASE 4.
     * Porcupine está activo, escuchando pasivamente el audio del micrófono.
     * Este estado persiste hasta que:
     * a) Se detecta el wake-word → transición a Listening
     * b) El usuario desactiva el modo wake-word → transición a Idle
     * c) La app va a background → Porcupine se pausa (estado se preserva)
     */
    object WakeWordListening : ConversationState()

    data class Listening(val sessionId: String) : ConversationState()

    data class Transcribing(
        val sessionId: String,
        val audioLengthMs: Long
    ) : ConversationState()

    data class Thinking(
        val sessionId: String,
        val transcript: Transcript
    ) : ConversationState()

    data class Speaking(
        val sessionId: String,
        val response: LlmResponse,
        val fallbackText: String = response.text
    ) : ConversationState()

    data class Error(
        val error: com.july.offline.core.error.AppError,
        val previousState: ConversationState
    ) : ConversationState()

    object Cancelled : ConversationState()
}
