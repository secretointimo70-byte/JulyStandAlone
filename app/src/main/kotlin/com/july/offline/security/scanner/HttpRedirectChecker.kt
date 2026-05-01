package com.july.offline.security.scanner

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class HttpRedirectChecker @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    data class RedirectResult(
        val host: String,
        val port: Int,
        val redirectsToHttps: Boolean,
        val redirectUrl: String?,
        val statusCode: Int,
        val serverBanner: String?
    )

    suspend fun check(host: String, port: Int = 80): RedirectResult =
        withContext(Dispatchers.IO) {
        logger.logInfo("HttpRedirectChecker", "Checking $host:$port")
        try {
            val conn = URL("http://$host:$port/").openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "HEAD"
            conn.setRequestProperty("User-Agent", "SecurityAudit/1.0")
            conn.connect()

            val status = conn.responseCode
            val location = conn.getHeaderField("Location")
            val server = conn.getHeaderField("Server")
            conn.disconnect()

            val redirectsToHttps = status in 301..308 &&
                location?.startsWith("https://", ignoreCase = true) == true

            RedirectResult(host, port, redirectsToHttps, location, status, server)
        } catch (e: Exception) {
            logger.logWarning("HttpRedirectChecker", "Check failed for $host:$port — ${e.message}")
            RedirectResult(host, port, false, null, -1, null)
        }
    }
}
