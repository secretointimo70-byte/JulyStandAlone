package com.july.offline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema principal de July.
 *
 * Oscuro por defecto. Dynamic Color desactivado para preservar identidad verde terminal.
 */
@Composable
fun JulyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JulyDarkColorScheme else JulyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JulyTypography,
        shapes = JulyShapes,
        content = content
    )
}

// ── Acceso a colores de la paleta desde Composables ───────────────────────

object JulyThemeColors {
    val greenAccent = JulyPalette.Green400
    val greenDim    = JulyPalette.Green600
    val greenMuted  = JulyPalette.Green800
    val textMono    = JulyPalette.TextSecondary
    val borderSubtle = JulyPalette.Dark300
    val borderEmphasis = JulyPalette.Dark400
}
