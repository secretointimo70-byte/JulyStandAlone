package com.july.offline.security.report

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReportFormatter {

    private val DATE_FMT = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    fun format(report: SecurityReport): String = buildString {
        val line = "─".repeat(52)
        val date = DATE_FMT.format(report.createdAt)
        val type = when (report.auditType) {
            SecurityReport.AuditType.FULL    -> "completa"
            SecurityReport.AuditType.APP     -> "aplicación"
            SecurityReport.AuditType.DEVICE  -> "dispositivo"
            SecurityReport.AuditType.NETWORK -> "red local"
        }

        appendLine("INFORME DE SEGURIDAD — JULY OFFLINE")
        appendLine(line)
        appendLine("Fecha       : $date")
        appendLine("Tipo        : $type")
        appendLine("Riesgo      : ${report.riskLevel}")
        appendLine("Hallazgos   : ${report.totalFindings}  " +
            "(${report.criticalCount} críticos · ${report.highCount} altos)")
        appendLine("Duración    : ${report.durationMs / 1000}s")
        appendLine(line)

        if (report.llmSummary.isNotBlank()) {
            appendLine()
            appendLine("RESUMEN")
            appendLine(report.llmSummary)
        }

        val bySeverity = report.findings.groupBy { it.severity }
        val order = listOf(
            SecurityFinding.Severity.CRITICAL,
            SecurityFinding.Severity.HIGH,
            SecurityFinding.Severity.MEDIUM,
            SecurityFinding.Severity.LOW,
            SecurityFinding.Severity.INFO
        )

        order.forEach { sev ->
            val group = bySeverity[sev] ?: return@forEach
            appendLine()
            appendLine(line)
            appendLine("${sev.name} (${group.size})")
            appendLine(line)
            group.forEach { f ->
                appendLine()
                appendLine("[${f.severity.name}] ${f.title}")
                appendLine(f.description)
                appendLine("→ ${f.recommendation}")
                f.remediation?.let { rem ->
                    appendLine()
                    appendLine("  Cómo corregirlo: ${rem.title}")
                    rem.steps.forEach { step ->
                        if (step.command != null) {
                            if (step.description.isNotBlank()) appendLine("  • ${step.description}")
                            appendLine("    $ ${step.command}")
                        } else if (step.description.isNotBlank()) {
                            appendLine("  • ${step.description}")
                        }
                    }
                }
            }
        }

        appendLine()
        appendLine(line)
        appendLine("Generado por July Offline · ${date}")
        appendLine(line)
    }

    fun filename(report: SecurityReport): String {
        val date = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmm")
            .withZone(ZoneId.systemDefault())
            .format(report.createdAt)
        return "july_seguridad_${date}.txt"
    }
}
