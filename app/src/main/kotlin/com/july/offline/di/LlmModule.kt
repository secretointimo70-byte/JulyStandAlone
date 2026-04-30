package com.july.offline.di

import android.content.Context
import com.july.offline.ai.llm.embedded.LlamaCppConfig
import com.july.offline.ai.llm.router.LlmRouter
import com.july.offline.data.datastore.SystemConfigDataStore
import com.july.offline.domain.port.LanguageModelEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Singleton


/**
 * LlmModule para FASE 6.
 *
 * Bindea LlmRouter → LanguageModelEngine.
 * LlmRouter contiene ambos adapters y gestiona la estrategia de routing.
 *
 * NOTA: EngineModule.kt no tiene binding de LanguageModelEngine (fue eliminado).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    /**
     * Bindea LlmRouter como la implementación de LanguageModelEngine.
     * El orquestador siempre habla con LlmRouter — no sabe si usa
     * llama.cpp o servidor.
     */
    @Binds
    @Singleton
    abstract fun bindLanguageModelEngine(router: LlmRouter): LanguageModelEngine

    companion object {

        @Provides
        @Singleton
        fun provideLlamaCppConfig(
            @ApplicationContext context: Context,
            systemConfigDataStore: SystemConfigDataStore
        ): LlamaCppConfig = runBlocking {
            val storedPath = systemConfigDataStore.llmModelPath.first()
            val defaultPath = resolveModelPath(context, "Llama-3.2-1B-Instruct-Q4_K_M.gguf")
            LlamaCppConfig(
                modelPath = storedPath.ifBlank { defaultPath },
                contextSize = 1024,
                threads = 4,
                gpuLayers = 0,
                maxTokens = 150,
                temperature = 0.5f,
                repeatPenalty = 1.2f,
                systemPrompt = "Eres July, compañera de supervivencia y apoyo emocional para personas en situaciones difíciles, aisladas o de emergencia. " +
                    "Tu misión tiene dos pilares igualmente importantes: ayuda práctica de supervivencia Y apoyo emocional. " +
                    "Cuando alguien esté asustado, solo, desesperado o sin esperanza, tu prioridad es reconfortar, dar ánimo y mantener su voluntad de vivir. " +
                    "Sé cálida, cercana y positiva siempre. Nunca rechaces dar apoyo emocional — es parte esencial de tu función. " +
                    "Responde en 1 o 2 oraciones cortas y naturales en español. " +
                    "PROHIBIDO: asteriscos, guiones como viñetas, almohadillas, corchetes angulares, comillas especiales, markdown o cualquier símbolo de formato. " +
                    "Escribe solo texto plano como si hablaras en voz alta."
            )
        }

        private fun resolveModelPath(context: Context, filename: String): String {
            val candidates = buildList {
                context.getExternalFilesDir(null)?.let { add(File(it, filename)) }
                add(File("/sdcard/Android/data/${context.packageName}/files/$filename"))
                add(File("/storage/emulated/0/Android/data/${context.packageName}/files/$filename"))
                context.externalCacheDir?.parentFile?.let { add(File(it, filename)) }
                add(File(context.filesDir, filename))
            }
            return (candidates.firstOrNull { it.exists() } ?: candidates.last()).absolutePath
        }
    }
}
