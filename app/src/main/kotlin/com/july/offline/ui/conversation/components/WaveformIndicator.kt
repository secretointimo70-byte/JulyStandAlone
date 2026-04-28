package com.july.offline.ui.conversation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.july.offline.ui.theme.JulyAnimations
import com.july.offline.ui.theme.JulyPalette

/**
 * Waveform de grabación activa.
 * Barras delgadas con alturas animadas escalonadas — estilo terminal precisos.
 */
@Composable
fun WaveformIndicator(modifier: Modifier = Modifier) {
    val barCount = 12
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val minHeight = if (index % 3 == 0) 4f else 6f
            val maxHeight = when (index % 4) {
                0 -> 20f
                1 -> 28f
                2 -> 16f
                else -> 24f
            }

            val height by infiniteTransition.animateFloat(
                initialValue = minHeight,
                targetValue = maxHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = JulyAnimations.DURATION_WAVE + index * 40,
                        easing = JulyAnimations.EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            val opacity = when {
                index < 2 || index >= barCount - 2 -> 0.3f
                index < 4 || index >= barCount - 4 -> 0.6f
                else -> 0.85f
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(
                        color = JulyPalette.Green400.copy(alpha = opacity),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}
