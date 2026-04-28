package com.july.offline.ui.emergency

import com.july.offline.domain.model.EmergencyLevel
import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.SurvivalContent
import com.july.offline.domain.model.SurvivalStep
import com.july.offline.domain.model.ValidationAnswer
import com.july.offline.domain.model.ValidationQuestion
import com.july.offline.domain.model.ValidationResult

enum class EmergencyPhase { MENU, VALIDATING, STEPS }

data class EmergencyUiState(
    val phase: EmergencyPhase = EmergencyPhase.MENU,
    val activeCategory: SurvivalCategory? = null,
    val content: SurvivalContent? = null,
    val currentStep: SurvivalStep? = null,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val language: String = "es",
    val level: EmergencyLevel = EmergencyLevel.CRITICAL,
    // Validation
    val validationQuestions: List<ValidationQuestion> = emptyList(),
    val validationQuestionIndex: Int = 0,
    val validationAnswers: Map<String, ValidationAnswer> = emptyMap(),
    val validationResult: ValidationResult? = null,
    val showFullSteps: Boolean = false
) {
    val hasPrevious: Boolean get() = currentStepIndex > 0
    val hasNext: Boolean get() = currentStepIndex < totalSteps - 1

    val currentValidationQuestion: ValidationQuestion?
        get() = validationQuestions.getOrNull(validationQuestionIndex)

    val isLastValidationQuestion: Boolean
        get() = validationQuestionIndex >= validationQuestions.size - 1

    val showNegativeFallback: Boolean
        get() = phase == EmergencyPhase.STEPS
                && validationResult?.type == ValidationResult.Type.NEGATIVE
                && !showFullSteps
}
