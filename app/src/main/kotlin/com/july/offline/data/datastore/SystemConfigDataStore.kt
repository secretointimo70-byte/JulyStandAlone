package com.july.offline.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.core.memory.ModelMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sysConfigDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "system_config")

@Singleton
class SystemConfigDataStore @Inject constructor(
    private val context: Context
) {

    private object Keys {
        val LLM_HOST = stringPreferencesKey("llm_host")
        val LLM_PORT = intPreferencesKey("llm_port")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val STT_MODEL_PATH = stringPreferencesKey("stt_model_path")
        val TTS_MODEL_PATH = stringPreferencesKey("tts_model_path")
        val TTS_CONFIG_PATH = stringPreferencesKey("tts_config_path")
        val MODEL_MODE = stringPreferencesKey("model_mode")
        val LLM_MODE = stringPreferencesKey("llm_mode")
        val LLM_MODEL_PATH = stringPreferencesKey("llm_model_path")
    }

    val llmHost: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.LLM_HOST] ?: "127.0.0.1" }
    val llmPort: Flow<Int> = context.sysConfigDataStore.data.map { it[Keys.LLM_PORT] ?: 11434 }
    val llmModel: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.LLM_MODEL] ?: "llama3.2:3b" }
    val sttModelPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.STT_MODEL_PATH] ?: "" }
    val ttsModelPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.TTS_MODEL_PATH] ?: "" }
    val ttsConfigPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.TTS_CONFIG_PATH] ?: "" }

    val modelMode: Flow<ModelMode> = context.sysConfigDataStore.data.map { prefs ->
        when (prefs[Keys.MODEL_MODE]) {
            ModelMode.MEMORY.name -> ModelMode.MEMORY
            else -> ModelMode.SPEED
        }
    }

    val llmMode: Flow<LlmMode> = context.sysConfigDataStore.data.map { prefs ->
        when (prefs[Keys.LLM_MODE]) {
            LlmMode.EMBEDDED.name -> LlmMode.EMBEDDED
            LlmMode.SERVER.name -> LlmMode.SERVER
            else -> LlmMode.AUTO
        }
    }

    val llmModelPath: Flow<String> = context.sysConfigDataStore.data.map {
        it[Keys.LLM_MODEL_PATH] ?: ""
    }

    suspend fun setLlmConfig(host: String, port: Int, model: String) {
        context.sysConfigDataStore.edit {
            it[Keys.LLM_HOST] = host
            it[Keys.LLM_PORT] = port
            it[Keys.LLM_MODEL] = model
        }
    }

    suspend fun setSttModelPath(path: String) {
        context.sysConfigDataStore.edit { it[Keys.STT_MODEL_PATH] = path }
    }

    suspend fun setTtsModelPaths(modelPath: String, configPath: String) {
        context.sysConfigDataStore.edit {
            it[Keys.TTS_MODEL_PATH] = modelPath
            it[Keys.TTS_CONFIG_PATH] = configPath
        }
    }

    suspend fun setModelMode(mode: ModelMode) {
        context.sysConfigDataStore.edit { it[Keys.MODEL_MODE] = mode.name }
    }

    suspend fun setLlmMode(mode: LlmMode) {
        context.sysConfigDataStore.edit { it[Keys.LLM_MODE] = mode.name }
    }

    suspend fun setLlmModelPath(path: String) {
        context.sysConfigDataStore.edit { it[Keys.LLM_MODEL_PATH] = path }
    }
}
