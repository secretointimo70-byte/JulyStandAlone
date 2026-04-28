package com.july.offline.ui.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Usuario: fondo verde oscuro, esquina inferior derecha aguda
 * Asistente: fondo superficie, esquina inferior izquierda aguda
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

            val textColor = JulyPalette.TextPrimary
            val shape = if (message.isUser) UserMessageShape else AssistantMessageShape

            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .background(color = bgColor, shape = shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
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
