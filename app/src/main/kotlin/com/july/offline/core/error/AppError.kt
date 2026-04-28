package com.july.offline.core.error

sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {

    data class Permission(
        override val message: String = "Microphone permission denied"
    ) : AppError(message)

    data class Stt(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Llm(
        override val message: String,
        override val cause: Throwable? = null,
        val retryable: Boolean = false
    ) : AppError(message, cause)

    data class Tts(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    object Cancelled : AppError("Operation cancelled by user")

    data class Unknown(
        override val message: String = "Unknown error",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
