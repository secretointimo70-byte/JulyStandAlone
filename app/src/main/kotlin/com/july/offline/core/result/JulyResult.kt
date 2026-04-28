package com.july.offline.core.result

import com.july.offline.core.error.AppError

sealed class JulyResult<out T> {
    data class Success<T>(val data: T) : JulyResult<T>()
    data class Failure(val error: AppError) : JulyResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data

    fun errorOrNull(): AppError? = (this as? Failure)?.error

    inline fun onSuccess(action: (T) -> Unit): JulyResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (AppError) -> Unit): JulyResult<T> {
        if (this is Failure) action(error)
        return this
    }

    inline fun <R> map(transform: (T) -> R): JulyResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    companion object {
        fun <T> success(data: T): JulyResult<T> = Success(data)
        fun failure(error: AppError): JulyResult<Nothing> = Failure(error)
    }
}
