package com.july.offline.ui.download

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.download.DownloadStatus
import com.july.offline.download.DownloadableModel
import com.july.offline.ui.theme.JulyPalette

private val CardShape = RoundedCornerShape(10.dp)
private val ButtonShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val states by viewModel.states.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = JulyPalette.Dark50,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JulyPalette.Dark100,
                    titleContentColor = JulyPalette.Green400
                ),
                title = {
                    Text("july / modelos", style = MaterialTheme.typography.titleLarge,
                        color = JulyPalette.Green400)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", color = JulyPalette.TextSecondary,
                            style = MaterialTheme.typography.titleMedium)
                    }
                },
                modifier = Modifier.border(0.5.dp, JulyPalette.Dark300,
                    androidx.compose.ui.graphics.RectangleShape)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Descarga los modelos de IA necesarios para que July funcione sin conexión.",
                style = MaterialTheme.typography.bodySmall,
                color = JulyPalette.TextTertiary,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            states.forEach { state ->
                ModelCard(
                    state = state,
                    onDownload = { viewModel.startDownload(state.model) },
                    onCancel = { viewModel.cancelDownload(state.model) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Los archivos se guardan en el almacenamiento interno de la app y no requieren conexión después de instalarse.",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.TextTertiary.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ModelCard(
    state: ModelDownloadUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = JulyPalette.Dark100,
        shape = CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, statusBorderColor(state.status), CardShape)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.model.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = JulyPalette.TextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(state.status)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.model.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = JulyPalette.TextTertiary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = state.model.sizeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = JulyPalette.TextTertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            when (val s = state.status) {
                is DownloadStatus.Installed -> InstalledRow()
                is DownloadStatus.Downloading -> DownloadingRow(s, onCancel)
                is DownloadStatus.Pending -> PendingRow(onCancel)
                is DownloadStatus.Failed -> FailedRow(state.model, onDownload)
                is DownloadStatus.NotInstalled -> DownloadButton(state.model, onDownload)
            }
        }
    }
}

@Composable
private fun InstalledRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("✓", color = JulyPalette.Green400, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("Instalado", style = MaterialTheme.typography.labelMedium, color = JulyPalette.Green400)
    }
}

@Composable
private fun DownloadingRow(status: DownloadStatus.Downloading, onCancel: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${status.downloadedMb} / ${status.totalMb}",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.TextSecondary
            )
            Text(
                text = "${status.progressPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.Green400,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { status.progress },
            modifier = Modifier.fillMaxWidth(),
            color = JulyPalette.Green400,
            trackColor = JulyPalette.Dark300
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = onCancel,
            color = Color.Transparent,
            shape = ButtonShape,
            modifier = Modifier.border(0.5.dp, JulyPalette.Dark400, ButtonShape)
        ) {
            Text(
                text = "Cancelar",
                style = MaterialTheme.typography.labelMedium,
                color = JulyPalette.TextTertiary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun PendingRow(onCancel: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = JulyPalette.Green400
        )
        Spacer(Modifier.width(8.dp))
        Text("En cola…", style = MaterialTheme.typography.labelMedium, color = JulyPalette.TextSecondary)
        Spacer(Modifier.width(12.dp))
        Surface(
            onClick = onCancel,
            color = Color.Transparent,
            shape = ButtonShape,
            modifier = Modifier.border(0.5.dp, JulyPalette.Dark400, ButtonShape)
        ) {
            Text("Cancelar", style = MaterialTheme.typography.labelMedium,
                color = JulyPalette.TextTertiary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun FailedRow(model: DownloadableModel, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("✕", color = Color(0xFFE24B4A), fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text("Error al descargar", style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE24B4A), modifier = Modifier.weight(1f))
        Surface(
            onClick = onRetry,
            color = JulyPalette.Green800,
            shape = ButtonShape,
            modifier = Modifier.border(0.5.dp, JulyPalette.Green400, ButtonShape)
        ) {
            Text("Reintentar", style = MaterialTheme.typography.labelMedium,
                color = JulyPalette.Green400,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun DownloadButton(model: DownloadableModel, onDownload: () -> Unit) {
    Surface(
        onClick = onDownload,
        color = JulyPalette.Green800,
        shape = ButtonShape,
        modifier = Modifier.border(0.5.dp, JulyPalette.Green400, ButtonShape)
    ) {
        Text(
            text = "Descargar  ${model.sizeLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = JulyPalette.Green400,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val (text, color) = when (status) {
        is DownloadStatus.Installed -> "✓" to JulyPalette.Green400
        is DownloadStatus.Downloading -> "↓" to Color(0xFF5BA3F5)
        is DownloadStatus.Pending -> "⏳" to JulyPalette.TextTertiary
        is DownloadStatus.Failed -> "✕" to Color(0xFFE24B4A)
        is DownloadStatus.NotInstalled -> return
    }
    Text(text, color = color, fontSize = 12.sp)
}

private fun statusBorderColor(status: DownloadStatus) = when (status) {
    is DownloadStatus.Installed -> Color(0xFF27500A).copy(alpha = 0.8f)
    is DownloadStatus.Downloading,
    is DownloadStatus.Pending -> Color(0xFF1A3A5C).copy(alpha = 0.8f)
    is DownloadStatus.Failed -> Color(0xFF5C1A1A).copy(alpha = 0.8f)
    is DownloadStatus.NotInstalled -> Color(0xFF2C2C2A)
}
