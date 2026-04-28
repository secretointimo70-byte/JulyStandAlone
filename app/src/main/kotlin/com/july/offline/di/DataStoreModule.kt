package com.july.offline.di

import android.content.Context
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.data.datastore.SystemConfigDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideAppPreferencesDataStore(
        @ApplicationContext context: Context
    ): AppPreferencesDataStore = AppPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideSystemConfigDataStore(
        @ApplicationContext context: Context
    ): SystemConfigDataStore = SystemConfigDataStore(context)
}
