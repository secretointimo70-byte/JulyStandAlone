package com.july.offline.di

import com.july.offline.audio.player.AudioPlayerAdapter
import com.july.offline.audio.recorder.AudioRecorderAdapter
import com.july.offline.audio.recorder.AudioRecorderConfig
import com.july.offline.audio.vad.VADConfig
import com.july.offline.audio.vad.VADProcessor
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.AudioCapturePort
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioCapturePort(impl: AudioRecorderAdapter): AudioCapturePort

    companion object {

        @Provides
        @Singleton
        fun provideAudioRecorderConfig(): AudioRecorderConfig = AudioRecorderConfig()

        @Provides
        @Singleton
        fun provideVADConfig(): VADConfig = VADConfig()

        @Provides
        @Singleton
        fun provideVADProcessor(config: VADConfig): VADProcessor = VADProcessor(config)

        @Provides
        @Singleton
        fun provideAudioPlayerAdapter(logger: DiagnosticsLogger): AudioPlayerAdapter =
            AudioPlayerAdapter(logger)
    }
}
