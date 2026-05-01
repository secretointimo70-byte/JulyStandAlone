package com.july.offline.security.scanner

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.security.report.SecurityFinding
import com.july.offline.security.report.SecurityFinding.Severity.*
import com.july.offline.security.report.SecurityFinding.FindingCategory.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

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
        val signatureAlgorithm: String,
        val isSelfSigned: Boolean
    )

    suspend fun analyze(host: String, port: Int = 443): TlsResult =
        withContext(Dispatchers.IO) {

        logger.logInfo("TlsAnalyzer", "Analyzing TLS $host:$port")

        // Intentar primero con validación estándar
        val standard = tryStandardTls(host, port)
        if (standard != null) return@withContext standard

        // Si falló la validación estándar, inspeccionar igualmente (cert autofirmado / CA privada)
        inspectWithTrustAll(host, port)
    }

    private fun tryStandardTls(host: String, port: Int): TlsResult? {
        return try {
            val conn = URL("https://$host:$port").openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            val certs = conn.serverCertificates
            conn.disconnect()

            if (certs.isEmpty()) return null
            val cert = certs[0] as X509Certificate
            buildResultFromCert(host, port, cert, isValidatedBySystem = true)
        } catch (e: Exception) {
            null // cert no confiable por el sistema — continuar con trust-all
        }
    }

    // Solo para inspección de metadatos del certificado, NUNCA para transmitir datos sensibles.
    private fun inspectWithTrustAll(host: String, port: Int): TlsResult {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        return try {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf(trustAll), SecureRandom())

            val socket = ctx.socketFactory.createSocket() as SSLSocket
            socket.connect(InetSocketAddress(host, port), 5000)
            socket.soTimeout = 5000
            socket.startHandshake()
            val certs = socket.session.peerCertificates
            socket.close()

            if (certs.isEmpty()) {
                TlsResult(host, port, false, null, listOf(noTlsFinding(host, port, "Sin certificados en la cadena")))
            } else {
                val cert = certs[0] as X509Certificate
                buildResultFromCert(host, port, cert, isValidatedBySystem = false)
            }
        } catch (e: Exception) {
            logger.logError("TlsAnalyzer", "inspect failed $host:$port", e)
            TlsResult(host, port, false, null, listOf(noTlsFinding(host, port, e.message ?: "error desconocido")))
        }
    }

    private fun buildResultFromCert(
        host: String,
        port: Int,
        cert: X509Certificate,
        isValidatedBySystem: Boolean
    ): TlsResult {
        val findings = mutableListOf<SecurityFinding>()
        val expiry = cert.notAfter.toInstant()
        val days = ChronoUnit.DAYS.between(Instant.now(), expiry)
        val isSelfSigned = cert.subjectDN.name == cert.issuerDN.name
        val cn = extractCN(cert.subjectDN.name)
        val suffix = "${host.replace(".", "_")}_$port"

        val certInfo = CertInfo(
            subject = cert.subjectDN.name,
            issuer = cert.issuerDN.name,
            notBefore = cert.notBefore.toInstant(),
            notAfter = expiry,
            daysUntilExpiry = days,
            signatureAlgorithm = cert.sigAlgName,
            isSelfSigned = isSelfSigned
        )

        // Certificado autofirmado
        if (isSelfSigned && !isValidatedBySystem) {
            findings.add(SecurityFinding(
                id = "TLS_SELF_SIGNED_$suffix",
                severity = MEDIUM, category = CERTIFICATE,
                title = "Certificado autofirmado en $host:$port",
                description = "CN: $cn. El servidor usa un cert autofirmado. " +
                    "Navegadores y apps mostrarán advertencias. Válido por $days días más.",
                recommendation = "Reemplazar con Let's Encrypt (gratuito) u otra CA pública.",
                rawData = "subject: ${cert.subjectDN.name}, issuer: ${cert.issuerDN.name}, days: $days"
            ))
        }

        // Expiración
        when {
            days < 0 -> findings.add(SecurityFinding(
                id = "TLS_CERT_EXPIRED_$suffix",
                severity = CRITICAL, category = CERTIFICATE,
                title = "Certificado TLS EXPIRADO en $host:$port",
                description = "Expiró hace ${-days} días. CN: $cn",
                recommendation = "Renovar el certificado inmediatamente.",
                rawData = "notAfter: ${cert.notAfter}"
            ))
            days < 30 -> findings.add(SecurityFinding(
                id = "TLS_CERT_EXPIRING_$suffix",
                severity = HIGH, category = CERTIFICATE,
                title = "Certificado expira en $days días ($host)",
                description = "CN: $cn. Renovar antes de que expire para evitar interrupciones.",
                recommendation = "Renovar el certificado.",
                rawData = "daysUntilExpiry: $days"
            ))
        }

        // Algoritmo débil
        if (cert.sigAlgName.contains("MD5", ignoreCase = true) ||
            cert.sigAlgName.contains("SHA1", ignoreCase = true)) {
            findings.add(SecurityFinding(
                id = "TLS_WEAK_SIG_$suffix",
                severity = HIGH, category = CERTIFICATE,
                title = "Algoritmo de firma débil: ${cert.sigAlgName} en $host",
                description = "El certificado usa un algoritmo de firma obsoleto y vulnerable.",
                recommendation = "Reemplazar con certificado SHA-256 o superior.",
                rawData = "sigAlgName: ${cert.sigAlgName}"
            ))
        }

        // Todo bien
        if (findings.isEmpty()) {
            findings.add(SecurityFinding(
                id = "TLS_OK_$suffix",
                severity = INFO, category = CERTIFICATE,
                title = "HTTPS válido en $host:$port",
                description = "Certificado: $cn. Válido por $days días. Algoritmo: ${cert.sigAlgName}",
                recommendation = "Configurar renovación automática.",
                rawData = "subject: ${cert.subjectDN.name}"
            ))
        }

        return TlsResult(host, port, isValidatedBySystem, certInfo, findings)
    }

    private fun noTlsFinding(host: String, port: Int, error: String) = SecurityFinding(
        id = "TLS_NO_RESPONSE_${host.replace(".", "_")}_$port",
        severity = MEDIUM, category = CERTIFICATE,
        title = "Sin respuesta TLS válida en $host:$port",
        description = "Puerto 443 abierto pero TLS no responde correctamente. Error: $error",
        recommendation = "Verificar la configuración de TLS en el servidor.",
        rawData = "error: $error"
    )

    private fun extractCN(dn: String): String =
        Regex("CN=([^,]+)").find(dn)?.groupValues?.get(1) ?: dn
}
