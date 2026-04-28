package com.july.offline.ai.llm

import retrofit2.http.Body
import retrofit2.http.POST

/** Interfaz Retrofit para el endpoint de chat del servidor LLM local. */
interface LlmApiService {

    @POST("api/chat")
    suspend fun chat(@Body request: LlmChatRequest): LlmChatResponse
}

data class LlmChatRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val stream: Boolean = false,
    val options: LlmOptions = LlmOptions()
)

data class LlmMessage(
    val role: String,   // "system", "user", "assistant"
    val content: String
)

data class LlmOptions(
    val temperature: Float = 0.7f,
    val num_predict: Int = 512
)

data class LlmChatResponse(
    val model: String,
    val message: LlmMessage,
    val done: Boolean,
    val total_duration: Long = 0L,
    val eval_count: Int = 0
)
