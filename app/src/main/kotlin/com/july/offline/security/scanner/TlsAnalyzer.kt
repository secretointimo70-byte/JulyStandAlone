package com.july.offline.security.scanner

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.security.report.SecurityFinding
import com.july.offline.security.report.SecurityFinding.Severity.*
import com.july.offline.security.report.SecurityFinding.FindingCategory.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.net.ssl.HttpsURLConnection
import javax.inject.Inject

class TlsAnalyzer @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    data class TlsResult(
        val host: String,
        val port: Int,
        val isValid: Boolean,
        val certificate: CertInfo?,
        val findings: List<SecurityFinding>
    )

    data class CertInfo(
        val subject: String,
        val issuer: String,
        val notBefore: Instant,
        val notAfter: Instant,
        val daysUntilExpiry: Long,
        val signatureAlgorithm: String
    )

    suspend fun analyze(host: String, port: Int = 443): TlsResult =
        withContext(Dispatchers.IO) {

        logger.logInfo("TlsAnalyzer", "Analyzing TLS $host:$port")
        val findings = mutableListOf<SecurityFinding>()
        var certInfo: CertInfo? = null

        try {
            val conn = URL("https://$host:$port").openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            val certs = conn.serverCertificates
            conn.disconnect()

            if (certs.isNotEmpty()) {
                val cert = certs[0] as X509Certificate
                val expiry = cert.notAfter.toInstant()
                val days = ChronoUnit.DAYS.between(Instant.now(), expiry)

                certInfo = CertInfo(
                    subject = cert.subjectDN.name,
                    issuer = cert.issuerDN.name,
                    notBefore = cert.notBefore.toInstant(),
                    notAfter = expiry,
                    daysUntilExpiry = days,
                    signatureAlgorithm = cert.sigAlgName
                )

                when {
                    days < 0 -> findings.add(SecurityFinding(
                        id = "TLS_CERT_EXPIRED",
                        severity = CRITICAL, category = CERTIFICATE,
                        title = "Certificado TLS expirado",
                        description = "El certificado expiró hace ${-days} días.",
                        recommendation = "Renovar el certificado inmediatamente.",
                        rawData = "notAfter: ${cert.notAfter}"
                    ))
                    days < 30 -> findings.add(SecurityFinding(
                        id = "TLS_CERT_EXPIRING",
                        severity = HIGH, category = CERTIFICATE,
                        title = "Certificado próximo a expirar",
                        description = "El certificado expira en $days días.",
                        recommendation = "Renovar antes de la expiración.",
                        rawData = "daysUntilExpiry: $days"
                    ))
                }

                if (cert.sigAlgName.contains("MD5", ignoreCase = true) ||
                    cert.sigAlgName.contains("SHA1", ignoreCase = true)) {
                    findings.add(SecurityFinding(
                        id = "TLS_WEAK_SIG",
                        severity = HIGH, category = CERTIFICATE,
                        title = "Algoritmo de firma débil: ${cert.sigAlgName}",
                        description = "El certificado usa un algoritmo de firma obsoleto.",
                        recommendation = "Reemplazar con certificado SHA-256 o superior.",
                        rawData = "sigAlgName: ${cert.sigAlgName}"
                    ))
                }

                if (findings.isEmpty()) {
                    findings.add(SecurityFinding(
                        id = "TLS_OK",
                        severity = INFO, category = CERTIFICATE,
                        title = "Certificado TLS válido",
                        description = "Válido por $days días más. Algoritmo: ${cert.sigAlgName}",
                        recommendation = "Configurar renovación automática (Let's Encrypt).",
                        rawData = "subject: ${cert.subjectDN.name}"
                    ))
                }
            }

            TlsResult(host, port, true, certInfo, findings)

        } catch (e: Exception) {
            logger.logError("TlsAnalyzer", "TLS analysis failed for $host:$port", e)
            findings.add(SecurityFinding(
                id = "TLS_CONNECT_FAIL",
                severity = MEDIUM, category = CERTIFICATE,
                title = "No se pudo verificar TLS en $host:$port",
                description = "Error: ${e.message}",
                recommendation = "Verificar que el servidor tiene HTTPS configurado.",
                rawData = "error: ${e.message}"
            ))
            TlsResult(host, port, false, null, findings)
        }
    }
}
