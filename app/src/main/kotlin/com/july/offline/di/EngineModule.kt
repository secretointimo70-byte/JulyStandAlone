package com.july.offline.di

import android.content.Context
import com.july.offline.ai.stt.WhisperConfig
import com.july.offline.ai.stt.WhisperSTTAdapter
import com.july.offline.ai.tts.AndroidTtsAdapter
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindSpeechToTextEngine(impl: WhisperSTTAdapter): SpeechToTextEngine

    /**
     * TTS: Android TTS del sistema (sin .so, sin modelos externos).
     * Para cambiar a Piper cuando estén disponibles los .so:
     *   1. Reemplazar AndroidTtsAdapter por PiperTTSAdapter aquí
     *   2. Restaurar providePiperConfig() en companion object
     */
    @Binds
    @Singleton
    abstract fun bindTextToSpeechEngine(impl: AndroidTtsAdapter): TextToSpeechEngine

    companion object {

        @Provides
        @Singleton
        fun provideWhisperConfig(
            @ApplicationContext context: Context
        ): WhisperConfig {
            val modelFile = resolveModelFile(context, "whisper-small.bin")
            return WhisperConfig(
                modelPath = modelFile.absolutePath,
                language = "es",
                threads = 4
            )
        }

        private fun resolveModelFile(context: Context, filename: String): File {
            val candidates = buildList {
                context.getExternalFilesDir(null)?.let { add(File(it, filename)) }
                add(File("/sdcard/Android/data/${context.packageName}/files/$filename"))
                add(File("/storage/emulated/0/Android/data/${context.packageName}/files/$filename"))
                context.externalCacheDir?.parentFile?.let { add(File(it, filename)) }
                add(File(context.filesDir, filename))
            }
            return candidates.firstOrNull { it.exists() } ?: candidates.last()
        }
    }
}
