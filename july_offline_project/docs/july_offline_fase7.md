# JULY OFFLINE — FASE 7
## Design System Completo · Tema Oscuro Verde Terminal · Animaciones Sutiles
### `com.july.offline`

**Alcance FASE 7:**
- Design System completo: paleta de colores, tipografía, tokens de espaciado
- `JulyTheme.kt` — MaterialTheme customizado oscuro/claro
- `JulyColors.kt` — paleta verde terminal + modo sistema
- `JulyTypography.kt` — JetBrains Mono (monoespaciada) + Inter (sans)
- `JulyShapes.kt` — geometría precisa, bordes finos
- `JulyAnimations.kt` — especificaciones de animación reutilizables
- Componentes UI refactorizados con el Design System
- `ConversationScreen` completa con el nuevo visual
- `SettingsScreen` con el nuevo visual
- `WaveformIndicator` animada con el estilo terminal
- `WakeWordIndicator` con pulso sutil
- `EngineHealthWidget` con chips estilo terminal
- `MessageBubble` diferenciado usuario/asistente
- `StatusBar` con fuente monoespaciada
- `res/font/` — fuentes embebidas
- `res/values/themes.xml` — tema base para el sistema de vistas (splash screen)
- `AndroidManifest.xml` — configuración de tema

---

## ÍNDICE DE ARCHIVOS FASE 7

### NUEVOS — UI/THEME
- `ui/theme/JulyColors.kt`
- `ui/theme/JulyTypography.kt`
- `ui/theme/JulyShapes.kt`
- `ui/theme/JulyAnimations.kt`
- `ui/theme/JulyTheme.kt`

### MODIFICADOS — UI/COMPONENTS
- `ui/conversation/ConversationScreen.kt`
- `ui/conversation/components/WaveformIndicator.kt`
- `ui/conversation/components/WakeWordIndicator.kt`
- `ui/conversation/components/EngineHealthWidget.kt`
- `ui/conversation/components/MessageBubble.kt`
- `ui/conversation/components/StatusBar.kt`
- `ui/settings/SettingsScreen.kt`

### NUEVOS — RECURSOS
- `res/font/jetbrains_mono_regular.ttf` (descargar — instrucciones abajo)
- `res/font/jetbrains_mono_medium.ttf`
- `res/font/inter_regular.ttf`
- `res/font/inter_medium.ttf`
- `res/values/themes.xml`
- `res/values-night/themes.xml`

### MODIFICADOS — APP
- `MainActivity.kt` (aplica JulyTheme)

---

## DECISIONES DE DISEÑO

**Paleta verde terminal:**
El verde `#39D353` es el color de acento principal — el mismo verde que
GitHub usa para los commits en el gráfico de actividad. Es reconocible,
tecnológico y tiene excelente contraste sobre fondo oscuro (ratio 7.2:1).

**JetBrains Mono como fuente monoespaciada:**
Usada para elementos de sistema: estado del motor, timestamps, labels de
estado, identificadores. Transmite precisión técnica. Disponible gratis
en Google Fonts y JetBrains.

**Inter como fuente sans-serif:**
Usada para contenido conversacional: burbujas de mensaje, descripciones
en settings. Máxima legibilidad en pantallas pequeñas.

**Geometría precisa:**
Esquinas con radio mínimo (4dp para chips, 8dp para tarjetas, 12dp para
burbujas). Sin sombras. Bordes de 0.5dp. El espacio y la línea hacen
todo el trabajo visual.

**Animaciones sutiles:**
Duración estándar 200ms con `EaseOut`. Transiciones de estado 300ms.
Pulso del wake-word 1200ms. Waveform con delays escalonados.
Sin rebotes ni springs llamativos.

---

## FUENTES — INSTRUCCIONES DE DESCARGA

```bash
# Crear directorio de fuentes
mkdir -p app/src/main/res/font

# JetBrains Mono — descargar desde Google Fonts
# https://fonts.google.com/specimen/JetBrains+Mono
# O directamente:
curl -L "https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip" \
  -o /tmp/jbmono.zip
unzip -o /tmp/jbmono.zip -d /tmp/jbmono
cp "/tmp/jbmono/fonts/ttf/JetBrainsMono-Regular.ttf" \
  app/src/main/res/font/jetbrains_mono_regular.ttf
cp "/tmp/jbmono/fonts/ttf/JetBrainsMono-Medium.ttf" \
  app/src/main/res/font/jetbrains_mono_medium.ttf

# Inter — descargar desde Google Fonts
# https://fonts.google.com/specimen/Inter
curl -L "https://github.com/rsms/inter/releases/download/v4.0/Inter-4.0.zip" \
  -o /tmp/inter.zip
unzip -o /tmp/inter.zip -d /tmp/inter
cp "/tmp/inter/Inter Desktop/Inter-Regular.otf" \
  app/src/main/res/font/inter_regular.ttf
cp "/tmp/inter/Inter Desktop/Inter-Medium.otf" \
  app/src/main/res/font/inter_medium.ttf

echo "Fuentes instaladas correctamente."
```

**Alternativa sin descarga:** usar fuentes del sistema con fallback:
```kotlin
// En JulyTypography.kt, reemplazar FontFamily.SansSerif por:
FontFamily(Font(R.font.inter_regular))
// Si el archivo no existe, Compose usa el fallback del sistema automáticamente
```

---

## DESIGN SYSTEM

### `ui/theme/JulyColors.kt`

```kotlin
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
```

---

### `ui/theme/JulyTypography.kt`

```kotlin
package com.july.offline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.july.offline.R

// ── Familias tipográficas ──────────────────────────────────────────────────

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

val InterSans = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium)
)

// ── Typography Material 3 ──────────────────────────────────────────────────

val JulyTypography = Typography(

    // Títulos grandes — monoespaciada para identidad del sistema
    displayLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    ),

    // Headings — monoespaciada
    headlineLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),

    // Títulos de sección — monoespaciada pequeña
    titleLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.08.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 18.sp
    ),

    // Cuerpo — Inter para legibilidad conversacional
    bodyLarge = TextStyle(
        fontFamily = InterSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    ),

    // Labels — monoespaciada para metadatos y estados
    labelLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.06.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.08.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 14.sp
    )
)
```

---

### `ui/theme/JulyShapes.kt`

```kotlin
package com.july.offline.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Geometría del Design System July.
 * Esquinas precisas y conservadoras — el contenido, no la forma, es el protagonista.
 */
val JulyShapes = Shapes(
    // Chips, badges, labels pequeños
    extraSmall = RoundedCornerShape(4.dp),
    // Botones, inputs, chips medianos
    small = RoundedCornerShape(6.dp),
    // Tarjetas pequeñas, contenedores de estado
    medium = RoundedCornerShape(8.dp),
    // Tarjetas principales, hojas de settings
    large = RoundedCornerShape(12.dp),
    // Burbujas de mensaje, bottom sheets
    extraLarge = RoundedCornerShape(16.dp)
)

// Shapes adicionales para casos específicos

/** Burbuja de mensaje del usuario — esquina inferior derecha aguda. */
val UserMessageShape = RoundedCornerShape(
    topStart = 12.dp, topEnd = 12.dp,
    bottomStart = 12.dp, bottomEnd = 3.dp
)

/** Burbuja de mensaje del asistente — esquina inferior izquierda aguda. */
val AssistantMessageShape = RoundedCornerShape(
    topStart = 12.dp, topEnd = 12.dp,
    bottomStart = 3.dp, bottomEnd = 12.dp
)

/** Chip de motor (STT/LLM/TTS) — rectángulo con esquinas mínimas. */
val EngineChipShape = RoundedCornerShape(4.dp)

/** Botón primario de grabación. */
val PrimaryButtonShape = RoundedCornerShape(8.dp)
```

---

### `ui/theme/JulyAnimations.kt`

```kotlin
package com.july.offline.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.IntOffset

/**
 * Especificaciones de animación del Design System July.
 * Todas las duraciones y curvas están centralizadas aquí.
 * Cambiar un valor aquí afecta toda la app de forma consistente.
 */
object JulyAnimations {

    // ── Duraciones (ms) ────────────────────────────────────────────────

    /** Transiciones de estado rápidas: aparición de chips, cambio de color. */
    const val DURATION_FAST = 150

    /** Transiciones de UI estándar: cambio de pantalla, aparición de elementos. */
    const val DURATION_STANDARD = 200

    /** Transiciones de estado conversacional: IDLE → LISTENING, etc. */
    const val DURATION_STATE = 300

    /** Animaciones de carga: pulso de wake-word, indicador de pensamiento. */
    const val DURATION_PULSE = 1200

    /** Waveform: ciclo de cada barra. */
    const val DURATION_WAVE = 400

    // ── Curvas de easing ──────────────────────────────────────────────

    /** Easing estándar para la mayoría de transiciones. */
    val EaseOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Easing para elementos que entran en pantalla. */
    val EaseInOut: Easing = FastOutSlowInEasing

    /** Easing lineal para animaciones continuas (waveform, pulso). */
    val Linear: Easing = LinearEasing

    // ── AnimationSpec factory functions ───────────────────────────────

    /** Spec estándar para transiciones de estado. */
    fun <T> stateTransition(): FiniteAnimationSpec<T> =
        tween(durationMillis = DURATION_STATE, easing = EaseOut)

    /** Spec rápida para micro-interacciones. */
    fun <T> fast(): FiniteAnimationSpec<T> =
        tween(durationMillis = DURATION_FAST, easing = EaseOut)

    /** Spec estándar para transiciones UI. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        tween(durationMillis = DURATION_STANDARD, easing = EaseOut)

    /** Spec para waveform — cada barra con delay escalonado. */
    fun waveBar(delayMs: Int): FiniteAnimationSpec<Float> = tween(
        durationMillis = DURATION_WAVE + delayMs * 50,
        easing = EaseInOut
    )

    /** Spec de pulso suave para wake-word indicator. */
    val pulseSpec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = DURATION_PULSE, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse
    )
}

/** Produce un Float animado de pulso entre [min] y [max]. */
@Composable
fun rememberPulseAnimation(
    min: Float = 0.7f,
    max: Float = 1.0f,
    label: String = "pulse"
): State<Float> {
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = JulyAnimations.pulseSpec,
        label = label
    )
}
```

---

### `ui/theme/JulyTheme.kt`

```kotlin
package com.july.offline.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tema principal de July.
 *
 * Estrategia de tema:
 * - Oscuro por defecto (isDefaultDark = true)
 * - El usuario puede forzar claro/oscuro desde Settings (FASE 8)
 * - En FASE 7, sigue la preferencia del sistema pero con sesgo hacia oscuro
 *
 * Dynamic Color (Material You) desactivado:
 * July tiene una identidad visual específica (verde terminal) que no debe
 * adaptarse al fondo de pantalla del usuario. Dynamic Color quebraría
 * la coherencia visual.
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

/**
 * Extensiones para acceder a los colores específicos de July
 * que no están en MaterialTheme.colorScheme estándar.
 */
object JulyThemeColors {
    val greenAccent = JulyPalette.Green400
    val greenDim    = JulyPalette.Green600
    val greenMuted  = JulyPalette.Green800
    val textMono    = JulyPalette.TextSecondary
    val borderSubtle = JulyPalette.Dark300
    val borderEmphasis = JulyPalette.Dark400
}
```

---

## RECURSOS ANDROID

### `res/values/themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Tema base para el splash screen y actividad principal -->
    <!-- Compose gestiona el tema internamente via JulyTheme -->
    <style name="Theme.JulyOffline" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/july_background</item>
        <item name="android:statusBarColor">@color/july_background</item>
        <item name="android:navigationBarColor">@color/july_background</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowLightNavigationBar">false</item>
    </style>

    <!-- Tema para el Splash Screen (Android 12+) -->
    <style name="Theme.JulyOffline.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/july_background</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_july_splash</item>
        <item name="postSplashScreenTheme">@style/Theme.JulyOffline</item>
    </style>
</resources>
```

### `res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="july_background">#0A0E0A</color>
    <color name="july_surface">#0F140F</color>
    <color name="july_accent">#39D353</color>
</resources>
```

### `res/values-night/themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Mismo tema — July es oscuro por defecto -->
    <style name="Theme.JulyOffline" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/july_background</item>
        <item name="android:statusBarColor">@color/july_background</item>
        <item name="android:navigationBarColor">@color/july_background</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowLightNavigationBar">false</item>
    </style>
</resources>
```

### `res/drawable/ic_july_splash.xml` (icono vectorial para splash)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- J simplificada en verde terminal -->
    <path
        android:fillColor="#39D353"
        android:pathData="M58,24 L66,24 L66,72 Q66,84 54,84 Q42,84 42,72 L42,66 L50,66 L50,72 Q50,76 54,76 Q58,76 58,72 Z"/>
</vector>
```

---

## ANDROIDMANIFEST.XML — ACTUALIZACIÓN

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <uses-feature
        android:name="android.hardware.microphone"
        android:required="true" />

    <application
        android:name=".JulyApplication"
        android:allowBackup="false"
        android:label="July"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="false"
        android:theme="@style/Theme.JulyOffline">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

---

## MAINACTIVITY — ACTUALIZACIÓN

### `MainActivity.kt`

```kotlin
package com.july.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.july.offline.navigation.JulyNavGraph
import com.july.offline.ui.theme.JulyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JulyTheme {
                val navController = rememberNavController()
                JulyNavGraph(navController = navController)
            }
        }
    }
}
```

---

## COMPONENTES ACTUALIZADOS

### `ui/conversation/components/StatusBar.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        // Prefijo cursor estilo terminal
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
```

---

### `ui/conversation/components/WaveformIndicator.kt`

```kotlin
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
import androidx.compose.material3.MaterialTheme
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
 * Paleta: verde con opacidad variable por barra.
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
```

---

### `ui/conversation/components/WakeWordIndicator.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Pulso sutil: escala 0.8 → 1.0, sin llamar la atención.
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

// Import necesario desde JulyAnimations.kt
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
```

---

### `ui/conversation/components/EngineHealthWidget.kt`

```kotlin
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
```

---

### `ui/conversation/components/MessageBubble.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.july.offline.ui.conversation.MessageUiModel
import com.july.offline.ui.theme.AssistantMessageShape
import com.july.offline.ui.theme.JulyPalette
import com.july.offline.ui.theme.UserMessageShape

/**
 * Burbuja de mensaje con el Design System de July.
 *
 * Usuario: fondo verde oscuro, borde verde, esquina inferior derecha aguda
 * Asistente: fondo superficie, borde sutil, esquina inferior izquierda aguda
 * Timestamp en fuente monoespaciada pequeña
 */
@Composable
fun MessageBubble(message: MessageUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            val bgColor = if (message.isUser)
                JulyPalette.Green800
            else
                JulyPalette.Dark200

            val borderColor = if (message.isUser)
                JulyPalette.Green600
            else
                JulyPalette.Dark400

            val textColor = if (message.isUser)
                JulyPalette.TextPrimary
            else
                JulyPalette.TextPrimary

            val shape = if (message.isUser) UserMessageShape else AssistantMessageShape

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .background(color = bgColor, shape = shape)
                    .then(
                        Modifier.padding(
                            start = if (!message.isUser) 0.5.dp else 0.dp,
                            end = if (message.isUser) 0.5.dp else 0.dp,
                            top = 0.5.dp,
                            bottom = 0.5.dp
                        )
                    )
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .background(color = bgColor, shape = shape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = "${message.timestamp} · ${if (message.isUser) "tú" else "july"}",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.TextTertiary
            )
        }
    }
}
```

---

### `ui/conversation/ConversationScreen.kt` (tema aplicado completo)

```kotlin
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.ui.conversation.components.*
import com.july.offline.ui.permission.PermissionHandler
import com.july.offline.ui.theme.*

@Composable
fun ConversationScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
            // Toggle wake-word compacto
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
            shape = androidx.compose.foundation.shape.RectangleShape
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
                letterSpacing = (0.08).sp
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

private val sp = androidx.compose.ui.unit.TextUnit
    .Unspecified.javaClass.declaredFields.first()
    .also { it.isAccessible = true }
    .let { 0.08f }
    .let { androidx.compose.ui.unit.sp }
```

---

### `ui/settings/SettingsScreen.kt` (tema aplicado)

```kotlin
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
import com.july.offline.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
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
                    androidx.compose.foundation.shape.RectangleShape
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
```

---

## RESUMEN DE CAMBIOS FASE 6 → FASE 7

| Componente | Cambio |
|---|---|
| `JulyColors.kt` | NUEVO — paleta completa verde terminal, dark/light schemes |
| `JulyTypography.kt` | NUEVO — JetBrains Mono + Inter, escala tipográfica completa |
| `JulyShapes.kt` | NUEVO — geometría precisa, shapes de burbuja personalizados |
| `JulyAnimations.kt` | NUEVO — duraciones y curvas centralizadas |
| `JulyTheme.kt` | NUEVO — MaterialTheme wraper con dynamic color desactivado |
| `StatusBar` | Fuente mono, prefijo >, color animado por estado |
| `WaveformIndicator` | 12 barras, opacidad escalonada, delays precisos |
| `WakeWordIndicator` | Punto pulsante sutil, fuente mono |
| `EngineHealthWidget` | Chips con borde de 0.5dp, estilo terminal |
| `MessageBubble` | Shapes asimétricos, fuente Inter para contenido |
| `ConversationScreen` | Scaffold con colores July, botones refactorizados |
| `SettingsScreen` | Chip groups, dividers de 0.5dp, paleta completa |
| `MainActivity` | Envuelve en `JulyTheme` |
| `themes.xml` | Fondo oscuro para splash y sistema |
| `colors.xml` | Colores base para recursos Android |
| `ic_july_splash.xml` | Icono vectorial para splash screen |
| Fuentes (bash script) | JetBrains Mono + Inter descargables |

---

## NOTAS DE IMPLEMENTACIÓN

### Lettercase en labels
Todo el texto de la UI de July está en minúsculas — incluidos los labels
de botones, estados y settings. Esto refuerza la estética terminal/minimalista.
En Compose, aplicar `.lowercase()` al string o usar el parámetro
`letterSpacing` no afecta el case — hay que hacer `text.lowercase()`.

### El `0.5.dp` como lenguaje visual
Todos los bordes son de 0.5dp. En Android esto equivale a 1px en pantallas
de densidad normal y se mantiene en 1px físico en pantallas de alta densidad
gracias al sistema de dp. El resultado es una línea extremadamente fina
que da sensación de precisión.

### Dynamic Color desactivado intencionalmente
Material You adapta los colores al fondo de pantalla. Para July esto es
inaceptable — el verde terminal es parte de la identidad del producto.
`JulyTheme` ignora el parámetro `dynamicColor` y siempre usa los schemes
definidos en `JulyColors.kt`.
