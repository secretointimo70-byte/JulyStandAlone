package com.july.offline.security.scanner

import android.content.Context
import android.net.wifi.WifiManager
import com.july.offline.core.logging.DiagnosticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject

class HostDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) {

    data class HostResult(
        val ip: String,
        val hostname: String?,
        val isReachable: Boolean,
        val responseTimeMs: Long
    )

    suspend fun discoverLocalNetwork(
        timeoutMs: Int = 1000,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<HostResult> = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ipInt = wifiManager.connectionInfo.ipAddress

        if (ipInt == 0) {
            logger.logWarning("HostDiscovery", "No WiFi connection")
            return@withContext emptyList()
        }

        val subnet = String.format(
            "%d.%d.%d",
            ipInt and 0xFF,
            (ipInt shr 8) and 0xFF,
            (ipInt shr 16) and 0xFF
        )
        logger.logInfo("HostDiscovery", "Scanning $subnet.0/24")

        val total = 254
        var scanned = 0

        (1..254).map { host ->
            async {
                val result = pingHost("$subnet.$host", timeoutMs)
                synchronized(this@withContext) {
                    scanned++
                    onProgress?.invoke(scanned, total)
                }
                result
            }
        }.awaitAll().filter { it.isReachable }
    }

    private fun pingHost(ip: String, timeoutMs: Int): HostResult {
        val start = System.currentTimeMillis()
        return try {
            val address = InetAddress.getByName(ip)
            val reachable = address.isReachable(timeoutMs)
            HostResult(
                ip = ip,
                hostname = if (reachable) {
                    try { address.canonicalHostName.takeIf { it != ip } } catch (e: Exception) { null }
                } else null,
                isReachable = reachable,
                responseTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            HostResult(ip, null, false, System.currentTimeMillis() - start)
        }
    }
}
