package com.july.offline.core.memory

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.data.datastore.SystemConfigDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ModelMemoryManager actualizado para FASE 6.
 * Añade gestión del contexto JNI del LLM embebido (llama.cpp).
 *
 * El LLM embebido es el motor más pesado (~2GB). En modo MEMORY,
 * es el primero en liberarse al ir a background para maximizar
 * la RAM liberada.
 */
@Singleton
open class ModelMemoryManager @Inject constructor(
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers,
    private val systemConfigDataStore: SystemConfigDataStore
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    init {
        // Inicializar el modo desde DataStore al arranque
        scope.launch {
            try {
                currentMode = systemConfigDataStore.modelMode.first()
                logger.logDebug("ModelMemoryManager", "Initial mode loaded: $currentMode")
            } catch (e: Exception) {
                logger.logError("ModelMemoryManager", "Failed to load initial mode", e)
            }
        }
    }

    private val _sttState = MutableStateFlow(ModelState.UNLOADED)
    val sttState: StateFlow<ModelState> = _sttState.asStateFlow()

    private val _ttsState = MutableStateFlow(ModelState.UNLOADED)
    val ttsState: StateFlow<ModelState> = _ttsState.asStateFlow()

    // FASE 6 — estado del LLM embebido
    private val _llmState = MutableStateFlow(ModelState.UNLOADED)
    val llmState: StateFlow<ModelState> = _llmState.asStateFlow()

    @Volatile var currentMode: ModelMode = ModelMode.SPEED

    val sttMutex = Mutex()
    val ttsMutex = Mutex()
    val llmMutex = Mutex()

    private var sttReleaseCallback: (suspend () -> Unit)? = null
    private var ttsReleaseCallback: (suspend () -> Unit)? = null
    private var llmReleaseCallback: (suspend () -> Unit)? = null

    open fun registerSttReleaseCallback(callback: suspend () -> Unit) {
        sttReleaseCallback = callback
    }

    open fun registerTtsReleaseCallback(callback: suspend () -> Unit) {
        ttsReleaseCallback = callback
    }

    open fun registerLlmReleaseCallback(callback: suspend () -> Unit) {
        llmReleaseCallback = callback
    }

    open fun notifyModelLoaded(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.LOADED
            EngineType.TTS -> _ttsState.value = ModelState.LOADED
            EngineType.LLM -> _llmState.value = ModelState.LOADED
        }
        logger.logInfo("ModelMemoryManager", "${engine.name} loaded (mode: $currentMode)")
    }

    open fun notifyModelReleased(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.UNLOADED
            EngineType.TTS -> _ttsState.value = ModelState.UNLOADED
            EngineType.LLM -> _llmState.value = ModelState.UNLOADED
        }
        logger.logInfo("ModelMemoryManager", "${engine.name} released")
    }

    open fun notifyModelLoading(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.LOADING
            EngineType.TTS -> _ttsState.value = ModelState.LOADING
            EngineType.LLM -> _llmState.value = ModelState.LOADING
        }
    }

    /**
     * Libera todos los modelos JNI en modo MEMORY.
     * Orden de liberación: LLM primero (más pesado ~2GB),
     * luego STT (~466MB), luego TTS (~300MB).
     */
    open suspend fun releaseModelsIfMemoryMode() {
        if (currentMode != ModelMode.MEMORY) return

        logger.logInfo("ModelMemoryManager", "Releasing all models (MEMORY mode)")

        // LLM primero — mayor impacto en RAM
        if (_llmState.value == ModelState.LOADED) {
            _llmState.value = ModelState.RELEASING
            llmMutex.withLock { llmReleaseCallback?.invoke() }
        }

        if (_sttState.value == ModelState.LOADED) {
            _sttState.value = ModelState.RELEASING
            sttMutex.withLock { sttReleaseCallback?.invoke() }
        }

        if (_ttsState.value == ModelState.LOADED) {
            _ttsState.value = ModelState.RELEASING
            ttsMutex.withLock { ttsReleaseCallback?.invoke() }
        }
    }

    /**
     * Libera STT antes de inferencia LLM para evitar OOM kill.
     * STT y LLM juntos usan ~1.2GB; liberando STT (~466MB) el proceso sobrevive.
     * STT se recargará automáticamente en la siguiente transcripción.
     */
    open suspend fun releaseSttForInference() {
        if (_sttState.value != ModelState.LOADED) return
        _sttState.value = ModelState.RELEASING
        sttMutex.withLock { sttReleaseCallback?.invoke() }
        logger.logInfo("ModelMemoryManager", "STT released before LLM inference (~466MB freed)")
    }

    val isAnyModelLoading: Boolean
        get() = _sttState.value == ModelState.LOADING ||
                _ttsState.value == ModelState.LOADING ||
                _llmState.value == ModelState.LOADING

    val allModelsReady: Boolean
        get() = _sttState.value == ModelState.LOADED &&
                _ttsState.value == ModelState.LOADED &&
                _llmState.value == ModelState.LOADED
}

// FASE 6 — añade LLM
enum class EngineType { STT, TTS, LLM }
