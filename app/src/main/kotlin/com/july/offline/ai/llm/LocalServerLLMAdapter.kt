package com.july.offline.ai.llm

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.data.network.NetworkHealthChecker
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Adaptador LLM que consume un servidor HTTP local compatible con la API de Ollama.
 * Construye el payload con historial + system prompt y mapea la respuesta al dominio.
 */
class LocalServerLLMAdapter @Inject constructor(
    private val apiService: LlmApiService,
    private val config: LlmServerConfig,
    private val networkHealthChecker: NetworkHealthChecker,
    private val logger: DiagnosticsLogger
) : LanguageModelEngine {

    override suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {
        return try {
            val messages = buildMessageList(prompt, history)
            val request = LlmChatRequest(
                model = config.modelName,
                messages = messages,
                stream = false
            )

            logger.logInfo("LocalServerLLM", "Generating for model: ${config.modelName}")
            val startMs = System.currentTimeMillis()
            val response = apiService.chat(request)
            val latencyMs = System.currentTimeMillis() - startMs

            logger.logEngineEvent("LLM", "response received", latencyMs)

            JulyResult.success(
                LlmResponse(
                    text = response.message.content,
                    tokenCount = response.eval_count,
                    latencyMs = latencyMs,
                    modelName = response.model
                )
            )

        } catch (e: HttpException) {
            val retryable = e.code() in listOf(503, 429, 500)
            JulyResult.failure(
                AppError.Llm(
                    message = "LLM HTTP error ${e.code()}: ${e.message()}",
                    cause = e,
                    retryable = retryable
                )
            )
        } catch (e: IOException) {
            JulyResult.failure(
                AppError.Network(
                    message = "Cannot reach LLM server at ${config.baseUrl}: ${e.message}",
                    cause = e
                )
            )
        } catch (e: Exception) {
            JulyResult.failure(
                AppError.Llm(
                    message = "LLM unexpected error: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        return networkHealthChecker.isReachable(config.host, config.port)
    }

    override suspend fun getModelInfo(): ModelInfo {
        return try {
            ModelInfo(name = config.modelName)
        } catch (e: Exception) {
            ModelInfo(name = config.modelName, version = "unknown")
        }
    }

    private fun buildMessageList(prompt: String, history: List<Message>): List<LlmMessage> {
        val messages = mutableListOf<LlmMessage>()
        messages.add(LlmMessage(role = "system", content = config.systemPrompt))
        history.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messages.add(LlmMessage(role = role, content = msg.content))
        }
        messages.add(LlmMessage(role = "user", content = prompt))
        return messages
    }
}
