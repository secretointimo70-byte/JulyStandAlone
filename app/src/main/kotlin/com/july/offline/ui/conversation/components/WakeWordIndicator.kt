package com.july.offline.ui.conversation.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.july.offline.ui.theme.JulyAnimations
import com.july.offline.ui.theme.JulyPalette

/**
 * Indicador de escucha pasiva de wake-word.
 * Punto que pulsa suavemente + texto monoespaciado.
 */
@Composable
fun WakeWordIndicator(modifier: Modifier = Modifier) {
    val scale by rememberPulseAnimation(min = 0.75f, max = 1.0f, label = "ww_pulse")

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(scale)
                .background(
                    color = JulyPalette.Green400.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "oye july...",
            style = MaterialTheme.typography.labelMedium,
            color = JulyPalette.TextTertiary
        )
    }
}

@Composable
private fun rememberPulseAnimation(
    min: Float,
    max: Float,
    label: String
): androidx.compose.runtime.State<Float> {
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = JulyAnimations.pulseSpec,
        label = label
    )
}
