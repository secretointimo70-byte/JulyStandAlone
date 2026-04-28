package com.july.offline.survival

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.SurvivalContent
import com.july.offline.domain.model.SurvivalStep
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SurvivalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurvivalService @Inject constructor(
    private val repository: SurvivalRepository,
    private val llmEngine: LanguageModelEngine,
    private val logger: DiagnosticsLogger
) {
    companion object {
        private const val EMERGENCY_SYSTEM_PROMPT =
            "Eres un asistente de supervivencia. Proporciona instrucciones claras y concretas. " +
            "Organiza tu respuesta en pasos numerados. Sé directo y sin rodeos."
    }

    suspend fun getContent(category: SurvivalCategory, language: String): SurvivalContent? {
        val local = repository.getContent(category, language)
        if (local != null) return local
        logger.logInfo("SurvivalService", "No local content for ${category.name}, trying LLM fallback")
        return generateWithLlm(category, language)
    }

    suspend fun listCategories(language: String): List<SurvivalContent> =
        repository.listCategories(language)

    private suspend fun generateWithLlm(category: SurvivalCategory, language: String): SurvivalContent? {
        if (!llmEngine.isAvailable()) return null
        val name = if (language == "es") category.displayNameEs else category.displayNameEn
        val prompt = if (language == "es")
            "Dame instrucciones de supervivencia para: $name. Formato: lista numerada de pasos."
        else
            "Give me survival instructions for: $name. Format: numbered list of steps."
        val systemMsg = Message(id = "emergency_system", role = MessageRole.SYSTEM, content = EMERGENCY_SYSTEM_PROMPT)
        return when (val result = llmEngine.generate(prompt, listOf(systemMsg))) {
            is JulyResult.Success -> parseLlmResponse(result.data.text, category, language)
            is JulyResult.Failure -> { logger.logError("SurvivalService", "LLM fallback failed"); null }
        }
    }

    private fun parseLlmResponse(text: String, category: SurvivalCategory, language: String): SurvivalContent {
        val steps = text.lines()
            .filter { it.matches(Regex("^\\d+\\..*")) }
            .mapIndexed { i, line ->
                val content = line.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                SurvivalStep(index = i + 1, title = content.take(60), description = content)
            }
        val title = if (language == "es") category.displayNameEs else category.displayNameEn
        return SurvivalContent(category = category, language = language, title = title,
            summary = title, steps = steps, source = "llm")
    }
}
