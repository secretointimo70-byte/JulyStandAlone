package com.july.offline.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Paleta base ────────────────────────────────────────────────────────────

object JulyPalette {

    // Verde terminal — acento principal
    val Green50  = Color(0xFFE8F5E9)
    val Green100 = Color(0xFFC8E6C9)
    val Green200 = Color(0xFF97C459)
    val Green400 = Color(0xFF39D353)   // ← acento principal
    val Green600 = Color(0xFF1A6B26)
    val Green800 = Color(0xFF0D3D14)
    val Green900 = Color(0xFF061F0A)

    // Neutros oscuros — superficies
    val Dark50  = Color(0xFF0A0E0A)   // fondo más oscuro
    val Dark100 = Color(0xFF0F140F)   // superficie primaria
    val Dark200 = Color(0xFF141A14)   // superficie secundaria
    val Dark300 = Color(0xFF1E2A1E)   // borde sutil
    val Dark400 = Color(0xFF2A3D2A)   // borde énfasis

    // Texto
    val TextPrimary   = Color(0xFFD4E8D4)   // texto principal
    val TextSecondary = Color(0xFF7A9E7A)   // texto secundario
    val TextTertiary  = Color(0xFF3D5E3D)   // texto muy sutil
    val TextMuted     = Color(0xFF1E2A1E)   // texto casi invisible

    // Azul eléctrico — mensajes del usuario
    val ElectricBlue     = Color(0xFF00B4FF)
    val ElectricBlueDim  = Color(0xFF0078AA)

    // Estados semánticos
    val Error   = Color(0xFFCF6679)
    val Warning = Color(0xFFBA7517)
    val Info    = Color(0xFF378ADD)
    val Success = Color(0xFF39D353)

    // Modo claro (para dispositivos que prefieren claro)
    val LightBackground = Color(0xFFF4F9F4)
    val LightSurface    = Color(0xFFFFFFFF)
    val LightBorder     = Color(0xFFD4E8D4)
    val LightTextPrimary = Color(0xFF0F2010)
}

// ── Color Schemes Material 3 ───────────────────────────────────────────────

val JulyDarkColorScheme = darkColorScheme(
    primary          = JulyPalette.Green400,
    onPrimary        = JulyPalette.Dark50,
    primaryContainer = JulyPalette.Green800,
    onPrimaryContainer = JulyPalette.Green100,

    secondary        = JulyPalette.Green200,
    onSecondary      = JulyPalette.Dark50,
    secondaryContainer = JulyPalette.Dark300,
    onSecondaryContainer = JulyPalette.TextSecondary,

    background       = JulyPalette.Dark50,
    onBackground     = JulyPalette.TextPrimary,

    surface          = JulyPalette.Dark100,
    onSurface        = JulyPalette.TextPrimary,
    surfaceVariant   = JulyPalette.Dark200,
    onSurfaceVariant = JulyPalette.TextSecondary,

    outline          = JulyPalette.Dark400,
    outlineVariant   = JulyPalette.Dark300,

    error            = JulyPalette.Error,
    onError          = JulyPalette.Dark50,
    errorContainer   = Color(0xFF4A1020),
    onErrorContainer = JulyPalette.Error,

    inverseSurface   = JulyPalette.TextPrimary,
    inverseOnSurface = JulyPalette.Dark50,
    inversePrimary   = JulyPalette.Green600,

    scrim            = Color(0x99000000)
)

val JulyLightColorScheme = lightColorScheme(
    primary          = JulyPalette.Green600,
    onPrimary        = Color.White,
    primaryContainer = JulyPalette.Green50,
    onPrimaryContainer = JulyPalette.Green800,

    secondary        = JulyPalette.Green800,
    onSecondary      = Color.White,
    secondaryContainer = JulyPalette.Green100,
    onSecondaryContainer = JulyPalette.Green800,

    background       = JulyPalette.LightBackground,
    onBackground     = JulyPalette.LightTextPrimary,

    surface          = JulyPalette.LightSurface,
    onSurface        = JulyPalette.LightTextPrimary,
    surfaceVariant   = JulyPalette.Green50,
    onSurfaceVariant = JulyPalette.Green800,

    outline          = JulyPalette.LightBorder,
    outlineVariant   = JulyPalette.Green100,

    error            = Color(0xFFB00020),
    onError          = Color.White
)
