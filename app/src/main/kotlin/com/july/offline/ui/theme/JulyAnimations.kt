package com.july.offline.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Especificaciones de animación del Design System July.
 * Todas las duraciones y curvas están centralizadas aquí.
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
