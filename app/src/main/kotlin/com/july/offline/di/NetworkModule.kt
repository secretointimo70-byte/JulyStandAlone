package com.july.offline.di

import com.july.offline.ai.llm.LlmApiService
import com.july.offline.ai.llm.LlmServerConfig
import com.july.offline.data.network.LocalNetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLlmServerConfig(): LlmServerConfig = LlmServerConfig()

    @Provides
    @Singleton
    fun provideRetrofit(config: LlmServerConfig): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(
                LocalNetworkClient.create(
                    connectTimeoutSeconds = config.connectTimeoutSeconds,
                    readTimeoutSeconds = config.readTimeoutSeconds
                )
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideLlmApiService(retrofit: Retrofit): LlmApiService =
        retrofit.create(LlmApiService::class.java)
}
