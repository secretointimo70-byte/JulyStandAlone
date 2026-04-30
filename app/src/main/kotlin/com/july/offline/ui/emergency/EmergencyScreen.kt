package com.july.offline.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.model.ValidationAnswer
import com.july.offline.domain.model.ValidationResult
import com.july.offline.ui.theme.JulyPalette

private val EmergencyRed   = Color(0xFFE24B4A)
private val EmergencyAmber = Color(0xFFE8A838)
private val CategoryShape  = RoundedCornerShape(8.dp)

@Composable
fun EmergencyScreen(
    onExitEmergency: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JulyPalette.Dark50)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        EmergencyHeader(
            showBack = uiState.phase != EmergencyPhase.MENU,
            onBack   = { viewModel.onBackToMenu() },
            onExit   = { viewModel.onExitEmergency(); onExitEmergency() }
        )

        if (uiState.isLoading && uiState.phase == EmergencyPhase.MENU) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmergencyRed)
            }
        } else when (uiState.phase) {
            EmergencyPhase.MENU -> CategoryMenu(
                language           = uiState.language,
                onCategorySelected = { viewModel.onCategorySelected(it) }
            )
            EmergencyPhase.VALIDATING -> ValidationView(
                uiState   = uiState,
                onAnswer  = { id, ans -> viewModel.onValidationAnswer(id, ans) },
                onSkip    = { viewModel.onSkipValidation() }
            )
            EmergencyPhase.STEPS -> if (uiState.showNegativeFallback) {
                NegativeFallbackView(
                    uiState          = uiState,
                    onViewFullSteps  = { viewModel.onShowFullSteps() }
                )
            } else {
                StepView(
                    uiState    = uiState,
                    onNext     = { viewModel.onNextStep() },
                    onPrevious = { viewModel.onPreviousStep() }
                )
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun EmergencyHeader(showBack: Boolean, onBack: () -> Unit, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A0A0A))
            .border(
                width = 0.5.dp,
                color = EmergencyRed.copy(alpha = 0.4f),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            Text(
                text     = "←",
                color    = EmergencyRed,
                fontSize = 20.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp)
            )
        }
        Text(
            text      = "⚠ EMERGENCIA",
            style     = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color     = EmergencyRed,
            modifier  = Modifier.weight(1f)
        )
        Surface(
            onClick = onExit,
            color   = Color(0xFF3A0A0A),
            shape   = RoundedCornerShape(4.dp),
            modifier = Modifier.border(0.5.dp, EmergencyRed.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
        ) {
            Text(
                text     = "Salir",
                color    = EmergencyRed,
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ── Menú de categorías ────────────────────────────────────────────────────────

@Composable
private fun CategoryMenu(language: String, onCategorySelected: (SurvivalCategory) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text     = if (language == "es") "Selecciona una categoría" else "Select a category",
            style    = MaterialTheme.typography.labelLarge,
            color    = JulyPalette.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SurvivalCategory.entries.chunked(2).forEach { row ->
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cat ->
                    CategoryCard(
                        category = cat,
                        language = language,
                        modifier = Modifier.weight(1f),
                        onClick  = { onCategorySelected(cat) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: SurvivalCategory,
    language: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = when (category) {
        SurvivalCategory.WATER     -> "💧"
        SurvivalCategory.FIRE      -> "🔥"
        SurvivalCategory.FOOD      -> "🌿"
        SurvivalCategory.SHELTER   -> "⛺"
        SurvivalCategory.FIRST_AID -> "➕"
        SurvivalCategory.SECURITY  -> "🛡"
        SurvivalCategory.MENTAL_RESILIENCE -> "🧠"
    }
    Surface(
        onClick  = onClick,
        color    = JulyPalette.Dark100,
        shape    = CategoryShape,
        modifier = modifier.height(80.dp).border(0.5.dp, EmergencyAmber.copy(alpha = 0.4f), CategoryShape)
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text       = if (language == "es") category.displayNameEs else category.displayNameEn,
                style      = MaterialTheme.typography.labelMedium,
                color      = EmergencyAmber,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Vista de validación ───────────────────────────────────────────────────────

@Composable
private fun ValidationView(
    uiState: EmergencyUiState,
    onAnswer: (questionId: String, answer: ValidationAnswer) -> Unit,
    onSkip: () -> Unit
) {
    val question = uiState.currentValidationQuestion
    val es       = uiState.language == "es"
    val total    = uiState.validationQuestions.size
    val current  = uiState.validationQuestionIndex + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título de categoría
        uiState.activeCategory?.let { cat ->
            Text(
                text       = if (es) cat.displayNameEs else cat.displayNameEn,
                style      = MaterialTheme.typography.titleMedium,
                color      = EmergencyAmber,
                fontWeight = FontWeight.Bold
            )
        }

        // Indicador de progreso de validación
        Text(
            text  = if (es) "Pregunta $current de $total" else "Question $current of $total",
            style = MaterialTheme.typography.labelSmall,
            color = JulyPalette.TextTertiary
        )
        LinearProgressIndicator(
            progress     = { current.toFloat() / total },
            modifier     = Modifier.fillMaxWidth(),
            color        = EmergencyAmber.copy(alpha = 0.7f),
            trackColor   = JulyPalette.Dark300
        )

        // Tarjeta de pregunta
        if (question != null) {
            Surface(
                color    = JulyPalette.Dark100,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, EmergencyAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text     = if (es) question.textEs else question.textEn,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Color.White,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 22.sp
                )
            }

            // Botones Sí / No
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ValidationButton(
                    label    = if (es) "Sí" else "Yes",
                    color    = EmergencyAmber,
                    modifier = Modifier.weight(1f),
                    onClick  = { onAnswer(question.id, ValidationAnswer.YES) }
                )
                ValidationButton(
                    label    = if (es) "No" else "No",
                    color    = EmergencyRed,
                    modifier = Modifier.weight(1f),
                    onClick  = { onAnswer(question.id, ValidationAnswer.NO) }
                )
            }

            // Spinner pequeño si aún carga contenido
            if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color    = JulyPalette.TextTertiary,
                        strokeWidth = 1.5.dp
                    )
                    Text(
                        text  = if (es) "Cargando contenido…" else "Loading content…",
                        style = MaterialTheme.typography.labelSmall,
                        color = JulyPalette.TextTertiary
                    )
                }
            }
        }

        // Omitir validación
        TextButton(
            onClick  = onSkip,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text  = if (es) "Omitir → ver pasos directamente" else "Skip → go directly to steps",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.TextTertiary
            )
        }
    }
}

@Composable
private fun ValidationButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        color    = color.copy(alpha = 0.12f),
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier.height(48.dp).border(0.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = label, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Vista de fallback negativo ────────────────────────────────────────────────

@Composable
private fun NegativeFallbackView(uiState: EmergencyUiState, onViewFullSteps: () -> Unit) {
    val result = uiState.validationResult ?: return
    val es     = uiState.language == "es"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner de alerta
        Surface(
            color    = EmergencyRed.copy(alpha = 0.15f),
            shape    = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmergencyRed.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
        ) {
            Text(
                text       = if (es) result.bannerEs.orEmpty() else result.bannerEn.orEmpty(),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = EmergencyRed,
                modifier   = Modifier.padding(14.dp)
            )
        }

        // Acciones de fallback
        val actions = if (es) result.fallbackActionsEs else result.fallbackActionsEn
        actions.forEachIndexed { idx, action ->
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text      = "${idx + 1}.",
                    color     = EmergencyRed,
                    fontWeight = FontWeight.Bold,
                    style     = MaterialTheme.typography.bodyMedium,
                    modifier  = Modifier.width(24.dp)
                )
                Text(
                    text       = action,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = JulyPalette.TextPrimary,
                    lineHeight = 22.sp,
                    modifier   = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Botón para ver protocolo completo
        Surface(
            onClick  = onViewFullSteps,
            color    = JulyPalette.Dark200,
            shape    = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(0.5.dp, JulyPalette.Dark400, RoundedCornerShape(8.dp))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text  = if (es) "Ver protocolo completo →" else "View full protocol →",
                    style = MaterialTheme.typography.labelLarge,
                    color = JulyPalette.TextSecondary
                )
            }
        }
    }
}

// ── Vista de pasos ────────────────────────────────────────────────────────────

@Composable
private fun StepView(uiState: EmergencyUiState, onNext: () -> Unit, onPrevious: () -> Unit) {
    val step    = uiState.currentStep
    val content = uiState.content
    val es      = uiState.language == "es"
    val result  = uiState.validationResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        content?.let {
            Text(
                text       = it.title,
                style      = MaterialTheme.typography.titleSmall,
                color      = EmergencyAmber,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = if (es) "Paso ${uiState.currentStepIndex + 1} de ${uiState.totalSteps}"
                        else    "Step ${uiState.currentStepIndex + 1} of ${uiState.totalSteps}",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.TextTertiary
            )
            LinearProgressIndicator(
                progress   = { (uiState.currentStepIndex + 1).toFloat() / uiState.totalSteps },
                modifier   = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color      = EmergencyAmber,
                trackColor = JulyPalette.Dark300
            )
        }

        // Banner de resultado de validación (PARTIAL / INCOMPLETE)
        if (result != null && result.type != ValidationResult.Type.POSITIVE) {
            val bannerColor = when (result.type) {
                ValidationResult.Type.NEGATIVE   -> EmergencyRed
                ValidationResult.Type.PARTIAL    -> EmergencyAmber
                ValidationResult.Type.INCOMPLETE -> EmergencyAmber.copy(alpha = 0.7f)
                else                             -> EmergencyAmber
            }
            Surface(
                color    = bannerColor.copy(alpha = 0.1f),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, bannerColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text     = if (es) result.bannerEs.orEmpty() else result.bannerEn.orEmpty(),
                    style    = MaterialTheme.typography.labelMedium,
                    color    = bannerColor,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Tarjeta del paso
        step?.let { s ->
            Surface(
                color    = JulyPalette.Dark100,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, EmergencyAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = s.title,
                        style      = MaterialTheme.typography.titleSmall,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = s.description,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = JulyPalette.TextSecondary,
                        lineHeight = 22.sp
                    )
                    s.warningNote?.let { warning ->
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color    = EmergencyRed.copy(alpha = 0.1f),
                            shape    = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, EmergencyRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text     = "⚠ $warning",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = EmergencyRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        uiState.errorMessage?.let { err ->
            Text(text = err, color = EmergencyRed, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
        }

        // Navegación
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.hasPrevious) {
                Surface(
                    onClick  = onPrevious,
                    color    = JulyPalette.Dark200,
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f).height(44.dp)
                        .border(0.5.dp, JulyPalette.Dark400, RoundedCornerShape(8.dp))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = if (es) "← Anterior" else "← Previous",
                            color = JulyPalette.TextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            if (uiState.hasNext) {
                Surface(
                    onClick  = onNext,
                    color    = Color(0xFF2A1A00),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f).height(44.dp)
                        .border(0.5.dp, EmergencyAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = if (es) "Siguiente →" else "Next →",
                            color = EmergencyAmber,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
