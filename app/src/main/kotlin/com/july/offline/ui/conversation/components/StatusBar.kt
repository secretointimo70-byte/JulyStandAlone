package com.july.offline.ui.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.july.offline.ui.conversation.ConversationPhase
import com.july.offline.ui.theme.JulyAnimations
import com.july.offline.ui.theme.JulyPalette

/**
 * Indicador de estado del sistema en fuente monoespaciada.
 * El color anima suavemente entre estados.
 */
@Composable
fun StatusBar(phase: ConversationPhase, modifier: Modifier = Modifier) {

    val label = when (phase) {
        ConversationPhase.IDLE               -> "idle"
        ConversationPhase.WAKE_WORD_LISTENING -> "oye july..."
        ConversationPhase.LISTENING          -> "escuchando"
        ConversationPhase.TRANSCRIBING       -> "transcribiendo"
        ConversationPhase.THINKING           -> "pensando"
        ConversationPhase.SPEAKING           -> "respondiendo"
        ConversationPhase.ERROR              -> "error"
        ConversationPhase.CANCELLED          -> "cancelado"
    }

    val targetColor = when (phase) {
        ConversationPhase.IDLE               -> JulyPalette.TextTertiary
        ConversationPhase.WAKE_WORD_LISTENING -> JulyPalette.Green600
        ConversationPhase.LISTENING          -> JulyPalette.Green400
        ConversationPhase.TRANSCRIBING       -> JulyPalette.Green200
        ConversationPhase.THINKING           -> JulyPalette.TextSecondary
        ConversationPhase.SPEAKING           -> JulyPalette.Green400
        ConversationPhase.ERROR              -> JulyPalette.Error
        ConversationPhase.CANCELLED          -> JulyPalette.TextTertiary
    }

    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(JulyAnimations.DURATION_STATE),
        label = "status_color"
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "> ",
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.5f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
