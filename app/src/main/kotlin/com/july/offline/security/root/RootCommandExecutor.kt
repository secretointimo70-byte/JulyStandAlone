package com.july.offline.security.root

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RootCommandExecutor @Inject constructor(
    private val rootChecker: RootChecker,
    private val logger: DiagnosticsLogger
) {

    data class CommandResult(
        val command: String,
        val output: String,
        val error: String,
        val exitCode: Int
    )

    suspend fun execute(command: String, timeoutMs: Long = 5000L): CommandResult =
        withContext(Dispatchers.IO) {
        if (!rootChecker.check().isRooted) {
            return@withContext CommandResult(command, "", "Root no disponible", -1)
        }
        logger.logInfo("RootCommandExecutor", "Executing: $command")
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = p.inputStream.bufferedReader().readText()
            val error = p.errorStream.bufferedReader().readText()
            CommandResult(command, output.trim(), error.trim(), p.waitFor())
        } catch (e: Exception) {
            logger.logError("RootCommandExecutor", "Command failed: $command", e)
            CommandResult(command, "", e.message ?: "Unknown error", -1)
        }
    }

    suspend fun getNetworkConnections() = execute("cat /proc/net/tcp /proc/net/tcp6 2>/dev/null")
    suspend fun getRunningProcesses() = execute("ps -A 2>/dev/null || ps 2>/dev/null")
    suspend fun getSeLinuxStatus() = execute("getenforce 2>/dev/null")
    suspend fun getDmesgSecurityEvents() =
        execute("dmesg | grep -i 'denied\\|audit\\|selinux' | tail -50")
}
