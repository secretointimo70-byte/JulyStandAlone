package com.july.offline.data.network

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkHealthChecker @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    companion object {
        private const val TCP_TIMEOUT_MS = 3_000
    }

    suspend fun isReachable(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TCP_TIMEOUT_MS)
                true
            }
        } catch (e: Exception) {
            logger.logDebug("NetworkHealthChecker", "Unreachable $host:$port — ${e.message}")
            false
        }
    }
}
