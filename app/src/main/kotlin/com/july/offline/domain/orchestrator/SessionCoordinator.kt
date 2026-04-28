package com.july.offline.domain.orchestrator

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.SessionEntity
import com.july.offline.domain.port.SessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinador de sesiones.
 * Crea y mantiene la sesión activa. Persiste mensajes.
 * No transiciona estados de conversación.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logger: DiagnosticsLogger
) {

    private var currentSession: SessionEntity? = null

    /**
     * Garantiza que hay una sesión activa.
     * Si no existe, crea una nueva.
     * @return ID de la sesión activa
     */
    suspend fun ensureActiveSession(): String {
        val session = currentSession ?: sessionRepository.createSession().also {
            currentSession = it
            logger.logInfo("SessionCoordinator", "Created session: ${it.id}")
        }
        return session.id
    }

    /** Añade un mensaje a la sesión activa y actualiza la caché local. */
    suspend fun addMessage(sessionId: String, message: Message) {
        sessionRepository.addMessage(sessionId, message)
        currentSession = currentSession?.let { session ->
            session.copy(messages = session.messages + message)
        }
        logger.logInfo("SessionCoordinator", "Added ${message.role} message to $sessionId")
    }

    /**
     * Devuelve el historial de mensajes de la sesión activa.
     * Usado por el orquestador para construir el contexto del LLM.
     */
    suspend fun getHistory(sessionId: String): List<Message> {
        return currentSession?.messages
            ?: sessionRepository.getSession(sessionId)?.messages
            ?: emptyList()
    }

    /** Cierra la sesión activa. El próximo ensureActiveSession() creará una nueva. */
    fun closeCurrentSession() {
        logger.logInfo("SessionCoordinator", "Closing session: ${currentSession?.id}")
        currentSession = null
    }
}
