package com.july.offline.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferencesDataStore @Inject constructor(
    private val context: Context
) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val SHOW_TRANSCRIPT = booleanPreferencesKey("show_transcript")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val EMERGENCY_MODULE_ENABLED = booleanPreferencesKey("emergency_module_enabled")
    }

    val language: Flow<String> = context.appPrefsDataStore.data.map { it[Keys.LANGUAGE] ?: "es" }
    val ttsEnabled: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.TTS_ENABLED] ?: true }
    val showTranscript: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.SHOW_TRANSCRIPT] ?: true }
    val wakeWordEnabled: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.WAKE_WORD_ENABLED] ?: false }
    val emergencyModuleEnabled: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.EMERGENCY_MODULE_ENABLED] ?: true }

    suspend fun setLanguage(lang: String) {
        context.appPrefsDataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.TTS_ENABLED] = enabled }
    }

    suspend fun setShowTranscript(show: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.SHOW_TRANSCRIPT] = show }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setEmergencyModuleEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.EMERGENCY_MODULE_ENABLED] = enabled }
    }
}
