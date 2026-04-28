package com.july.offline.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MemoryModule {
    // La configuración de ModelMemoryManager se ha movido a su bloque init
    // para evitar un ciclo de dependencias en Dagger.
}
