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
