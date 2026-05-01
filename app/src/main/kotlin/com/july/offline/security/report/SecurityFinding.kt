package com.july.offline.security.report

data class SecurityFinding(
    val id: String,
    val severity: Severity,
    val category: FindingCategory,
    val title: String,
    val description: String,
    val recommendation: String,
    val rawData: String? = null,
    val remediation: Remediation? = null
) {
    enum class Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }

    enum class FindingCategory {
        PERMISSION,
        STORAGE,
        NETWORK,
        ENCRYPTION,
        AUTHENTICATION,
        CONFIGURATION,
        CERTIFICATE,
        OPEN_PORT,
        SERVICE,
        ROOTKIT
    }
}
