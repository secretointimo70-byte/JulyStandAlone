package com.july.offline.security.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.security.report.SecurityFinding
import com.july.offline.ui.theme.JulyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = JulyPalette.Dark50,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JulyPalette.Dark100),
                title = {
                    Text(
                        "july / seguridad",
                        style = MaterialTheme.typography.titleLarge,
                        color = JulyPalette.Green400
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", color = JulyPalette.TextSecondary,
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = uiState) {

                is SecurityUiState.Idle -> IdleContent(
                    onFullAudit = { viewModel.runFullAudit() },
                    onAppAudit = { viewModel.runAppAudit() },
                    onDeviceAudit = { viewModel.runDeviceAudit() },
                    onNetworkAudit = { viewModel.runNetworkAudit() }
                )

                is SecurityUiState.Scanning -> ScanningContent(state)

                is SecurityUiState.Complete -> ReportContent(
                    state = state,
                    onReset = { viewModel.reset() }
                )

                is SecurityUiState.Error -> {
                    Text(state.message, color = JulyPalette.Error,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.reset() }) {
                        Text("reintentar", color = JulyPalette.Green400)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onFullAudit: () -> Unit,
    onAppAudit: () -> Unit,
    onDeviceAudit: () -> Unit,
    onNetworkAudit: () -> Unit
) {
    Text(
        "Selecciona el tipo de auditoría:",
        style = MaterialTheme.typography.bodyLarge,
        color = JulyPalette.TextPrimary
    )
    Spacer(Modifier.height(16.dp))

    listOf(
        "Auditoría completa" to onFullAudit,
        "Solo app July" to onAppAudit,
        "Solo dispositivo" to onDeviceAudit,
        "Solo red local" to onNetworkAudit
    ).forEach { (label, action) ->
        Surface(
            onClick = action,
            color = JulyPalette.Dark200,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(0.5.dp, JulyPalette.Dark400, MaterialTheme.shapes.small)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = JulyPalette.Green400,
                modifier = Modifier.padding(14.dp)
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(
        "⚠ Solo usar en infraestructura propia o con autorización escrita.",
        style = MaterialTheme.typography.labelSmall,
        color = JulyPalette.Error.copy(alpha = 0.7f)
    )
}

@Composable
private fun ScanningContent(state: SecurityUiState.Scanning) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(color = JulyPalette.Green400)
        Spacer(Modifier.height(16.dp))
        Text(state.message, style = MaterialTheme.typography.bodyMedium,
            color = JulyPalette.TextSecondary)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.progress / 100f },
            color = JulyPalette.Green400,
            trackColor = JulyPalette.Dark300,
            modifier = Modifier.fillMaxWidth()
        )
        Text("${state.progress}%", style = MaterialTheme.typography.labelSmall,
            color = JulyPalette.TextTertiary)
    }
}

@Composable
private fun ReportContent(
    state: SecurityUiState.Complete,
    onReset: () -> Unit
) {
    val report = state.report
    val riskColor = when (report.riskLevel) {
        "CRITICAL" -> JulyPalette.Error
        "HIGH"     -> JulyPalette.Warning
        else       -> JulyPalette.Green400
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Surface(
                color = JulyPalette.Dark200,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, riskColor.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("riesgo: ${report.riskLevel.lowercase()}",
                        style = MaterialTheme.typography.labelLarge, color = riskColor)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${report.totalFindings} hallazgos · ${report.criticalCount} críticos · " +
                            "${report.highCount} altos · ${report.durationMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = JulyPalette.TextTertiary
                    )
                    if (report.llmSummary.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(report.llmSummary, style = MaterialTheme.typography.bodySmall,
                            color = JulyPalette.TextSecondary)
                    }
                }
            }
        }

        items(report.findings) { finding ->
            FindingCard(finding)
        }

        item {
            TextButton(onClick = onReset) {
                Text("nueva auditoría", style = MaterialTheme.typography.labelMedium,
                    color = JulyPalette.TextSecondary)
            }
        }
    }
}

@Composable
private fun FindingCard(finding: SecurityFinding) {
    val color = when (finding.severity) {
        SecurityFinding.Severity.CRITICAL -> JulyPalette.Error
        SecurityFinding.Severity.HIGH     -> JulyPalette.Warning
        SecurityFinding.Severity.MEDIUM   -> JulyPalette.Green200
        SecurityFinding.Severity.LOW      -> JulyPalette.TextSecondary
        SecurityFinding.Severity.INFO     -> JulyPalette.TextTertiary
    }
    Surface(
        color = JulyPalette.Dark100,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, color.copy(alpha = 0.4f), MaterialTheme.shapes.small)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(finding.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = JulyPalette.TextPrimary,
                    modifier = Modifier.weight(1f))
                Text(finding.severity.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Text(finding.description, style = MaterialTheme.typography.bodySmall,
                color = JulyPalette.TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text("→ ${finding.recommendation}",
                style = MaterialTheme.typography.labelSmall,
                color = JulyPalette.Green400.copy(alpha = 0.8f))
        }
    }
}
