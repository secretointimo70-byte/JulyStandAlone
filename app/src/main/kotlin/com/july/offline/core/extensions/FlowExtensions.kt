package com.july.offline.core.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.july.offline.core.error.AppError
import com.july.offline.core.result.JulyResult

fun <T> Flow<T>.asJulyResult(
    mapError: (Throwable) -> AppError = { AppError.Unknown(it.message ?: "Unknown", it) }
): Flow<JulyResult<T>> = map<T, JulyResult<T>> { JulyResult.success(it) }
    .catch { emit(JulyResult.failure(mapError(it))) }
