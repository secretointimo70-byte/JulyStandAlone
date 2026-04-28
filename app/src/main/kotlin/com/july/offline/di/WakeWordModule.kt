package com.july.offline.di

import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.wakeword.OnnxWakeWordAdapter
import com.july.offline.wakeword.OnnxWakeWordConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WakeWordModule {

    @Binds
    @Singleton
    abstract fun bindWakeWordEngine(impl: OnnxWakeWordAdapter): WakeWordEngine

    companion object {

        /**
         * Configuración del motor de wake-word openWakeWord.
         *
         * Modelos requeridos en app/src/main/assets/:
         *   - wakeword_embedding.onnx  (~3 MB, compartido)
         *   - wakeword_model.onnx      (~50 KB, específico de la frase)
         *
         * Para obtener los modelos:
         *   1. Descargar embedding model:
         *      https://github.com/dscripka/openWakeWord/releases → embedding_model.onnx
         *      Renombrar a wakeword_embedding.onnx
         *
         *   2. Usar modelo preentrenado como placeholder (ej. "hey_jarvis"):
         *      https://github.com/dscripka/openWakeWord/releases → hey_jarvis_v0.1.onnx
         *      Renombrar a wakeword_model.onnx
         *
         *   3. O entrenar modelo personalizado "Oye July":
         *      pip install openwakeword
         *      python -m openwakeword.train --phrase "oye julio"
         */
        @Provides
        @Singleton
        fun provideOnnxWakeWordConfig(): OnnxWakeWordConfig = OnnxWakeWordConfig(
            embeddingModelAsset = "wakeword_embedding.onnx",
            wakeWordModelAsset = "wakeword_model.onnx",
            threshold = 0.5f,
            contextFrames = 16
        )
    }
}
