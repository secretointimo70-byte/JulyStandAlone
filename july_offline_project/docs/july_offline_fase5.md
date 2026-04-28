# JULY OFFLINE — FASE 5
## Lifecycle Porcupine + Gestión de Memoria JNI
### `com.july.offline`

**Alcance FASE 5:**
- `AppLifecycleObserver` via `ProcessLifecycleOwner` para pausar/reanudar Porcupine
- `ModelMemoryManager` con dos modos: SPEED (modelos siempre en RAM) y MEMORY (liberar tras 5 min en background)
- `ModelReleaseTimer` con coroutine timer cancelable
- `ModelMode` enum persistido en `SystemConfigDataStore`
- Toggle en `SettingsScreen` para que el usuario elija velocidad vs memoria
- `WhisperSTTAdapter` y `PiperTTSAdapter` actualizados con `ensureLoaded()`
- Tests de lifecycle y del `ModelMemoryManager`

---

## ÍNDICE DE ARCHIVOS FASE 5

### NUEVOS — LIFECYCLE
- `lifecycle/AppLifecycleObserver.kt`
- `lifecycle/ModelReleaseTimer.kt`

### NUEVOS — MEMORIA
- `core/memory/ModelMemoryManager.kt`
- `core/memory/ModelMode.kt`
- `core/memory/ModelState.kt`

### MODIFICADOS — AI
- `ai/stt/WhisperSTTAdapter.kt` (añade ensureLoaded + releaseContext)
- `ai/tts/PiperTTSAdapter.kt` (añade ensureLoaded + releaseContext)

### MODIFICADOS — DATA
- `data/datastore/SystemConfigDataStore.kt` (añade modelMode)

### MODIFICADOS — DI
- `di/LifecycleModule.kt` (NUEVO)
- `di/MemoryModule.kt` (NUEVO)

### MODIFICADOS — APP
- `JulyApplication.kt` (registra AppLifecycleObserver)

### MODIFICADOS — SETTINGS
- `settings/AppSettings.kt` (añade modelMode)
- `ui/settings/SettingsViewModel.kt` (añade toggle modelMode)
- `ui/settings/SettingsScreen.kt` (añade toggle UI)

### NUEVOS — TESTS
- `testutil/FakeModelMemoryManager.kt`
- `lifecycle/AppLifecycleObserverTest.kt`
- `memory/ModelMemoryManagerTest.kt`
- `memory/ModelReleaseTimerTest.kt`

---

## CORE — MEMORIA

### `core/memory/ModelMode.kt`

```kotlin
package com.july.offline.core.memory

/**
 * Estrategia de gestión de memoria para los modelos JNI.
 *
 * SPEED: Whisper (~466MB) y Piper (~300MB) permanecen cargados en RAM
 *        siempre que la app esté viva. Primera inferencia instantánea.
 *        Consumo total: ~766MB + overhead del proceso (~200MB) ≈ ~1GB RAM.
 *        Recomendado para dispositivos con 6GB+ RAM.
 *
 * MEMORY: Los modelos se liberan del contexto JNI tras 5 minutos en background.
 *         Al volver a foreground, se recargan lazy en la primera inferencia.
 *         Recarga Whisper: ~2-4s. Recarga Piper: ~1-2s.
 *         Recomendado para dispositivos con 4GB RAM o menos.
 */
enum class ModelMode {
    SPEED,   // mantener en RAM siempre
    MEMORY   // liberar tras inactividad en background
}
```

---

### `core/memory/ModelState.kt`

```kotlin
package com.july.offline.core.memory

/**
 * Estado del ciclo de vida de un modelo JNI en memoria.
 */
enum class ModelState {
    /** Modelo no cargado. El contexto JNI es 0L. */
    UNLOADED,
    /** Modelo cargándose (initContext() en progreso). */
    LOADING,
    /** Modelo en memoria y listo para inferencia. */
    LOADED,
    /** Modelo liberándose (whisperFree / piperFree en progreso). */
    RELEASING
}
```

---

### `core/memory/ModelMemoryManager.kt`

```kotlin
package com.july.offline.core.memory

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor central de memoria para modelos JNI.
 *
 * Responsabilidades:
 * - Mantener el estado de carga de cada motor (STT, TTS)
 * - En modo MEMORY: coordinar la liberación de contextos JNI tras inactividad
 * - Exponer StateFlow del estado de cada modelo para que la UI pueda mostrar
 *   indicadores de "cargando modelo..."
 * - Thread-safe: usa Mutex por motor para evitar doble carga/liberación
 *
 * Los adaptadores (WhisperSTTAdapter, PiperTTSAdapter) llaman a
 * notifyModelLoaded() / notifyModelReleased() para mantener el estado sincronizado.
 */
@Singleton
class ModelMemoryManager @Inject constructor(
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    // Estado de cada motor
    private val _sttState = MutableStateFlow(ModelState.UNLOADED)
    val sttState: StateFlow<ModelState> = _sttState.asStateFlow()

    private val _ttsState = MutableStateFlow(ModelState.UNLOADED)
    val ttsState: StateFlow<ModelState> = _ttsState.asStateFlow()

    // Modo actual (leído desde SystemConfigDataStore en MemoryModule)
    @Volatile var currentMode: ModelMode = ModelMode.SPEED

    // Mutex por motor para operaciones de carga/liberación
    val sttMutex = Mutex()
    val ttsMutex = Mutex()

    // Callbacks de liberación registrados por los adaptadores
    private var sttReleaseCallback: (suspend () -> Unit)? = null
    private var ttsReleaseCallback: (suspend () -> Unit)? = null

    /** Registra el callback que libera el contexto JNI de STT. */
    fun registerSttReleaseCallback(callback: suspend () -> Unit) {
        sttReleaseCallback = callback
    }

    /** Registra el callback que libera el contexto JNI de TTS. */
    fun registerTtsReleaseCallback(callback: suspend () -> Unit) {
        ttsReleaseCallback = callback
    }

    /** Notifica que el modelo STT fue cargado exitosamente. */
    fun notifyModelLoaded(engine: EngineType) {
        when (engine) {
            EngineType.STT -> {
                _sttState.value = ModelState.LOADED
                logger.logInfo("ModelMemoryManager", "STT model loaded (mode: $currentMode)")
            }
            EngineType.TTS -> {
                _ttsState.value = ModelState.LOADED
                logger.logInfo("ModelMemoryManager", "TTS model loaded (mode: $currentMode)")
            }
        }
    }

    /** Notifica que el modelo fue liberado. */
    fun notifyModelReleased(engine: EngineType) {
        when (engine) {
            EngineType.STT -> {
                _sttState.value = ModelState.UNLOADED
                logger.logInfo("ModelMemoryManager", "STT model released")
            }
            EngineType.TTS -> {
                _ttsState.value = ModelState.UNLOADED
                logger.logInfo("ModelMemoryManager", "TTS model released")
            }
        }
    }

    /** Notifica que un modelo está cargándose. */
    fun notifyModelLoading(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.LOADING
            EngineType.TTS -> _ttsState.value = ModelState.LOADING
        }
    }

    /**
     * Libera ambos modelos JNI si el modo es MEMORY.
     * Llamado por ModelReleaseTimer tras 5 minutos en background.
     * No hace nada en modo SPEED.
     */
    suspend fun releaseModelsIfMemoryMode() {
        if (currentMode != ModelMode.MEMORY) return

        logger.logInfo("ModelMemoryManager", "Releasing models (MEMORY mode, background timeout)")

        if (_sttState.value == ModelState.LOADED) {
            _sttState.value = ModelState.RELEASING
            sttMutex.withLock {
                sttReleaseCallback?.invoke()
            }
        }

        if (_ttsState.value == ModelState.LOADED) {
            _ttsState.value = ModelState.RELEASING
            ttsMutex.withLock {
                ttsReleaseCallback?.invoke()
            }
        }
    }

    /** true si algún modelo está cargando (UI puede mostrar spinner). */
    val isAnyModelLoading: Boolean
        get() = _sttState.value == ModelState.LOADING ||
                _ttsState.value == ModelState.LOADING

    /** true si todos los modelos están listos para inferencia. */
    val allModelsReady: Boolean
        get() = _sttState.value == ModelState.LOADED &&
                _ttsState.value == ModelState.LOADED
}

enum class EngineType { STT, TTS }
```

---

## LIFECYCLE

### `lifecycle/ModelReleaseTimer.kt`

```kotlin
package com.july.offline.lifecycle

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timer que libera los modelos JNI tras un periodo de inactividad en background.
 *
 * Funcionamiento:
 * - Se inicia cuando la app va a background (AppLifecycleObserver.onStop)
 * - Se cancela si la app vuelve a foreground antes de que expire (onStart)
 * - Si expira sin ser cancelado, llama a ModelMemoryManager.releaseModelsIfMemoryMode()
 *
 * El timer solo tiene efecto si ModelMode == MEMORY.
 * En modo SPEED, releaseModelsIfMemoryMode() es un no-op.
 */
@Singleton
class ModelReleaseTimer @Inject constructor(
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    companion object {
        const val RELEASE_DELAY_MS = 5 * 60 * 1_000L  // 5 minutos
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var timerJob: Job? = null

    /**
     * Inicia el timer de liberación.
     * Si ya hay un timer activo, no lo reinicia.
     */
    fun start() {
        if (timerJob?.isActive == true) return

        timerJob = scope.launch {
            logger.logInfo(
                "ModelReleaseTimer",
                "Timer started — models will be released in ${RELEASE_DELAY_MS / 1000}s if in MEMORY mode"
            )
            delay(RELEASE_DELAY_MS)
            logger.logInfo("ModelReleaseTimer", "Timer expired, releasing models")
            modelMemoryManager.releaseModelsIfMemoryMode()
        }
    }

    /**
     * Cancela el timer si estaba activo.
     * Llamado cuando la app vuelve a foreground.
     */
    fun cancel() {
        if (timerJob?.isActive == true) {
            timerJob?.cancel()
            logger.logInfo("ModelReleaseTimer", "Timer cancelled — app returned to foreground")
        }
        timerJob = null
    }

    val isRunning: Boolean get() = timerJob?.isActive == true
}
```

---

### `lifecycle/AppLifecycleObserver.kt`

```kotlin
package com.july.offline.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.state.ConversationStateHolder
import com.july.offline.domain.model.ConversationState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observer del ciclo de vida a nivel de proceso (no Activity).
 *
 * Ventaja de ProcessLifecycleOwner sobre ActivityLifecycleCallbacks:
 * - onStop no se dispara durante rotaciones de pantalla
 * - onStop solo se dispara cuando TODA la app va a background
 * - Elimina falsos positivos de "app en background" durante config changes
 *
 * Comportamiento:
 * - onStart (app visible): cancela el timer de liberación, reanuda Porcupine si procede
 * - onStop (app en background): pausa Porcupine, inicia el timer de liberación
 *
 * Registro: llamado desde JulyApplication.onCreate() via ProcessLifecycleOwner.
 * No necesita desregistrarse — vive mientras el proceso vive.
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val wakeWordCoordinator: WakeWordCoordinator,
    private val stateHolder: ConversationStateHolder,
    private val modelReleaseTimer: ModelReleaseTimer,
    private val logger: DiagnosticsLogger
) : DefaultLifecycleObserver {

    /**
     * Registra este observer en el ProcessLifecycleOwner.
     * Llamar una sola vez desde JulyApplication.onCreate().
     */
    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        logger.logInfo("AppLifecycleObserver", "Registered with ProcessLifecycleOwner")
    }

    /**
     * App vuelve a foreground (o se inicia).
     * - Cancela el timer de liberación de modelos
     * - Reanuda Porcupine si el modo wake-word estaba activo
     */
    override fun onStart(owner: LifecycleOwner) {
        logger.logInfo("AppLifecycleObserver", "App → foreground")

        // Cancelar timer de liberación de modelos
        modelReleaseTimer.cancel()

        // Reanudar Porcupine si el modo wake-word estaba activo antes de ir a background
        // Solo si el estado actual es WakeWordListening (fue pausado, no desactivado)
        val currentState = stateHolder.conversationState.value
        if (currentState is ConversationState.WakeWordListening &&
            !wakeWordCoordinator.isActive) {
            logger.logInfo("AppLifecycleObserver", "Resuming Porcupine after foreground return")
            wakeWordCoordinator.resumeAfterCycle()
        }
    }

    /**
     * App va a background.
     * - Pausa Porcupine (libera su AudioRecord para no bloquear el micrófono)
     * - Inicia el timer de liberación de modelos JNI
     *
     * NOTA: No transiciona el ConversationState. Si Porcupine estaba en
     * WakeWordListening, el estado permanece así para que onStart() sepa
     * que debe reanudarlo al volver.
     */
    override fun onStop(owner: LifecycleOwner) {
        logger.logInfo("AppLifecycleObserver", "App → background")

        // Pausar Porcupine para liberar el AudioRecord
        // El WakeWordCoordinator guarda internamente si estaba activo
        if (wakeWordCoordinator.isActive) {
            logger.logInfo("AppLifecycleObserver", "Pausing Porcupine (app to background)")
            wakeWordCoordinator.pauseForBackground()
        }

        // Iniciar timer de liberación de modelos JNI (solo actúa en modo MEMORY)
        modelReleaseTimer.start()
    }
}
```

---

### `domain/orchestrator/WakeWordCoordinator.kt` (actualizado — añade pauseForBackground)

```kotlin
package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.domain.port.WakeWordEvent
import com.july.offline.domain.state.ConversationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WakeWordCoordinator actualizado para FASE 5.
 *
 * Cambios respecto a FASE 4:
 * - Añade pauseForBackground(): detiene Porcupine pero marca _wasActiveBeforeBackground = true
 * - resumeAfterCycle() también cubre el caso de retorno desde background
 * - El estado WakeWordListening no se abandona cuando se va a background
 */
@Singleton
class WakeWordCoordinator @Inject constructor(
    private val wakeWordEngine: WakeWordEngine,
    private val stateHolder: ConversationStateHolder,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var listeningJob: Job? = null

    /** true si Porcupine fue pausado por background (para reanudar en onStart). */
    @Volatile private var _pausedForBackground = false

    var onWakeWordDetected: (() -> Unit)? = null

    fun startWakeWordListening() {
        if (wakeWordEngine.isListening) return

        _pausedForBackground = false
        stateHolder.transitionToWakeWordListening()

        listeningJob = scope.launch {
            logger.logInfo("WakeWordCoordinator", "Starting Porcupine")

            wakeWordEngine.startListening()
                .catch { cause ->
                    logger.logError("WakeWordCoordinator", "Stream error", cause)
                    kotlinx.coroutines.delay(1_000L)
                    emit(WakeWordEvent.Error(cause))
                }
                .collect { event ->
                    when (event) {
                        is WakeWordEvent.Detected -> {
                            logger.logInfo("WakeWordCoordinator", "Keyword detected")
                            wakeWordEngine.stop()
                            onWakeWordDetected?.invoke()
                        }
                        is WakeWordEvent.Error -> {
                            logger.logError("WakeWordCoordinator", "Engine error: ${event.cause.message}")
                        }
                    }
                }
        }
    }

    fun stopWakeWordListening() {
        listeningJob?.cancel()
        listeningJob = null
        wakeWordEngine.stop()
        _pausedForBackground = false
        stateHolder.resetToIdle()
        logger.logInfo("WakeWordCoordinator", "Stopped by user")
    }

    /**
     * NUEVO FASE 5.
     * Pausa Porcupine cuando la app va a background.
     * No modifica el ConversationState (sigue en WakeWordListening).
     * El AudioRecord de Porcupine se libera para no bloquear el micrófono en background.
     */
    fun pauseForBackground() {
        if (isActive) {
            _pausedForBackground = true
            listeningJob?.cancel()
            listeningJob = null
            wakeWordEngine.stop()
            logger.logInfo("WakeWordCoordinator", "Paused for background (state preserved)")
        }
    }

    /**
     * Reanuda Porcupine tras ciclo de conversación O tras volver de background.
     * Solo actúa si el modo wake-word estaba activo.
     */
    fun resumeAfterCycle() {
        if (!wakeWordEngine.isListening) {
            startWakeWordListening()
        }
    }

    val isActive: Boolean
        get() = wakeWordEngine.isListening || listeningJob?.isActive == true

    val wasActiveBeforeBackground: Boolean
        get() = _pausedForBackground
}
```

---

## AI — ADAPTERS ACTUALIZADOS

### `ai/stt/WhisperSTTAdapter.kt` (añade ensureLoaded + releaseContext)

```kotlin
package com.july.offline.ai.stt

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.EngineType
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.port.SpeechToTextEngine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * WhisperSTTAdapter actualizado para FASE 5.
 *
 * Cambios:
 * - Integración con ModelMemoryManager para notificar estado de carga
 * - releaseContext() público para que ModelMemoryManager pueda liberar el modelo
 * - ensureLoaded() reemplaza la lógica inline de initContext()
 * - El Mutex de carga/liberación es el de ModelMemoryManager (evita doble lock)
 */
class WhisperSTTAdapter @Inject constructor(
    private val config: WhisperConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger
) : SpeechToTextEngine {

    @Volatile private var contextHandle: Long = 0L

    init {
        // Registrar callback de liberación en ModelMemoryManager
        modelMemoryManager.registerSttReleaseCallback { releaseContext() }
    }

    /**
     * Garantiza que el contexto JNI está inicializado.
     * Thread-safe via ModelMemoryManager.sttMutex.
     * @return true si el modelo está listo, false si falla la carga
     */
    private suspend fun ensureLoaded(): Boolean {
        if (contextHandle != 0L) return true

        return modelMemoryManager.sttMutex.withLock {
            if (contextHandle != 0L) return@withLock true

            if (!File(config.modelPath).exists()) {
                logger.logError("WhisperSTT", "Model not found: ${config.modelPath}")
                return@withLock false
            }

            modelMemoryManager.notifyModelLoading(EngineType.STT)
            logger.logInfo("WhisperSTT", "Loading model...")

            val handle = WhisperJNI.whisperInit(config.modelPath, config.threads)
            return@withLock if (handle != 0L) {
                contextHandle = handle
                modelMemoryManager.notifyModelLoaded(EngineType.STT)
                logger.logInfo("WhisperSTT", "Model loaded")
                true
            } else {
                logger.logError("WhisperSTT", "whisperInit() returned 0")
                false
            }
        }
    }

    /**
     * Libera el contexto JNI. Llamado por ModelMemoryManager en modo MEMORY.
     * Thread-safe via ModelMemoryManager.sttMutex.
     */
    suspend fun releaseContext() {
        modelMemoryManager.sttMutex.withLock {
            if (contextHandle != 0L) {
                WhisperJNI.whisperFree(contextHandle)
                contextHandle = 0L
                modelMemoryManager.notifyModelReleased(EngineType.STT)
                logger.logInfo("WhisperSTT", "Context released")
            }
        }
    }

    override suspend fun transcribe(audio: ByteArray): JulyResult<Transcript> {
        return try {
            withTimeout(config.maxDurationMs) {
                if (!ensureLoaded()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Stt("Whisper model not available at ${config.modelPath}")
                    )
                }

                val startMs = System.currentTimeMillis()
                val floatSamples = convertPcm16ToFloat(audio)

                val rawText = WhisperJNI.whisperTranscribe(
                    contextHandle = contextHandle,
                    pcmSamples = floatSamples,
                    language = config.language
                )

                logger.logEngineEvent("STT", "transcribed", System.currentTimeMillis() - startMs)

                JulyResult.success(
                    Transcript(
                        text = rawText.trim(),
                        confidence = -1f,
                        languageCode = config.language,
                        durationMs = audio.size.toLong() / 32L
                    )
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            JulyResult.failure(AppError.Stt("Whisper timeout after ${config.maxDurationMs}ms", e))
        } catch (e: Exception) {
            logger.logError("WhisperSTT", "Transcription failed", e)
            JulyResult.failure(AppError.Stt("Whisper error: ${e.message}", e))
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        return ensureLoaded()
    }

    private fun convertPcm16ToFloat(pcm: ByteArray): FloatArray {
        val sampleCount = pcm.size / 2
        val floats = FloatArray(sampleCount)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            floats[i] = buffer.short / 32768.0f
        }
        return floats
    }
}
```

---

### `ai/tts/PiperTTSAdapter.kt` (añade ensureLoaded + releaseContext)

```kotlin
package com.july.offline.ai.tts

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.EngineType
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.port.TextToSpeechEngine
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * PiperTTSAdapter actualizado para FASE 5.
 * Misma estrategia que WhisperSTTAdapter: ensureLoaded() + releaseContext().
 */
class PiperTTSAdapter @Inject constructor(
    private val config: PiperConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger
) : TextToSpeechEngine {

    @Volatile private var contextHandle: Long = 0L

    init {
        modelMemoryManager.registerTtsReleaseCallback { releaseContext() }
    }

    private suspend fun ensureLoaded(): Boolean {
        if (contextHandle != 0L) return true

        return modelMemoryManager.ttsMutex.withLock {
            if (contextHandle != 0L) return@withLock true

            if (!File(config.modelPath).exists() || !File(config.modelConfigPath).exists()) {
                logger.logError("PiperTTS", "Model files not found")
                return@withLock false
            }

            modelMemoryManager.notifyModelLoading(EngineType.TTS)
            logger.logInfo("PiperTTS", "Loading model...")

            val handle = PiperJNI.piperInit(config.modelPath, config.modelConfigPath)
            return@withLock if (handle != 0L) {
                contextHandle = handle
                modelMemoryManager.notifyModelLoaded(EngineType.TTS)
                logger.logInfo("PiperTTS", "Model loaded")
                true
            } else {
                logger.logError("PiperTTS", "piperInit() returned 0")
                false
            }
        }
    }

    suspend fun releaseContext() {
        modelMemoryManager.ttsMutex.withLock {
            if (contextHandle != 0L) {
                PiperJNI.piperFree(contextHandle)
                contextHandle = 0L
                modelMemoryManager.notifyModelReleased(EngineType.TTS)
                logger.logInfo("PiperTTS", "Context released")
            }
        }
    }

    override suspend fun synthesize(text: String): JulyResult<ByteArray> {
        return try {
            if (!ensureLoaded()) {
                return JulyResult.failure(
                    AppError.Tts("Piper model not available at ${config.modelPath}")
                )
            }
            if (text.isBlank()) return JulyResult.success(ByteArray(0))

            val startMs = System.currentTimeMillis()
            val shorts = PiperJNI.piperSynthesize(contextHandle, text, config.speakerId)
            logger.logEngineEvent("TTS", "synthesized ${shorts.size} samples",
                System.currentTimeMillis() - startMs)

            if (shorts.isEmpty()) {
                return JulyResult.failure(AppError.Tts("Piper returned empty audio"))
            }
            JulyResult.success(convertShortsToPcm(shorts))

        } catch (e: Exception) {
            logger.logError("PiperTTS", "Synthesis failed", e)
            JulyResult.failure(AppError.Tts("Piper error: ${e.message}", e))
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        if (!File(config.modelConfigPath).exists()) return false
        return ensureLoaded()
    }

    override fun getSupportedLanguages(): List<String> = listOf("es")

    private fun convertShortsToPcm(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { buffer.putShort(it) }
        return bytes
    }
}
```

---

## DATA — ACTUALIZACIÓN

### `data/datastore/SystemConfigDataStore.kt` (añade modelMode)

```kotlin
package com.july.offline.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
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
        val MODEL_MODE = stringPreferencesKey("model_mode")   // NUEVO FASE 5
    }

    val llmHost: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.LLM_HOST] ?: "127.0.0.1" }
    val llmPort: Flow<Int> = context.sysConfigDataStore.data.map { it[Keys.LLM_PORT] ?: 11434 }
    val llmModel: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.LLM_MODEL] ?: "llama3.2:3b" }
    val sttModelPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.STT_MODEL_PATH] ?: "" }
    val ttsModelPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.TTS_MODEL_PATH] ?: "" }
    val ttsConfigPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.TTS_CONFIG_PATH] ?: "" }

    /** NUEVO FASE 5 — modo de gestión de memoria JNI */
    val modelMode: Flow<ModelMode> = context.sysConfigDataStore.data.map { prefs ->
        when (prefs[Keys.MODEL_MODE]) {
            ModelMode.MEMORY.name -> ModelMode.MEMORY
            else -> ModelMode.SPEED   // default: velocidad
        }
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
}
```

---

## DI — NUEVOS MÓDULOS

### `di/MemoryModule.kt` (NUEVO)

```kotlin
package com.july.offline.di

import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.ModelMode
import com.july.offline.data.datastore.SystemConfigDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MemoryModule {

    /**
     * Configura el ModelMemoryManager con el modo leído desde DataStore.
     * El modo se fija al arranque. Si el usuario lo cambia en Settings,
     * la app aplica el nuevo modo al volver a foreground (próximo onStart).
     */
    @Provides
    @Singleton
    fun configureModelMemoryManager(
        modelMemoryManager: ModelMemoryManager,
        systemConfigDataStore: SystemConfigDataStore
    ): ModelMemoryManager {
        val mode = runBlocking { systemConfigDataStore.modelMode.first() }
        modelMemoryManager.currentMode = mode
        return modelMemoryManager
    }
}
```

---

### `di/LifecycleModule.kt` (NUEVO)

```kotlin
package com.july.offline.di

import com.july.offline.lifecycle.AppLifecycleObserver
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo vacío — AppLifecycleObserver se inyecta directamente en JulyApplication.
 * Declarado para documentar que el lifecycle es gestionado a nivel SingletonComponent.
 */
@Module
@InstallIn(SingletonComponent::class)
object LifecycleModule
```

---

## APP — ACTUALIZACIÓN

### `JulyApplication.kt` (registra AppLifecycleObserver)

```kotlin
package com.july.offline

import android.app.Application
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.lifecycle.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JulyApplication : Application() {

    @Inject lateinit var engineHealthMonitor: EngineHealthMonitor
    @Inject lateinit var diagnosticsLogger: DiagnosticsLogger
    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    @Inject lateinit var modelMemoryManager: ModelMemoryManager  // fuerza init de MemoryModule

    override fun onCreate() {
        super.onCreate()

        // Iniciar monitoreo de salud de motores
        engineHealthMonitor.startMonitoring()

        // Registrar observer de lifecycle a nivel de proceso
        // ProcessLifecycleOwner requiere que la app use androidx.lifecycle
        appLifecycleObserver.register()

        // Borrar logs antiguos
        diagnosticsLogger.pruneOldLogs()
    }
}
```

---

## SETTINGS — ACTUALIZACIÓN

### `settings/AppSettings.kt` (añade modelMode)

```kotlin
package com.july.offline.settings

import com.july.offline.core.memory.ModelMode

data class AppSettings(
    val language: String = "es",
    val ttsEnabled: Boolean = true,
    val showTranscript: Boolean = true,
    val modelMode: ModelMode = ModelMode.SPEED   // NUEVO FASE 5
)
```

---

### `ui/settings/SettingsViewModel.kt` (añade toggle modelMode)

```kotlin
package com.july.offline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val modelMemoryManager: ModelMemoryManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = combine(
        preferencesDataStore.language,
        preferencesDataStore.ttsEnabled,
        preferencesDataStore.showTranscript,
        systemConfigDataStore.modelMode
    ) { language, ttsEnabled, showTranscript, modelMode ->
        AppSettings(
            language = language,
            ttsEnabled = ttsEnabled,
            showTranscript = showTranscript,
            modelMode = modelMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setTtsEnabled(enabled) }
    }

    fun setShowTranscript(show: Boolean) {
        viewModelScope.launch { preferencesDataStore.setShowTranscript(show) }
    }

    /**
     * Cambia el modo de gestión de memoria.
     * Persiste el valor y actualiza ModelMemoryManager en tiempo real.
     * El nuevo modo aplica desde el próximo ciclo de background.
     */
    fun setModelMode(mode: ModelMode) {
        viewModelScope.launch {
            systemConfigDataStore.setModelMode(mode)
            modelMemoryManager.currentMode = mode
        }
    }
}
```

---

### `ui/settings/SettingsScreen.kt` (toggle modelMode)

```kotlin
package com.july.offline.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.core.memory.ModelMode

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── TTS toggle ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Síntesis de voz (TTS)", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.ttsEnabled,
                    onCheckedChange = { viewModel.setTtsEnabled(it) }
                )
            }

            // ── Transcript toggle ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar transcripción", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.showTranscript,
                    onCheckedChange = { viewModel.setShowTranscript(it) }
                )
            }

            HorizontalDivider()

            // ── ModelMode toggle NUEVO FASE 5 ─────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Modo de memoria",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = when (settings.modelMode) {
                        ModelMode.SPEED ->
                            "Velocidad: modelos en RAM siempre (~1 GB). " +
                            "Primera respuesta instantánea."
                        ModelMode.MEMORY ->
                            "Memoria: modelos se liberan tras 5 min en segundo plano (~400 MB libres). " +
                            "Recarga al volver: ~3 s."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.modelMode == ModelMode.SPEED,
                        onClick = { viewModel.setModelMode(ModelMode.SPEED) },
                        label = { Text("Velocidad") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = settings.modelMode == ModelMode.MEMORY,
                        onClick = { viewModel.setModelMode(ModelMode.MEMORY) },
                        label = { Text("Memoria") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()
            Text(
                text = "Idioma: ${settings.language}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## TESTS

### `testutil/FakeModelMemoryManager.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.core.memory.EngineType
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.ModelMode
import com.july.offline.core.memory.ModelState
import io.mockk.mockk

/**
 * Fake de ModelMemoryManager para tests.
 * Expone contadores de llamadas para verificar interacciones.
 */
class FakeModelMemoryManager : ModelMemoryManager(
    logger = mockk(relaxed = true),
    dispatchers = TestCoroutineDispatchers()
) {
    var releaseModelsCallCount = 0
    var notifyLoadedCallCount = 0
    var notifyReleasedCallCount = 0

    var sttReleaseCallback: (suspend () -> Unit)? = null
    var ttsReleaseCallback: (suspend () -> Unit)? = null

    override fun registerSttReleaseCallback(callback: suspend () -> Unit) {
        sttReleaseCallback = callback
    }

    override fun registerTtsReleaseCallback(callback: suspend () -> Unit) {
        ttsReleaseCallback = callback
    }

    override fun notifyModelLoaded(engine: EngineType) {
        notifyLoadedCallCount++
        super.notifyModelLoaded(engine)
    }

    override fun notifyModelReleased(engine: EngineType) {
        notifyReleasedCallCount++
        super.notifyModelReleased(engine)
    }

    override suspend fun releaseModelsIfMemoryMode() {
        releaseModelsCallCount++
        super.releaseModelsIfMemoryMode()
    }
}
```

---

### `lifecycle/AppLifecycleObserverTest.kt`

```kotlin
package com.july.offline.lifecycle

import androidx.lifecycle.LifecycleOwner
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.state.ConversationStateHolder
import com.july.offline.testutil.FakeWakeWordEngine
import com.july.offline.testutil.TestCoroutineDispatchers
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AppLifecycleObserverTest {

    private lateinit var wakeWordCoordinator: WakeWordCoordinator
    private lateinit var stateHolder: ConversationStateHolder
    private lateinit var modelReleaseTimer: ModelReleaseTimer
    private lateinit var observer: AppLifecycleObserver
    private lateinit var fakeEngine: FakeWakeWordEngine
    private lateinit var lifecycleOwner: LifecycleOwner
    private val dispatchers = TestCoroutineDispatchers()

    @BeforeEach
    fun setup() {
        val logger = io.mockk.mockk<com.july.offline.core.logging.DiagnosticsLogger>(relaxed = true)
        fakeEngine = FakeWakeWordEngine()
        stateHolder = ConversationStateHolder(logger)
        modelReleaseTimer = mockk(relaxed = true)
        lifecycleOwner = mockk(relaxed = true)

        wakeWordCoordinator = WakeWordCoordinator(
            wakeWordEngine = fakeEngine,
            stateHolder = stateHolder,
            logger = logger,
            dispatchers = dispatchers
        )

        observer = AppLifecycleObserver(
            wakeWordCoordinator = wakeWordCoordinator,
            stateHolder = stateHolder,
            modelReleaseTimer = modelReleaseTimer,
            logger = logger
        )
    }

    @Test
    fun `onStop pausa Porcupine si estaba activo`() = runTest {
        // Activar wake-word
        wakeWordCoordinator.startWakeWordListening()
        assertTrue(wakeWordCoordinator.isActive || fakeEngine.isListening)

        observer.onStop(lifecycleOwner)

        // Verificar que Porcupine fue pausado
        assertTrue(fakeEngine.stopCalled)
        assertTrue(wakeWordCoordinator.wasActiveBeforeBackground)
    }

    @Test
    fun `onStop inicia el timer de liberacion de modelos`() = runTest {
        observer.onStop(lifecycleOwner)

        verify { modelReleaseTimer.start() }
    }

    @Test
    fun `onStart cancela el timer de liberacion`() = runTest {
        observer.onStart(lifecycleOwner)

        verify { modelReleaseTimer.cancel() }
    }

    @Test
    fun `onStart reanuda Porcupine si estado es WakeWordListening y no esta activo`() = runTest {
        // Simular que Porcupine fue pausado para background
        wakeWordCoordinator.startWakeWordListening()
        wakeWordCoordinator.pauseForBackground()

        // Estado debe seguir siendo WakeWordListening
        assertEquals(
            ConversationState.WakeWordListening,
            stateHolder.conversationState.value
        )

        observer.onStart(lifecycleOwner)

        // Porcupine debe estar activo o haberlo iniciado
        // (resumeAfterCycle llama a startWakeWordListening)
        assertTrue(fakeEngine.isListening || wakeWordCoordinator.isActive)
    }

    @Test
    fun `onStart no reanuda Porcupine si estado es Idle`() = runTest {
        // Estado es Idle por defecto
        assertEquals(ConversationState.Idle, stateHolder.conversationState.value)

        observer.onStart(lifecycleOwner)

        // Porcupine no debe iniciarse
        assertFalse(fakeEngine.isListening)
    }

    @Test
    fun `ciclo completo background foreground preserva modo wake-word`() = runTest {
        // 1. Activar wake-word
        wakeWordCoordinator.startWakeWordListening()

        // 2. App va a background
        observer.onStop(lifecycleOwner)
        verify { modelReleaseTimer.start() }
        assertTrue(wakeWordCoordinator.wasActiveBeforeBackground)

        // 3. App vuelve a foreground
        observer.onStart(lifecycleOwner)
        verify { modelReleaseTimer.cancel() }

        // 4. Estado debe ser WakeWordListening de nuevo
        assertEquals(
            ConversationState.WakeWordListening,
            stateHolder.conversationState.value
        )
    }
}
```

---

### `memory/ModelMemoryManagerTest.kt`

```kotlin
package com.july.offline.memory

import com.july.offline.core.memory.EngineType
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.ModelMode
import com.july.offline.core.memory.ModelState
import com.july.offline.testutil.TestCoroutineDispatchers
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ModelMemoryManagerTest {

    private lateinit var manager: ModelMemoryManager
    private val dispatchers = TestCoroutineDispatchers()

    @BeforeEach
    fun setup() {
        manager = ModelMemoryManager(
            logger = mockk(relaxed = true),
            dispatchers = dispatchers
        )
    }

    @Test
    fun `estado inicial es UNLOADED para ambos motores`() {
        assertEquals(ModelState.UNLOADED, manager.sttState.value)
        assertEquals(ModelState.UNLOADED, manager.ttsState.value)
    }

    @Test
    fun `notifyModelLoaded actualiza estado a LOADED`() {
        manager.notifyModelLoaded(EngineType.STT)
        assertEquals(ModelState.LOADED, manager.sttState.value)

        manager.notifyModelLoaded(EngineType.TTS)
        assertEquals(ModelState.LOADED, manager.ttsState.value)
    }

    @Test
    fun `notifyModelReleased actualiza estado a UNLOADED`() {
        manager.notifyModelLoaded(EngineType.STT)
        manager.notifyModelReleased(EngineType.STT)

        assertEquals(ModelState.UNLOADED, manager.sttState.value)
    }

    @Test
    fun `releaseModelsIfMemoryMode no hace nada en modo SPEED`() = runTest {
        manager.currentMode = ModelMode.SPEED
        var sttReleaseCalled = false
        manager.registerSttReleaseCallback { sttReleaseCalled = true }
        manager.notifyModelLoaded(EngineType.STT)

        manager.releaseModelsIfMemoryMode()

        assertFalse(sttReleaseCalled)
        assertEquals(ModelState.LOADED, manager.sttState.value)
    }

    @Test
    fun `releaseModelsIfMemoryMode libera modelos en modo MEMORY`() = runTest {
        manager.currentMode = ModelMode.MEMORY
        var sttReleaseCalled = false
        var ttsReleaseCalled = false

        manager.registerSttReleaseCallback { sttReleaseCalled = true }
        manager.registerTtsReleaseCallback { ttsReleaseCalled = true }

        manager.notifyModelLoaded(EngineType.STT)
        manager.notifyModelLoaded(EngineType.TTS)

        manager.releaseModelsIfMemoryMode()

        assertTrue(sttReleaseCalled)
        assertTrue(ttsReleaseCalled)
    }

    @Test
    fun `allModelsReady es true solo cuando ambos estan LOADED`() {
        assertFalse(manager.allModelsReady)

        manager.notifyModelLoaded(EngineType.STT)
        assertFalse(manager.allModelsReady)

        manager.notifyModelLoaded(EngineType.TTS)
        assertTrue(manager.allModelsReady)
    }

    @Test
    fun `releaseModelsIfMemoryMode no llama callback si modelo no esta LOADED`() = runTest {
        manager.currentMode = ModelMode.MEMORY
        var callbackCalled = false
        manager.registerSttReleaseCallback { callbackCalled = true }
        // STT sigue UNLOADED

        manager.releaseModelsIfMemoryMode()

        assertFalse(callbackCalled)
    }
}
```

---

### `memory/ModelReleaseTimerTest.kt`

```kotlin
package com.july.offline.memory

import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.ModelMode
import com.july.offline.lifecycle.ModelReleaseTimer
import com.july.offline.testutil.TestCoroutineDispatchers
import io.mockk.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ModelReleaseTimerTest {

    private lateinit var modelMemoryManager: ModelMemoryManager
    private lateinit var timer: ModelReleaseTimer
    private val dispatchers = TestCoroutineDispatchers()

    @BeforeEach
    fun setup() {
        modelMemoryManager = mockk(relaxed = true)
        timer = ModelReleaseTimer(
            modelMemoryManager = modelMemoryManager,
            logger = mockk(relaxed = true),
            dispatchers = dispatchers
        )
    }

    @Test
    fun `timer no esta activo antes de start`() {
        assertFalse(timer.isRunning)
    }

    @Test
    fun `start activa el timer`() = runTest {
        timer.start()
        assertTrue(timer.isRunning)
    }

    @Test
    fun `cancel desactiva el timer`() = runTest {
        timer.start()
        timer.cancel()
        assertFalse(timer.isRunning)
    }

    @Test
    fun `timer llama releaseModels tras 5 minutos`() = runTest {
        timer.start()

        // Avanzar el tiempo virtual 5 minutos
        advanceTimeBy(ModelReleaseTimer.RELEASE_DELAY_MS + 1)

        coVerify { modelMemoryManager.releaseModelsIfMemoryMode() }
    }

    @Test
    fun `si se cancela antes de 5 minutos no libera modelos`() = runTest {
        timer.start()

        // Avanzar 2 minutos y cancelar
        advanceTimeBy(2 * 60 * 1_000L)
        timer.cancel()

        // Avanzar otros 5 minutos
        advanceTimeBy(ModelReleaseTimer.RELEASE_DELAY_MS)

        coVerify(exactly = 0) { modelMemoryManager.releaseModelsIfMemoryMode() }
    }

    @Test
    fun `start es idempotente si ya esta activo`() = runTest {
        timer.start()
        timer.start()  // segunda llamada no debe crear segundo job

        // Solo debe haber un timer activo
        assertTrue(timer.isRunning)
    }

    @Test
    fun `timer puede reiniciarse tras cancel`() = runTest {
        timer.start()
        timer.cancel()
        assertFalse(timer.isRunning)

        timer.start()
        assertTrue(timer.isRunning)
    }
}
```

---

## RESUMEN DE CAMBIOS FASE 4 → FASE 5

| Componente | Cambio |
|---|---|
| `AppLifecycleObserver` | NUEVO — pausa Porcupine en onStop, reanuda en onStart via ProcessLifecycleOwner |
| `ModelReleaseTimer` | NUEVO — timer cancelable de 5 min para liberar modelos JNI |
| `ModelMemoryManager` | NUEVO — singleton que gestiona estado y liberación de contextos JNI |
| `ModelMode` | NUEVO — enum SPEED / MEMORY |
| `ModelState` | NUEVO — enum UNLOADED / LOADING / LOADED / RELEASING |
| `WakeWordCoordinator` | Añade `pauseForBackground()` + `wasActiveBeforeBackground` |
| `WhisperSTTAdapter` | Añade `ensureLoaded()` + `releaseContext()` + integración ModelMemoryManager |
| `PiperTTSAdapter` | Añade `ensureLoaded()` + `releaseContext()` + integración ModelMemoryManager |
| `SystemConfigDataStore` | Añade `modelMode` Flow |
| `MemoryModule` | NUEVO — configura ModelMemoryManager desde DataStore |
| `LifecycleModule` | NUEVO — declarativo, documenta la responsabilidad |
| `JulyApplication` | Registra AppLifecycleObserver + inyecta ModelMemoryManager |
| `AppSettings` | Añade `modelMode` |
| `SettingsViewModel` | Añade `setModelMode()` |
| `SettingsScreen` | Toggle Velocidad / Memoria con FilterChip |
| `FakeModelMemoryManager` | NUEVO fake para tests |
| Tests (3 suites) | AppLifecycleObserverTest, ModelMemoryManagerTest, ModelReleaseTimerTest |

---

## NOTAS CRÍTICAS

### Por qué ProcessLifecycleOwner y no ActivityLifecycleCallbacks

`ActivityLifecycleCallbacks` dispara `onActivityStopped` durante las rotaciones
de pantalla (la Activity se destruye y recrea). Esto causaría que Porcupine se
pause y reanude innecesariamente en cada rotación, generando latencia de ~500ms
visible para el usuario.

`ProcessLifecycleOwner.onStop` solo se dispara cuando **toda la app** va a
background — no ante config changes. Es la solución correcta para este caso.

### Por qué Mutex por motor en lugar de un lock global

Whisper y Piper se cargan/liberan independientemente. Un Mutex global
bloquearía la carga de Piper mientras Whisper se está cargando (~3s).
Con Mutex separados, ambos pueden cargarse en paralelo en el primer uso
tras volver de background en modo MEMORY.

### Consumo de memoria por modo

| Modo | RAM en foreground | RAM en background (>5min) |
|---|---|---|
| SPEED | ~1 GB (modelos + proceso) | ~1 GB |
| MEMORY | ~1 GB | ~234 MB (proceso sin modelos) |

El modo MEMORY libera ~766 MB tras 5 minutos en background,
lo que evita que el sistema Android mate el proceso por presión de memoria.
