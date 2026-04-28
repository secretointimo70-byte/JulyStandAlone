package com.july.offline.settings

import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.core.memory.ModelMode

data class AppSettings(
    val language: String = "es",
    val ttsEnabled: Boolean = true,
    val showTranscript: Boolean = true,
    val modelMode: ModelMode = ModelMode.SPEED,
    val llmMode: LlmMode = LlmMode.AUTO
)
