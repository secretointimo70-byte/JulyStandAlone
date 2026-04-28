package com.july.offline.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo vacío — AppLifecycleObserver se inyecta directamente en JulyApplication.
 * Declarado para documentar que el lifecycle es gestionado a nivel SingletonComponent.
 */
@Module
@InstallIn(SingletonComponent::class)
object LifecycleModule
