package com.july.offline.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.SurvivalContent
import com.july.offline.domain.model.ValidationAnswer
import com.july.offline.domain.model.ValidationResult
import com.july.offline.domain.orchestrator.EmergencyCoordinator
import com.july.offline.domain.state.EmergencyStateHolder
import com.july.offline.domain.validation.CategoryValidation
import com.july.offline.survival.SurvivalService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val emergencyStateHolder: EmergencyStateHolder,
    private val emergencyCoordinator: EmergencyCoordinator,
    private val survivalService: SurvivalService,
    private val preferencesDataStore: AppPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    // Contenido cargado una vez — navegación de pasos en memoria
    private var cachedContent: SurvivalContent? = null
    private var currentIndex = 0

    init {
        viewModelScope.launch {
            preferencesDataStore.language.collect { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }
    }

    fun onCategorySelected(category: SurvivalCategory) {
        cachedContent = null
        currentIndex = 0
        emergencyCoordinator.selectCategory(category)

        val lang   = _uiState.value.language
        val config = CategoryValidation.configFor(category)

        _uiState.value = _uiState.value.copy(
            phase                  = EmergencyPhase.VALIDATING,
            activeCategory         = category,
            isLoading              = true,
            validationQuestions    = config.questions,
            validationQuestionIndex = 0,
            validationAnswers      = emptyMap(),
            validationResult       = null,
            content                = null,
            currentStep            = null,
            currentStepIndex       = 0,
            totalSteps             = 0,
            errorMessage           = null,
            showFullSteps          = false
        )

        // Carga contenido en paralelo mientras el usuario responde
        viewModelScope.launch { preloadContent(category, lang) }
    }

    fun onValidationAnswer(questionId: String, answer: ValidationAnswer) {
        val state      = _uiState.value
        val newAnswers = state.validationAnswers + (questionId to answer)

        if (state.isLastValidationQuestion) {
            completeValidation(newAnswers)
        } else {
            _uiState.value = state.copy(
                validationAnswers       = newAnswers,
                validationQuestionIndex = state.validationQuestionIndex + 1
            )
        }
    }

    fun onSkipValidation() {
        completeValidation(emptyMap(), forceIncomplete = true)
    }

    fun onShowFullSteps() {
        _uiState.value = _uiState.value.copy(showFullSteps = true)
    }

    fun onBackToMenu() {
        cachedContent = null
        currentIndex  = 0
        emergencyCoordinator.clearCategory()
        _uiState.value = EmergencyUiState(language = _uiState.value.language)
    }

    fun onNextStep() {
        val content = cachedContent ?: return
        if (currentIndex < content.stepCount - 1) {
            currentIndex++
            emergencyCoordinator.advanceStep()
            pushStepToUi()
        }
    }

    fun onPreviousStep() {
        val content = cachedContent ?: return
        if (currentIndex > 0) {
            currentIndex--
            emergencyCoordinator.previousStep()
            pushStepToUi()
        }
    }

    fun onExitEmergency() {
        cachedContent = null
        currentIndex  = 0
        emergencyCoordinator.deactivate()
    }

    // ── privado ────────────────────────────────────────────────────────────

    private fun completeValidation(
        answers: Map<String, ValidationAnswer>,
        forceIncomplete: Boolean = false
    ) {
        val state    = _uiState.value
        val category = state.activeCategory ?: return
        val config   = CategoryValidation.configFor(category)

        val result = if (forceIncomplete) {
            ValidationResult(
                type     = ValidationResult.Type.INCOMPLETE,
                bannerEs = "Datos incompletos. Se muestran todos los pasos.",
                bannerEn = "Incomplete data. All steps shown."
            )
        } else {
            config.evaluate(answers)
        }

        val content = cachedContent
        if (content != null) {
            transitionToSteps(category, content, result)
        } else {
            // Contenido aún cargando — preloadContent lo recogerá al terminar
            _uiState.value = state.copy(
                validationResult  = result,
                validationAnswers = answers
            )
        }
    }

    private suspend fun preloadContent(category: SurvivalCategory, lang: String) {
        val content = survivalService.getContent(category, lang)
        if (content != null && content.stepCount > 0) {
            cachedContent = content
            val pendingResult = _uiState.value.validationResult
            if (pendingResult != null) {
                // Validación ya completada mientras cargaba
                transitionToSteps(category, content, pendingResult)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading    = false,
                errorMessage = if (lang == "es") "Contenido no disponible" else "Content unavailable"
            )
        }
    }

    private fun transitionToSteps(
        category: SurvivalCategory,
        content: SurvivalContent,
        result: ValidationResult
    ) {
        currentIndex = 0
        _uiState.value = _uiState.value.copy(
            phase            = EmergencyPhase.STEPS,
            activeCategory   = category,
            content          = content,
            currentStep      = content.stepAt(0),
            currentStepIndex = 0,
            totalSteps       = content.stepCount,
            isLoading        = false,
            validationResult = result
        )
    }

    private fun pushStepToUi() {
        val content = cachedContent ?: return
        _uiState.value = _uiState.value.copy(
            currentStep      = content.stepAt(currentIndex),
            currentStepIndex = currentIndex
        )
    }
}
