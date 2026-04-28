package com.july.offline.di

import com.july.offline.data.repository.SurvivalRepositoryImpl
import com.july.offline.domain.port.EmergencyTriggerPort
import com.july.offline.domain.port.SurvivalRepository
import com.july.offline.survival.NoOpEmergencyTriggerPort
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmergencyModule {

    @Binds @Singleton
    abstract fun bindEmergencyTriggerPort(impl: NoOpEmergencyTriggerPort): EmergencyTriggerPort

    @Binds @Singleton
    abstract fun bindSurvivalRepository(impl: SurvivalRepositoryImpl): SurvivalRepository
}
