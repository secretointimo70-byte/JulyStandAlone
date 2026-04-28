package com.july.offline.ui.conversation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Amber900  = Color(0xFF6B3A00)
private val Amber700  = Color(0xFFB8680A)
private val Amber500  = Color(0xFFD4900A)
private val Amber300  = Color(0xFFE8B84A)
private val Dark900   = Color(0xFF0D0600)
private val Dark800   = Color(0xFF1A0C00)
private val Dark700   = Color(0xFF2A1400)
private val BtnShape  = RoundedCornerShape(6.dp)

@Composable
fun YuliApocalipticaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val buttonHeight = screenHeight / 8

    // Pulso sutil en el borde
    val pulse = rememberInfiniteTransition(label = "border_pulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .clip(BtnShape)
            .background(
                Brush.horizontalGradient(
                    0.0f to Dark900,
                    0.3f to Dark800,
                    0.7f to Dark700,
                    1.0f to Dark900
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Amber700.copy(alpha = borderAlpha),
                        Amber500.copy(alpha = borderAlpha),
                        Amber700.copy(alpha = borderAlpha)
                    )
                ),
                shape = BtnShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Amber500)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Franja horizontal decorativa de fondo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Amber700.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Ícono cerebro
            Text(
                text = "🧠",
                fontSize = (buttonHeight.value * 0.28f).sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // YULI
                Text(
                    text = "YULI",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = (buttonHeight.value * 0.32f).sp,
                    color = Amber300,
                    letterSpacing = 5.sp,
                    lineHeight = (buttonHeight.value * 0.34f).sp
                )
                // APOCALÍPTICA
                Text(
                    text = "APOCALÍPTICA",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (buttonHeight.value * 0.14f).sp,
                    color = Amber500,
                    letterSpacing = 3.sp,
                    lineHeight = (buttonHeight.value * 0.16f).sp
                )
                // Tagline
                Text(
                    text = "TU INTELIGENCIA PARA SOBREVIVIR",
                    fontFamily = FontFamily.Monospace,
                    fontSize = (buttonHeight.value * 0.09f).sp,
                    color = Amber700,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // Flecha derecha
            Text(
                text = "▶",
                fontSize = (buttonHeight.value * 0.18f).sp,
                color = Amber700.copy(alpha = borderAlpha)
            )
        }
    }
}
