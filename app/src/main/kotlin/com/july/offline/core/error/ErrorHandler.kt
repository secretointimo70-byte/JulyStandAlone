package com.july.offline.core.error

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    companion object {
        private const val LLM_RETRY_DELAY_MS = 500L
        private const val MAX_LLM_RETRIES = 1
    }

    suspend fun handle(error: AppError): ErrorAction = withContext(dispatchers.io) {
        logger.logError(
            tag = "ErrorHandler",
            message = error.message,
            cause = error.cause
        )

        when (error) {
            is AppError.Permission -> ErrorAction.ShowPermissionRationale
            is AppError.Stt -> ErrorAction.ResetToIdle(error)
            is AppError.Llm -> {
                if (error.retryable) ErrorAction.Retry(delayMs = LLM_RETRY_DELAY_MS)
                else ErrorAction.ResetToIdle(error)
            }
            is AppError.Tts -> ErrorAction.FallbackToText(error)
            is AppError.Network -> ErrorAction.ResetToIdle(error)
            is AppError.Cancelled -> ErrorAction.ResetToIdle(error)
            is AppError.Unknown -> ErrorAction.ResetToIdle(error)
        }
    }

    suspend fun <T> withLlmRetry(block: suspend () -> T): Result<T> {
        repeat(MAX_LLM_RETRIES + 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == MAX_LLM_RETRIES) {
                    return Result.failure(e)
                }
                delay(LLM_RETRY_DELAY_MS)
            }
        }
        return Result.failure(IllegalStateException("Retry loop exhausted"))
    }
}

sealed class ErrorAction {
    data class ResetToIdle(val error: AppError) : ErrorAction()
    data class Retry(val delayMs: Long) : ErrorAction()
    data class FallbackToText(val error: AppError) : ErrorAction()
    object ShowPermissionRationale : ErrorAction()
}
