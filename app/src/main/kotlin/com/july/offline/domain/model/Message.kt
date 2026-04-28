package com.july.offline.domain.model

import java.time.Instant

/** Rol del emisor del mensaje en la conversación. */
enum class MessageRole { USER, ASSISTANT, SYSTEM }

/**
 * Mensaje individual de la conversación.
 * Inmutable. Toda modificación produce una nueva instancia.
 */
data class Message(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant = Instant.now()
)
