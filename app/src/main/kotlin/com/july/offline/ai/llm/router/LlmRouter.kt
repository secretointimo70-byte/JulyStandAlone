package com.july.offline.ai.llm.router

import com.july.offline.ai.llm.LocalServerLLMAdapter
import com.july.offline.ai.llm.embedded.LlamaCppLLMAdapter
import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.data.datastore.SystemConfigDataStore
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router LLM que implementa la estrategia embebido-primario / servidor-fallback.
 *
 * Lógica de routing:
 * - AUTO (default): intenta embebido → si falla con error no-network, intenta servidor
 * - EMBEDDED: solo embebido, sin fallback
 * - SERVER: solo servidor local, sin embebido
 *
 * Definición de "falla que activa fallback":
 * - AppError.Llm con retryable = false (modelo no cargado, OOM, timeout JNI)
 * - AppError.Network del embebido (no debería ocurrir pero se maneja)
 * NO activa fallback:
 * - AppError.Llm con retryable = true (se reintenta en el orquestador)
 * - Éxito pero respuesta vacía (se propaga como error normal)
 *
 * El modo se puede cambiar en runtime desde SettingsViewModel.
 */
@Singleton
class LlmRouter @Inject constructor(
    private val embeddedAdapter: LlamaCppLLMAdapter,
    private val serverAdapter: LocalServerLLMAdapter,
    private val logger: DiagnosticsLogger,
    systemConfigDataStore: SystemConfigDataStore
) : LanguageModelEngine {

    /** Modo actual. Inicializado desde DataStore y modificable en runtime desde Settings. */
    @Volatile var currentMode: LlmMode = runBlocking { systemConfigDataStore.llmMode.first() }

    override suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {

        return when (currentMode) {
            LlmMode.EMBEDDED -> {
                logger.logInfo("LlmRouter", "Using embedded LLM (EMBEDDED mode)")
                embeddedAdapter.generate(prompt, history)
            }

            LlmMode.SERVER -> {
                logger.logInfo("LlmRouter", "Using server LLM (SERVER mode)")
                serverAdapter.generate(prompt, history)
            }

            LlmMode.AUTO -> generateWithFallback(prompt, history)
        }
    }

    private suspend fun generateWithFallback(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {

        logger.logInfo("LlmRouter", "Using embedded LLM (AUTO mode — primary)")
        val embeddedResult = embeddedAdapter.generate(prompt, history)

        // Si el embebido tuvo éxito, devolver directamente
        if (embeddedResult.isSuccess) return embeddedResult

        val error = embeddedResult.errorOrNull()

        // Decidir si activar fallback
        val shouldFallback = when (error) {
            is AppError.Llm -> !error.retryable  // fallo permanente del embebido
            is AppError.Network -> true           // no debería pasar, pero fallback de seguridad
            else -> false
        }

        if (!shouldFallback) {
            logger.logWarning("LlmRouter", "Embedded failed with retryable error, not falling back")
            return embeddedResult
        }

        // Verificar si el servidor está disponible antes de intentar
        if (!serverAdapter.isAvailable()) {
            logger.logWarning(
                "LlmRouter",
                "Embedded failed and server is unreachable — both unavailable"
            )
            return JulyResult.failure(
                AppError.Llm(
                    message = "Both embedded LLM and local server are unavailable",
                    retryable = false
                )
            )
        }

        logger.logWarning(
            "LlmRouter",
            "Embedded failed (${error?.message}), trying server fallback"
        )
        val serverResult = serverAdapter.generate(prompt, history)

        if (serverResult.isSuccess) {
            logger.logInfo("LlmRouter", "Server fallback succeeded")
        } else {
            logger.logError("LlmRouter", "Both embedded and server failed")
        }

        return serverResult
    }

    override suspend fun isAvailable(): Boolean {
        return when (currentMode) {
            LlmMode.EMBEDDED -> embeddedAdapter.isAvailable()
            LlmMode.SERVER -> serverAdapter.isAvailable()
            LlmMode.AUTO -> embeddedAdapter.isAvailable() || serverAdapter.isAvailable()
        }
    }

    override suspend fun getModelInfo(): ModelInfo {
        return when (currentMode) {
            LlmMode.EMBEDDED -> embeddedAdapter.getModelInfo()
            LlmMode.SERVER -> serverAdapter.getModelInfo()
            LlmMode.AUTO -> if (embeddedAdapter.isAvailable())
                embeddedAdapter.getModelInfo()
            else
                serverAdapter.getModelInfo()
        }
    }
}
