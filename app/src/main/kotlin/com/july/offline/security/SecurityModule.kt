package com.july.offline.security

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.security.audit.AppAuditor
import com.july.offline.security.audit.DeviceAuditor
import com.july.offline.security.audit.NetworkAuditor
import com.july.offline.security.report.SecurityFinding
import com.july.offline.security.report.SecurityReport
import com.july.offline.security.report.SecurityReport.AuditType
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityModule @Inject constructor(
    private val appAuditor: AppAuditor,
    private val deviceAuditor: DeviceAuditor,
    private val networkAuditor: NetworkAuditor,
    private val llmEngine: LanguageModelEngine,
    private val logger: DiagnosticsLogger
) {

    suspend fun runFullAudit(
        onProgress: ((String, Int) -> Unit)? = null
    ): SecurityReport {
        val start = System.currentTimeMillis()
        val findings = mutableListOf<SecurityFinding>()

        onProgress?.invoke("Auditando app July...", 10)
        findings += appAuditor.audit()

        onProgress?.invoke("Auditando dispositivo...", 35)
        findings += deviceAuditor.audit()

        onProgress?.invoke("Auditando red local...", 60)
        findings += networkAuditor.audit { msg, progress, _ ->
            onProgress?.invoke(msg, 60 + progress * 30 / 100)
        }.findings

        onProgress?.invoke("Generando análisis con LLM...", 92)
        val summary = generateLlmSummary(findings)

        onProgress?.invoke("Informe completado", 100)
        return SecurityReport(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now(),
            auditType = AuditType.FULL,
            findings = findings.sortedByDescending { it.severity.ordinal },
            llmSummary = summary,
            durationMs = System.currentTimeMillis() - start
        )
    }

    suspend fun runAppAudit(): SecurityReport {
        val findings = appAuditor.audit()
        return SecurityReport(
            UUID.randomUUID().toString(), Instant.now(), AuditType.APP,
            findings.sortedByDescending { it.severity.ordinal },
            generateLlmSummary(findings)
        )
    }

    suspend fun runDeviceAudit(): SecurityReport {
        val findings = deviceAuditor.audit()
        return SecurityReport(
            UUID.randomUUID().toString(), Instant.now(), AuditType.DEVICE,
            findings.sortedByDescending { it.severity.ordinal },
            generateLlmSummary(findings)
        )
    }

    suspend fun runNetworkAudit(
        onProgress: ((String, Int, Int) -> Unit)? = null
    ): SecurityReport {
        val result = networkAuditor.audit(onProgress)
        return SecurityReport(
            UUID.randomUUID().toString(), Instant.now(), AuditType.NETWORK,
            result.findings.sortedByDescending { it.severity.ordinal },
            generateLlmSummary(result.findings)
        )
    }

    private suspend fun generateLlmSummary(findings: List<SecurityFinding>): String {
        if (findings.isEmpty()) return "No se encontraron hallazgos de seguridad."
        return try {
            if (!llmEngine.isAvailable()) return "LLM no disponible para análisis."

            val critical = findings.filter { it.severity == SecurityFinding.Severity.CRITICAL }
            val high = findings.filter { it.severity == SecurityFinding.Severity.HIGH }

            val prompt = buildString {
                appendLine("Analiza estos hallazgos de seguridad y genera un resumen con las 3 acciones más urgentes:")
                if (critical.isNotEmpty()) {
                    appendLine("CRÍTICOS (${critical.size}):")
                    critical.forEach { appendLine("- ${it.title}: ${it.description.take(100)}") }
                }
                if (high.isNotEmpty()) {
                    appendLine("ALTOS (${high.size}):")
                    high.forEach { appendLine("- ${it.title}: ${it.description.take(100)}") }
                }
                appendLine("Total hallazgos: ${findings.size}")
                appendLine("Proporciona: nivel de riesgo general y las 3 acciones más urgentes.")
            }

            val systemMsg = Message(
                id = "security_system", role = MessageRole.SYSTEM,
                content = "Eres un experto en ciberseguridad. Análisis técnico concreto y accionable. Lenguaje directo."
            )

            when (val r = llmEngine.generate(prompt, listOf(systemMsg))) {
                is JulyResult.Success -> r.data.text
                is JulyResult.Failure -> "Error al generar análisis: ${r.error.message}"
            }
        } catch (e: Exception) {
            logger.logError("SecurityModule", "LLM summary failed", e)
            "Análisis LLM no disponible."
        }
    }
}
