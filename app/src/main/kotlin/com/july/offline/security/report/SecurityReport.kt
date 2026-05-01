package com.july.offline.security.report

import java.time.Instant

data class SecurityReport(
    val id: String,
    val createdAt: Instant,
    val auditType: AuditType,
    val findings: List<SecurityFinding>,
    val llmSummary: String = "",
    val durationMs: Long = 0L
) {
    enum class AuditType { APP, DEVICE, NETWORK, FULL }

    val criticalCount: Int get() = findings.count { it.severity == SecurityFinding.Severity.CRITICAL }
    val highCount: Int get() = findings.count { it.severity == SecurityFinding.Severity.HIGH }
    val totalFindings: Int get() = findings.size

    val riskLevel: String get() = when {
        criticalCount > 0 -> "CRITICAL"
        highCount > 0     -> "HIGH"
        findings.any { it.severity == SecurityFinding.Severity.MEDIUM } -> "MEDIUM"
        else              -> "LOW"
    }
}
