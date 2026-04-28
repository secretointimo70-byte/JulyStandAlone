package com.july.offline.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.core.memory.ModelMode
import com.july.offline.ui.permission.PermissionHandler
import com.july.offline.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
                .background(JulyPalette.Dark50),
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
            JulySettingRow(label = "síntesis de voz", sublabel = "Piper · es_ES-sharvard") {
                JulySwitch(
                    checked = settings.ttsEnabled,
                    onCheckedChange = { viewModel.setTtsEnabled(it) }
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
private fun JulyDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(JulyPalette.Dark300)
    )
}
