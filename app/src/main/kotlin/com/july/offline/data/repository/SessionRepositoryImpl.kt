package com.july.offline.data.repository

import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.entity.MessageDbEntity
import com.july.offline.data.db.entity.SessionDbEntity
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.SessionEntity
import com.july.offline.domain.port.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao
) : SessionRepository {

    override suspend fun createSession(): SessionEntity {
        val now = Instant.now()
        val entity = SessionDbEntity(
            id = UUID.randomUUID().toString(),
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            title = ""
        )
        sessionDao.insert(entity)
        return entity.toDomain(emptyList())
    }

    override suspend fun addMessage(sessionId: String, message: Message) {
        messageDao.insert(message.toDbEntity(sessionId))
        sessionDao.getById(sessionId)?.let { session ->
            sessionDao.update(session.copy(updatedAt = Instant.now().toEpochMilli()))
        }
    }

    override suspend fun getSession(sessionId: String): SessionEntity? {
        val session = sessionDao.getById(sessionId) ?: return null
        val messages = messageDao.getBySession(sessionId).map { it.toDomain() }
        return session.toDomain(messages)
    }

    override fun getRecentSessions(limit: Int): Flow<List<SessionEntity>> {
        return sessionDao.getRecent(limit).map { sessions ->
            sessions.map { it.toDomain(emptyList()) }
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteById(sessionId)
    }

    private fun SessionDbEntity.toDomain(messages: List<Message>) = SessionEntity(
        id = id,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        messages = messages,
        title = title
    )

    private fun MessageDbEntity.toDomain() = Message(
        id = id,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = Instant.ofEpochMilli(timestamp)
    )

    private fun Message.toDbEntity(sessionId: String) = MessageDbEntity(
        id = id,
        sessionId = sessionId,
        role = role.name,
        content = content,
        timestamp = timestamp.toEpochMilli()
    )
}
