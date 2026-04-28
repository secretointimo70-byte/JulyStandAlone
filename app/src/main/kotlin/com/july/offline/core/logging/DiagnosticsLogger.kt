package com.july.offline.core.logging

import android.util.Log
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.entity.DiagnosticsDbEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsLogger @Inject constructor(
    private val diagnosticsDao: DiagnosticsDao
) {

    companion object {
        private const val APP_TAG = "JulyOffline"
        private const val RETENTION_DAYS = 7L
    }

    private val logScope = CoroutineScope(SupervisorJob())

    fun logInfo(tag: String, message: String) {
        Log.i(APP_TAG, "[$tag] $message")
        persist("INFO", tag, message)
    }

    fun logDebug(tag: String, message: String) {
        Log.d(APP_TAG, "[$tag] $message")
    }

    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        Log.w(APP_TAG, "[$tag] $message", cause)
        persist("WARNING", tag, message, cause)
    }

    fun logError(tag: String, message: String, cause: Throwable? = null) {
        Log.e(APP_TAG, "[$tag] $message", cause)
        persist("ERROR", tag, message, cause)
    }

    fun logStateTransition(from: String, to: String, trigger: String) {
        val message = "$from → $to (trigger: $trigger)"
        Log.i(APP_TAG, "[State] $message")
        persist("INFO", "State", message)
    }

    fun logEngineEvent(engine: String, event: String, latencyMs: Long? = null) {
        val latencyStr = latencyMs?.let { " [${it}ms]" } ?: ""
        val message = "$event$latencyStr"
        Log.i(APP_TAG, "[Engine:$engine] $message")
        persist("INFO", "Engine:$engine", message)
    }

    fun pruneOldLogs() {
        val cutoffMs = Instant.now().toEpochMilli() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        logScope.launch {
            try {
                diagnosticsDao.deleteOlderThan(cutoffMs)
            } catch (e: Exception) {
                Log.w(APP_TAG, "[DiagnosticsLogger] pruneOldLogs failed", e)
            }
        }
    }

    private fun persist(level: String, tag: String, message: String, cause: Throwable? = null) {
        logScope.launch {
            try {
                diagnosticsDao.insert(
                    DiagnosticsDbEntity(
                        timestamp = Instant.now().toEpochMilli(),
                        level = level,
                        tag = tag,
                        message = message,
                        stackTrace = cause?.stackTraceToString()?.take(2000)
                    )
                )
            } catch (e: Exception) {
                Log.w(APP_TAG, "[DiagnosticsLogger] persist failed", e)
            }
        }
    }
}
