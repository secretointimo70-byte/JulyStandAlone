package com.july.offline.ai.llm

/**
 * Configuración del servidor LLM local.
 * Compatible con la API de Ollama y LM Studio.
 */
data class LlmServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 11434,
    val modelName: String = "llama3.2:3b",
    val connectTimeoutSeconds: Long = 5L,
    val readTimeoutSeconds: Long = 60L,
    val systemPrompt: String = "Eres July, un asistente de voz offline. Responde de forma concisa y natural."
) {
    val baseUrl: String get() = "http://$host:$port/"
}
