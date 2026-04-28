package com.july.offline.domain.port

import com.july.offline.domain.model.Message
import com.july.offline.domain.model.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistencia de sesiones y mensajes.
 * Implementado en capa data/.
 */
interface SessionRepository {

    /** Crea una nueva sesión y la persiste. Devuelve la entidad creada. */
    suspend fun createSession(): SessionEntity

    /** Añade un mensaje a una sesión existente. */
    suspend fun addMessage(sessionId: String, message: Message)

    /** Recupera una sesión por ID incluyendo sus mensajes. null si no existe. */
    suspend fun getSession(sessionId: String): SessionEntity?

    /**
     * Flow reactivo de sesiones recientes ordenadas por updatedAt DESC.
     * @param limit número máximo de sesiones a devolver
     */
    fun getRecentSessions(limit: Int = 20): Flow<List<SessionEntity>>

    /** Elimina una sesión y todos sus mensajes. */
    suspend fun deleteSession(sessionId: String)
}
