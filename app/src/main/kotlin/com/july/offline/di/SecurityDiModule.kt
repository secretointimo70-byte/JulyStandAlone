package com.july.offline.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// SecurityModule y todos sus dependientes usan @Inject constructor — Hilt los resuelve automáticamente.
@Module
@InstallIn(SingletonComponent::class)
object SecurityDiModule
