package com.july.offline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.ai.llm.router.LlmRouter
import com.july.offline.ai.tts.AndroidTtsAdapter
import com.july.offline.ai.tts.TtsVoiceOption
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.ModelMode
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.data.datastore.SystemConfigDataStore
import com.july.offline.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: AppPreferencesDataStore,
    private val systemConfigDataStore: SystemConfigDataStore,
    private val modelMemoryManager: ModelMemoryManager,
    private val llmRouter: LlmRouter,
    private val ttsAdapter: AndroidTtsAdapter
) : ViewModel() {

    val settings: StateFlow<AppSettings> = combine(
        combine(
            preferencesDataStore.language,
            preferencesDataStore.ttsEnabled,
            preferencesDataStore.showTranscript,
            systemConfigDataStore.modelMode,
            systemConfigDataStore.llmMode
        ) { language, ttsEnabled, showTranscript, modelMode, llmMode ->
            AppSettings(language, ttsEnabled, showTranscript, modelMode, llmMode)
        },
        preferencesDataStore.ttsVoiceName
    ) { base, voiceName ->
        base.copy(ttsVoiceName = voiceName)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    val availableVoices: StateFlow<List<TtsVoiceOption>> = ttsAdapter.availableVoices

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setTtsEnabled(enabled) }
    }

    fun setShowTranscript(show: Boolean) {
        viewModelScope.launch { preferencesDataStore.setShowTranscript(show) }
    }

    fun setModelMode(mode: ModelMode) {
        viewModelScope.launch {
            systemConfigDataStore.setModelMode(mode)
            modelMemoryManager.currentMode = mode
        }
    }

    fun setLlmMode(mode: LlmMode) {
        viewModelScope.launch {
            systemConfigDataStore.setLlmMode(mode)
            llmRouter.currentMode = mode
        }
    }

    fun selectVoice(voiceName: String) {
        viewModelScope.launch {
            preferencesDataStore.setTtsVoiceName(voiceName)
            ttsAdapter.applyVoice(voiceName)
        }
    }
}
