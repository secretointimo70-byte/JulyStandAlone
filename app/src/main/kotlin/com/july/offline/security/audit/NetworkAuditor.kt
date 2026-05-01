package com.july.offline.security.audit

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.security.report.SecurityFinding
import com.july.offline.security.report.SecurityFinding.Severity.*
import com.july.offline.security.report.SecurityFinding.FindingCategory.*
import com.july.offline.security.scanner.HostDiscovery
import com.july.offline.security.scanner.PortScanner
import com.july.offline.security.scanner.TlsAnalyzer
import javax.inject.Inject

class NetworkAuditor @Inject constructor(
    private val hostDiscovery: HostDiscovery,
    private val portScanner: PortScanner,
    private val tlsAnalyzer: TlsAnalyzer,
    private val logger: DiagnosticsLogger
) {

    data class NetworkAuditResult(
        val hostsFound: List<HostDiscovery.HostResult>,
        val findings: List<SecurityFinding>
    )

    suspend fun audit(
        onProgress: ((String, Int, Int) -> Unit)? = null
    ): NetworkAuditResult {
        val findings = mutableListOf<SecurityFinding>()
        logger.logInfo("NetworkAuditor", "Starting network audit")

        onProgress?.invoke("Descubriendo hosts en la red...", 0, 100)
        val hosts = hostDiscovery.discoverLocalNetwork { done, total ->
            onProgress?.invoke("Escaneando red local... ($done/$total)", done * 30 / total.coerceAtLeast(1), 100)
        }

        if (hosts.isEmpty()) {
            findings.add(SecurityFinding(
                id = "NET_NO_HOSTS", severity = INFO, category = NETWORK,
                title = "Sin hosts detectados",
                description = "No se encontraron hosts activos en la red local.",
                recommendation = "Verificar que el dispositivo está conectado a WiFi.",
                rawData = "hosts_found: 0"
            ))
            return NetworkAuditResult(emptyList(), findings)
        }

        findings.add(SecurityFinding(
            id = "NET_HOSTS_FOUND", severity = INFO, category = NETWORK,
            title = "${hosts.size} hosts activos en la red",
            description = "Hosts: " + hosts.take(10)
                .joinToString(", ") { it.ip + (it.hostname?.let { h -> " ($h)" } ?: "") } +
                if (hosts.size > 10) " y ${hosts.size - 10} más..." else "",
            recommendation = "Verificar que todos los hosts son dispositivos conocidos.",
            rawData = hosts.joinToString("\n") { "${it.ip}: ${it.hostname ?: "unknown"}" }
        ))

        hosts.take(20).forEachIndexed { idx, host ->
            onProgress?.invoke(
                "Escaneando puertos de ${host.ip}...",
                30 + idx * 60 / hosts.size.coerceAtLeast(1), 100
            )

            val openPorts = portScanner.scan(host.ip, PortScanner.COMMON_PORTS).filter { it.isOpen }

            openPorts.forEach { p ->
                findings.add(SecurityFinding(
                    id = "NET_PORT_${host.ip.replace(".", "_")}_${p.port}",
                    severity = riskLevel(p.port), category = OPEN_PORT,
                    title = "Puerto abierto: ${host.ip}:${p.port} (${p.serviceGuess ?: "desconocido"})",
                    description = buildDesc(p),
                    recommendation = portRecommendation(p.port),
                    rawData = "host: ${host.ip}, port: ${p.port}, banner: ${p.banner ?: "none"}"
                ))
            }

            if (openPorts.any { it.port == 443 }) {
                tlsAnalyzer.analyze(host.ip, 443).findings.forEach { f ->
                    findings.add(f.copy(id = "${f.id}_${host.ip.replace(".", "_")}"))
                }
            }

            if (openPorts.any { it.port == 23 }) {
                findings.add(SecurityFinding(
                    id = "NET_TELNET_${host.ip.replace(".", "_")}",
                    severity = CRITICAL, category = NETWORK,
                    title = "Telnet abierto en ${host.ip}",
                    description = "Telnet transmite credenciales en texto plano.",
                    recommendation = "Deshabilitar Telnet. Migrar a SSH (puerto 22).",
                    rawData = "telnet: ${host.ip}:23"
                ))
            }
        }

        onProgress?.invoke("Auditoría de red completada", 100, 100)
        logger.logInfo("NetworkAuditor", "Done: ${findings.size} findings")
        return NetworkAuditResult(hosts, findings)
    }

    private fun riskLevel(port: Int): SecurityFinding.Severity = when (port) {
        23 -> CRITICAL
        21, 3389, 5900, 6379, 9200, 27017 -> HIGH
        22, 3306, 5432, 1433 -> MEDIUM
        80, 8080 -> LOW
        else -> INFO
    }

    private fun buildDesc(r: PortScanner.PortResult): String = buildString {
        append("Puerto ${r.port} abierto en ${r.host}.")
        r.serviceGuess?.let { append(" Servicio: $it.") }
        r.banner?.let { append(" Banner: \"${it.take(80)}\"") }
    }

    private fun portRecommendation(port: Int): String = when (port) {
        21 -> "Migrar de FTP a SFTP. Deshabilitar FTP anónimo."
        22 -> "Verificar autenticación por clave, no contraseña. Deshabilitar acceso root."
        23 -> "Deshabilitar Telnet inmediatamente. Usar SSH."
        3306 -> "Restringir MySQL a 127.0.0.1. Cambiar contraseñas por defecto."
        3389 -> "Limitar RDP a IPs específicas via firewall."
        5900 -> "Usar VNC solo sobre túnel SSH."
        6379 -> "Configurar autenticación en Redis. Restringir bind a 127.0.0.1."
        9200 -> "Habilitar autenticación en Elasticsearch."
        27017 -> "Habilitar autenticación en MongoDB. Restringir bind a 127.0.0.1."
        else -> "Verificar si este servicio es necesario. Cerrar puertos no utilizados."
    }
}
