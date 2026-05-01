package com.july.offline.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.ai.tts.TtsVoiceOption
import com.july.offline.core.memory.ModelMode
import com.july.offline.ui.permission.PermissionHandler
import com.july.offline.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = JulyPalette.Dark50,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JulyPalette.Dark100,
                    titleContentColor = JulyPalette.Green400
                ),
                title = {
                    Text(
                        text = "july / ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        color = JulyPalette.Green400
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", color = JulyPalette.TextSecondary,
                            style = MaterialTheme.typography.titleMedium)
                    }
                },
                modifier = Modifier.border(
                    0.5.dp,
                    JulyPalette.Dark300,
                    androidx.compose.ui.graphics.RectangleShape
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(JulyPalette.Dark50)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Permiso de Micrófono
            PermissionHandler(
                onPermissionGranted = { /* Ya tiene el permiso */ },
                onPermissionDenied = { /* Manejado internamente por el Handler */ }
            ) { requestPermission ->
                JulySettingRow(
                    label = "permiso de micrófono",
                    sublabel = "necesario para escuchar tus comandos"
                ) {
                    TextButton(onClick = requestPermission) {
                        Text(
                            text = "verificar",
                            color = JulyPalette.Green400,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            JulyDivider()

            // TTS toggle
            JulySettingRow(label = "síntesis de voz", sublabel = "motor de texto a voz del sistema") {
                JulySwitch(
                    checked = settings.ttsEnabled,
                    onCheckedChange = { viewModel.setTtsEnabled(it) }
                )
            }

            // Selector de voz (visible solo si hay voces disponibles)
            if (availableVoices.isNotEmpty()) {
                JulyDivider()
                JulyVoiceSection(
                    voices = availableVoices,
                    selectedVoiceName = settings.ttsVoiceName,
                    onSelect = { viewModel.selectVoice(it) }
                )
            }

            JulyDivider()

            // Motor de IA
            JulySettingSection(label = "motor de ia") {
                JulyChipGroup(
                    options = listOf("auto", "embebido", "servidor"),
                    selected = when (settings.llmMode) {
                        LlmMode.AUTO -> "auto"
                        LlmMode.EMBEDDED -> "embebido"
                        LlmMode.SERVER -> "servidor"
                    },
                    onSelect = { option ->
                        viewModel.setLlmMode(
                            when (option) {
                                "embebido" -> LlmMode.EMBEDDED
                                "servidor" -> LlmMode.SERVER
                                else -> LlmMode.AUTO
                            }
                        )
                    }
                )
            }

            JulyDivider()

            // Modo memoria
            JulySettingSection(label = "modo de memoria") {
                JulyChipGroup(
                    options = listOf("velocidad", "memoria"),
                    selected = when (settings.modelMode) {
                        ModelMode.SPEED -> "velocidad"
                        ModelMode.MEMORY -> "memoria"
                    },
                    onSelect = { option ->
                        viewModel.setModelMode(
                            if (option == "memoria") ModelMode.MEMORY else ModelMode.SPEED
                        )
                    }
                )
            }

            JulyDivider()

            JulyDivider()

            // Descargar modelos
            JulySettingRow(
                label = "descargar modelos",
                sublabel = "whisper · llama 1B · llama 3B"
            ) {
                TextButton(onClick = onNavigateToDownloads) {
                    Text(
                        text = "abrir →",
                        color = JulyPalette.Green400,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            JulyDivider()

            // Idioma (informativo)
            JulySettingRow(label = "idioma", sublabel = settings.language) {}

            JulyDivider()

            // Auditoría de seguridad
            JulySettingRow(
                label = "auditoría de seguridad",
                sublabel = "análisis de app · dispositivo · red local"
            ) {
                TextButton(onClick = onNavigateToSecurity) {
                    Text(
                        text = "abrir →",
                        color = JulyPalette.Error.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ── Subcomponentes de settings ────────────────────────────────────────────

@Composable
private fun JulySettingRow(
    label: String,
    sublabel: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = JulyPalette.TextPrimary
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = JulyPalette.TextTertiary
                )
            }
        }
        trailing()
    }
}

@Composable
private fun JulySettingSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = JulyPalette.TextPrimary
        )
        content()
    }
}

@Composable
private fun JulyChipGroup(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                color = if (isSelected) JulyPalette.Green800 else JulyPalette.Dark200,
                shape = EngineChipShape,
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 0.5.dp,
                        color = if (isSelected) JulyPalette.Green400 else JulyPalette.Dark400,
                        shape = EngineChipShape
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) JulyPalette.Green400 else JulyPalette.TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun JulySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = JulyPalette.Green400,
            checkedTrackColor = JulyPalette.Green800,
            uncheckedThumbColor = JulyPalette.TextTertiary,
            uncheckedTrackColor = JulyPalette.Dark300
        )
    )
}

@Composable
private fun JulyVoiceSection(
    voices: List<TtsVoiceOption>,
    selectedVoiceName: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "voz en español",
            style = MaterialTheme.typography.bodyMedium,
            color = JulyPalette.TextPrimary
        )
        Text(
            text = "★★★ alta calidad · ★★ buena · ★ estándar",
            style = MaterialTheme.typography.labelSmall,
            color = JulyPalette.TextTertiary
        )
        voices.forEach { voice ->
            val isSelected = voice.name == selectedVoiceName ||
                (selectedVoiceName.isBlank() && voice == voices.first())
            Surface(
                onClick = { onSelect(voice.name) },
                color = if (isSelected) JulyPalette.Green800 else JulyPalette.Dark200,
                shape = EngineChipShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        color = if (isSelected) JulyPalette.Green400 else JulyPalette.Dark400,
                        shape = EngineChipShape
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = voice.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) JulyPalette.Green400 else JulyPalette.TextSecondary
                    )
                    if (isSelected) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = JulyPalette.Green400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JulyDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(JulyPalette.Dark300)
    )
}
