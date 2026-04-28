package com.july.offline.domain.model

import java.time.Instant

/**
 * Entidad de sesión de conversación en el dominio.
 * Una sesión agrupa todos los mensajes de una conversación continua.
 */
data class SessionEntity(
    val id: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val messages: List<Message> = emptyList(),
    val title: String = ""
) {
    val messageCount: Int get() = messages.size
    val lastMessage: Message? get() = messages.lastOrNull()
}
