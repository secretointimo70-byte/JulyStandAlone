package com.july.offline.domain.model

import com.july.offline.core.error.AppError
import java.time.Instant

/**
 * Estado de runtime de la sesión activa.
 * Complementa ConversationState con métricas y contexto de sesión.
 */
data class RuntimeState(
    val currentSessionId: String? = null,
    val cycleCount: Int = 0,
    val lastErrorAt: Instant? = null,
    val lastError: AppError? = null,
    val appStartedAt: Instant = Instant.now()
)
