package com.july.offline.ui.conversation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.july.offline.ui.conversation.EngineHealthUiState
import com.july.offline.ui.theme.EngineChipShape
import com.july.offline.ui.theme.JulyPalette

/**
 * Chips de estado de motores estilo terminal.
 * Solo visible cuando hay algún motor no disponible.
 * Formato: STT ✓ | LLM ✗ | TTS ✓
 */
@Composable
fun EngineHealthWidget(
    healthState: EngineHealthUiState,
    modifier: Modifier = Modifier
) {
    if (!healthState.showWarning) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("STT", healthState.sttReady, "stt"),
            Triple("LLM", healthState.llmReady, "llm"),
            Triple("TTS", healthState.ttsReady, "tts")
        ).forEach { (name, ready, _) ->
            EngineChip(name = name, ready = ready)
        }
    }
}

@Composable
private fun EngineChip(name: String, ready: Boolean) {
    val borderColor = if (ready) JulyPalette.Green600 else JulyPalette.Error
    val textColor = if (ready) JulyPalette.Green400 else JulyPalette.Error
    val bgColor = if (ready) JulyPalette.Green900 else JulyPalette.Error.copy(alpha = 0.1f)

    Surface(
        color = bgColor,
        shape = EngineChipShape,
        modifier = Modifier.border(
            width = 0.5.dp,
            color = borderColor,
            shape = EngineChipShape
        )
    ) {
        Text(
            text = "$name ${if (ready) "✓" else "✗"}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
