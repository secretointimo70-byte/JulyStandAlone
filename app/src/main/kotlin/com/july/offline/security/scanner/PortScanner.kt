package com.july.offline.security.scanner

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class PortScanner @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    data class PortResult(
        val host: String,
        val port: Int,
        val isOpen: Boolean,
        val banner: String? = null,
        val serviceGuess: String? = null
    )

    companion object {
        val COMMON_PORTS = listOf(
            21, 22, 23, 25, 53, 80, 110, 143, 443, 445,
            1433, 1521, 3306, 3389, 5432, 5900, 6379,
            8080, 8443, 9200, 11434, 27017
        )

        val SERVICE_NAMES = mapOf(
            21 to "FTP", 22 to "SSH", 23 to "Telnet",
            25 to "SMTP", 53 to "DNS", 80 to "HTTP",
            110 to "POP3", 143 to "IMAP", 443 to "HTTPS",
            445 to "SMB/CIFS", 1433 to "MSSQL", 1521 to "Oracle DB",
            3306 to "MySQL", 3389 to "RDP", 5432 to "PostgreSQL",
            5900 to "VNC", 6379 to "Redis", 8080 to "HTTP-alt",
            8443 to "HTTPS-alt", 9200 to "Elasticsearch",
            11434 to "Ollama LLM", 27017 to "MongoDB"
        )
    }

    suspend fun scan(
        host: String,
        ports: List<Int> = COMMON_PORTS,
        timeoutMs: Int = 1500,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<PortResult> = withContext(Dispatchers.IO) {
        logger.logInfo("PortScanner", "Scanning $host — ${ports.size} ports")
        val total = ports.size
        var scanned = 0

        val results = ports.map { port ->
            async {
                val result = checkPort(host, port, timeoutMs)
                synchronized(this@withContext) {
                    scanned++
                    onProgress?.invoke(scanned, total)
                }
                result
            }
        }.awaitAll()

        logger.logInfo("PortScanner", "Done: ${results.count { it.isOpen }} open on $host")
        results
    }

    private fun checkPort(host: String, port: Int, timeoutMs: Int): PortResult {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = 500
                val banner = try {
                    val buf = ByteArray(256)
                    val n = socket.getInputStream().read(buf)
                    if (n > 0) String(buf, 0, n).trim().take(100) else null
                } catch (e: Exception) { null }
                PortResult(host, port, true, banner, SERVICE_NAMES[port])
            }
        } catch (e: Exception) {
            PortResult(host, port, false)
        }
    }
}
