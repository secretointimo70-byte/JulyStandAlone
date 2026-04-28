package com.july.offline.ui.conversation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.ui.conversation.components.*
import com.july.offline.ui.permission.PermissionHandler
import com.july.offline.ui.theme.*

@Composable
fun ConversationScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emergencyState by viewModel.emergencyState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(emergencyState) {
        if (emergencyState !is com.july.offline.domain.model.EmergencyState.Inactive) {
            onNavigateToEmergency()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = JulyPalette.Dark50,
        topBar = {
            JulyTopBar(
                uiState = uiState,
                onToggleWakeWord = { viewModel.onWakeWordToggled(it) },
                onSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Motor health chips (solo si hay problema)
            if (uiState.engineHealth.showWarning) {
                Spacer(Modifier.height(6.dp))
                EngineHealthWidget(healthState = uiState.engineHealth)
                Spacer(Modifier.height(6.dp))
            } else {
                Spacer(Modifier.height(4.dp))
            }

            // Historial de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = uiState.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                // Indicador de estado actual al final del historial
                item {
                    Spacer(Modifier.height(4.dp))
                    when (uiState.phase) {
                        ConversationPhase.WAKE_WORD_LISTENING ->
                            WakeWordIndicator()
                        ConversationPhase.LISTENING -> Column {
                            StatusBar(phase = uiState.phase)
                            Spacer(Modifier.height(6.dp))
                            WaveformIndicator()
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "escucha: ${uiState.vadVoiceSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JulyPalette.Green400
                                )
                                Text(
                                    text = "silencio: ${uiState.vadSilenceSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JulyPalette.TextTertiary
                                )
                                Text(
                                    text = "energía: ${uiState.vadEnergyLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JulyPalette.TextTertiary
                                )
                            }
                        }
                        ConversationPhase.TRANSCRIBING,
                        ConversationPhase.THINKING,
                        ConversationPhase.SPEAKING ->
                            StatusBar(phase = uiState.phase)
                        else -> {}
                    }
                }
            }

            // Mensaje de error
            uiState.errorMessage?.let { error ->
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = JulyPalette.Error
                    )
                }
            }

            // Línea divisora sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(JulyPalette.Dark300)
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(6.dp))

            // Botón Yuli Apocalíptica
            YuliApocalipticaButton(
                onClick = onNavigateToEmergency,
                modifier = Modifier.padding(horizontal = 0.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Controles con gestión de permiso
            PermissionHandler(
                onPermissionGranted = { viewModel.onMicPressed() },
                onPermissionDenied = { viewModel.onPermissionDenied() }
            ) { requestPermissionAndStart ->

                val isIdle = uiState.phase in listOf(
                    ConversationPhase.IDLE,
                    ConversationPhase.WAKE_WORD_LISTENING,
                    ConversationPhase.ERROR,
                    ConversationPhase.CANCELLED
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón cancelar (solo visible durante ciclo)
                    if (uiState.isCancelVisible) {
                        JulyCancelButton(
                            onClick = { viewModel.onCancelPressed() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Botón principal
                    JulyPrimaryButton(
                        phase = uiState.phase,
                        enabled = uiState.isMicButtonEnabled && isIdle,
                        onClick = { if (isIdle) requestPermissionAndStart() },
                        modifier = Modifier.weight(if (uiState.isCancelVisible) 2f else 1f)
                    )
                }
            }
        }
    }
}

// ── Subcomponentes internos ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JulyTopBar(
    uiState: ConversationUiState,
    onToggleWakeWord: (Boolean) -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = JulyPalette.Dark100,
            titleContentColor = JulyPalette.Green400
        ),
        title = {
            Text(
                text = "july",
                style = MaterialTheme.typography.titleLarge,
                color = JulyPalette.Green400
            )
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "oye",
                    style = MaterialTheme.typography.labelSmall,
                    color = JulyPalette.TextTertiary
                )
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = uiState.isWakeWordActive,
                    onCheckedChange = onToggleWakeWord,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JulyPalette.Green400,
                        checkedTrackColor = JulyPalette.Green800,
                        uncheckedThumbColor = JulyPalette.TextTertiary,
                        uncheckedTrackColor = JulyPalette.Dark300
                    ),
                    modifier = Modifier
                        .height(24.dp)
                        .width(44.dp)
                )
            }
            IconButton(onClick = onSettings) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.titleMedium,
                    color = JulyPalette.TextTertiary
                )
            }
        },
        modifier = Modifier.border(
            width = 0.5.dp,
            color = JulyPalette.Dark300,
            shape = androidx.compose.ui.graphics.RectangleShape
        )
    )
}

@Composable
private fun JulyPrimaryButton(
    phase: ConversationPhase,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (phase) {
        ConversationPhase.IDLE,
        ConversationPhase.CANCELLED -> "hablar"
        ConversationPhase.WAKE_WORD_LISTENING -> "hablar ahora"
        ConversationPhase.LISTENING -> "escuchando..."
        ConversationPhase.TRANSCRIBING -> "procesando..."
        ConversationPhase.THINKING -> "pensando..."
        ConversationPhase.SPEAKING -> "respondiendo..."
        ConversationPhase.ERROR -> "reintentar"
    }

    val borderColor by animateColorAsState(
        targetValue = if (enabled) JulyPalette.Green400 else JulyPalette.Dark400,
        animationSpec = tween(JulyAnimations.DURATION_STANDARD),
        label = "btn_border"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) JulyPalette.Green800 else JulyPalette.Dark200,
        shape = PrimaryButtonShape,
        modifier = modifier
            .height(44.dp)
            .border(width = 0.5.dp, color = borderColor, shape = PrimaryButtonShape)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) JulyPalette.Green400 else JulyPalette.TextTertiary,
                letterSpacing = 0.08.sp
            )
        }
    }
}

@Composable
private fun JulyCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = JulyPalette.Dark200,
        shape = PrimaryButtonShape,
        modifier = modifier
            .height(44.dp)
            .border(0.5.dp, JulyPalette.Dark400, PrimaryButtonShape)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = "cancelar",
                style = MaterialTheme.typography.labelLarge,
                color = JulyPalette.TextSecondary
            )
        }
    }
}
