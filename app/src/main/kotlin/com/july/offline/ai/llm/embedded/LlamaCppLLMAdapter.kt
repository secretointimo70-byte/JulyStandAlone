package com.july.offline.ai.llm.embedded

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.EngineType
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.OomHandler
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject

/**
 * Adaptador LLM que usa llama.cpp via JNI.
 * Implementa LanguageModelEngine — el contrato es idéntico al servidor local.
 *
 * FASE 8: Integra OomHandler para captura de OutOfMemoryError durante JNI.
 * Llama 3.2 usa el formato ChatML / Instruct con tokens <|system|>, <|user|>, <|assistant|>.
 */
class LlamaCppLLMAdapter @Inject constructor(
    private val config: LlamaCppConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val oomHandler: OomHandler,
    private val logger: DiagnosticsLogger
) : LanguageModelEngine {

    @Volatile private var contextHandle: Long = 0L

    init {
        modelMemoryManager.registerLlmReleaseCallback { releaseContext() }
    }

    private suspend fun ensureLoaded(): Boolean {
        if (contextHandle != 0L) return true

        return modelMemoryManager.llmMutex.withLock {
            if (contextHandle != 0L) return@withLock true

            if (!File(config.modelPath).exists()) {
                logger.logError("LlamaCpp", "Model not found: ${config.modelPath}")
                return@withLock false
            }

            modelMemoryManager.notifyModelLoading(EngineType.LLM)
            logger.logInfo(
                "LlamaCpp",
                "Loading model ${config.modelPath} (ctx=${config.contextSize}, threads=${config.threads})..."
            )

            val startMs = System.currentTimeMillis()
            val handle = LlamaCppJNI.llamaInit(
                modelPath = config.modelPath,
                contextSize = config.contextSize,
                threads = config.threads,
                gpuLayers = config.gpuLayers
            )
            val loadMs = System.currentTimeMillis() - startMs

            return@withLock if (handle != 0L) {
                contextHandle = handle
                modelMemoryManager.notifyModelLoaded(EngineType.LLM)
                logger.logInfo("LlamaCpp", "Model loaded in ${loadMs}ms")
                true
            } else {
                logger.logError("LlamaCpp", "llamaInit() returned 0 — load failed")
                false
            }
        }
    }

    suspend fun releaseContext() {
        modelMemoryManager.llmMutex.withLock {
            if (contextHandle != 0L) {
                LlamaCppJNI.llamaFree(contextHandle)
                contextHandle = 0L
                modelMemoryManager.notifyModelReleased(EngineType.LLM)
                logger.logInfo("LlamaCpp", "Context released")
            }
        }
    }

    override suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {
        return try {
            withTimeout(config.inferenceTimeoutMs) {
                if (!ensureLoaded()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Llm(
                            message = "LlamaCpp model not available at ${config.modelPath}",
                            retryable = false
                        )
                    )
                }

                val fullPrompt = buildPrompt(prompt, history)

                val tokenCount = LlamaCppJNI.llamaTokenize(contextHandle, fullPrompt)
                if (tokenCount > config.contextSize - config.maxTokens) {
                    logger.logWarning(
                        "LlamaCpp",
                        "Prompt too long ($tokenCount tokens), truncating history"
                    )
                    val truncatedHistory = history.takeLast(4)
                    return@withTimeout generate(prompt, truncatedHistory)
                }

                // Liberar STT antes de inferencia — evita OOM kill (STT+LLM = ~1.2GB)
                modelMemoryManager.releaseSttForInference()

                logger.logInfo("LlamaCpp", "Generating ($tokenCount prompt tokens)...")
                val startMs = System.currentTimeMillis()

                val rawText = oomHandler.safeJniCall(
                    engine = "LlamaCpp",
                    onOom = { "" }
                ) {
                    LlamaCppJNI.llamaGenerate(
                        contextHandle = contextHandle,
                        prompt = fullPrompt,
                        maxTokens = config.maxTokens,
                        temperature = config.temperature,
                        topP = config.topP,
                        repeatPenalty = config.repeatPenalty
                    )
                }

                val latencyMs = System.currentTimeMillis() - startMs
                logger.logEngineEvent("LLM_EMBEDDED", "generated", latencyMs)

                if (rawText.isBlank()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Llm("LlamaCpp returned empty response", retryable = false)
                    )
                }

                JulyResult.success(
                    LlmResponse(
                        text = rawText.trim(),
                        tokenCount = -1,
                        latencyMs = latencyMs,
                        modelName = "llama.cpp:${File(config.modelPath).name}"
                    )
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            JulyResult.failure(
                AppError.Llm(
                    message = "LlamaCpp timeout after ${config.inferenceTimeoutMs}ms",
                    cause = e,
                    retryable = false
                )
            )
        } catch (e: Exception) {
            logger.logError("LlamaCpp", "Generation failed", e)
            JulyResult.failure(
                AppError.Llm(
                    message = "LlamaCpp error: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        return ensureLoaded()
    }

    override suspend fun getModelInfo(): ModelInfo {
        if (contextHandle == 0L) return ModelInfo(name = "llama.cpp (not loaded)")
        return try {
            val infoJson = LlamaCppJNI.llamaModelInfo(contextHandle)
            val name = infoJson
                .substringAfter("\"name\":\"")
                .substringBefore("\"")
            ModelInfo(name = name)
        } catch (e: Exception) {
            ModelInfo(name = "llama.cpp")
        }
    }

    /**
     * Construye el prompt completo en formato ChatML (Llama 3.x Instruct).
     */
    private fun buildPrompt(prompt: String, history: List<Message>): String {
        val sb = StringBuilder()

        sb.append("<|system|>\n")
        sb.append(config.systemPrompt)
        sb.append("\n<|end|>\n")

        history.takeLast(10).forEach { msg ->
            when (msg.role) {
                MessageRole.USER -> {
                    sb.append("<|user|>\n")
                    sb.append(msg.content)
                    sb.append("\n<|end|>\n")
                }
                MessageRole.ASSISTANT -> {
                    sb.append("<|assistant|>\n")
                    sb.append(msg.content)
                    sb.append("\n<|end|>\n")
                }
                MessageRole.SYSTEM -> { /* ignorado */ }
            }
        }

        sb.append("<|user|>\n")
        sb.append(prompt)
        sb.append("\n<|end|>\n")
        sb.append("<|assistant|>\n")

        return sb.toString()
    }
}
