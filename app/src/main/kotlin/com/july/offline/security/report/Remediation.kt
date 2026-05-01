package com.july.offline.security.report

data class Remediation(
    val title: String,
    val steps: List<RemediationStep>
)

data class RemediationStep(
    val description: String,
    val command: String? = null
)
