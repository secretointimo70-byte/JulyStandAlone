package com.july.offline.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstracción de dispatchers inyectable.
 * Las propiedades son `open` para permitir override en TestCoroutineDispatchers.
 */
@Singleton
open class CoroutineDispatchers @Inject constructor() {
    open val main: CoroutineDispatcher = Dispatchers.Main
    open val io: CoroutineDispatcher = Dispatchers.IO
    open val default: CoroutineDispatcher = Dispatchers.Default
    open val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
