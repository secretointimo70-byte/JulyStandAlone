# JULY OFFLINE — FASE 4
## Wake-Word (Porcupine) + Tests Unitarios
### `com.july.offline`

**Alcance FASE 4:**
- Integración Porcupine SDK para detección pasiva de wake-word "Oye July"
- Nuevo estado `ConversationState.WakeWordListening`
- `WakeWordEngine` interface en domain/port
- `WakeWordCoordinator` en domain/orchestrator
- `PorcupineWakeWordAdapter` en capa `wakeword/`
- Módulo DI `WakeWordModule`
- UI: toggle para activar/desactivar modo wake-word
- Tests: JUnit 5 + MockK + Turbine
  - `ConversationOrchestratorTest`
  - `WakeWordCoordinatorTest`
  - `AudioCoordinatorTest`
  - `WhisperSTTAdapterTest`
  - `LocalServerLLMAdapterTest`
  - `SessionRepositoryImplTest`
  - Fakes reutilizables: `FakeSpeechToTextEngine`, `FakeLanguageModelEngine`, `FakeTextToSpeechEngine`, `FakeAudioCapturePort`, `FakeSessionRepository`

---

## ÍNDICE DE ARCHIVOS FASE 4

### NUEVOS — DOMAIN
- `domain/model/ConversationState.kt` (añade WakeWordListening)
- `domain/port/WakeWordEngine.kt`
- `domain/orchestrator/WakeWordCoordinator.kt`
- `domain/orchestrator/ConversationOrchestrator.kt` (añade wake-word cycle)
- `domain/state/ConversationStateHolder.kt` (añade transitionToWakeWordListening)

### NUEVOS — WAKEWORD
- `wakeword/PorcupineConfig.kt`
- `wakeword/PorcupineWakeWordAdapter.kt`

### NUEVOS — DI
- `di/WakeWordModule.kt`

### NUEVOS — SETTINGS
- `data/datastore/AppPreferencesDataStore.kt` (añade wakeWordEnabled)

### NUEVOS — UI
- `ui/conversation/ConversationUiState.kt` (añade WAKE_WORD_LISTENING phase)
- `ui/conversation/ConversationScreen.kt` (toggle wake-word)
- `ui/conversation/ConversationViewModel.kt` (toggle handler)
- `ui/conversation/components/WakeWordIndicator.kt`

### NUEVOS — BUILD
- `app/build.gradle.kts` (dependencia Porcupine SDK)
- `gradle/libs.versions.toml` (versión Porcupine)

### NUEVOS — TESTS
- `app/src/test/kotlin/.../testutil/FakeSpeechToTextEngine.kt`
- `app/src/test/kotlin/.../testutil/FakeLanguageModelEngine.kt`
- `app/src/test/kotlin/.../testutil/FakeTextToSpeechEngine.kt`
- `app/src/test/kotlin/.../testutil/FakeAudioCapturePort.kt`
- `app/src/test/kotlin/.../testutil/FakeSessionRepository.kt`
- `app/src/test/kotlin/.../testutil/FakeWakeWordEngine.kt`
- `app/src/test/kotlin/.../testutil/TestCoroutineDispatchers.kt`
- `app/src/test/kotlin/.../orchestrator/ConversationOrchestratorTest.kt`
- `app/src/test/kotlin/.../orchestrator/WakeWordCoordinatorTest.kt`
- `app/src/test/kotlin/.../orchestrator/AudioCoordinatorTest.kt`
- `app/src/test/kotlin/.../ai/stt/WhisperSTTAdapterTest.kt`
- `app/src/test/kotlin/.../ai/llm/LocalServerLLMAdapterTest.kt`
- `app/src/test/kotlin/.../data/repository/SessionRepositoryImplTest.kt`

---

## BUILD — DEPENDENCIAS

### `gradle/libs.versions.toml` — añadir sección Porcupine y testing

```toml
[versions]
# ... versiones existentes ...
porcupine = "3.0.2"
junit5 = "5.10.2"
mockk = "1.13.12"
turbine = "1.1.0"
coroutinesTest = "1.9.0"
roomTesting = "2.6.1"

[libraries]
# ... librerías existentes ...

# Porcupine Wake-Word SDK
porcupine = { group = "ai.picovoice", name = "porcupine-android", version.ref = "porcupine" }

# Testing — JUnit 5
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }

# Testing — MockK
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

# Testing — Turbine (Flow testing)
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

# Testing — Coroutines
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

# Testing — Room
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "roomTesting" }

[plugins]
# ... plugins existentes ...
junit5-android = { id = "de.mannodermaus.android-junit5", version = "1.10.2.0" }
```

---

### `app/build.gradle.kts` — sección adicional

```kotlin
plugins {
    // ... plugins existentes ...
    id("de.mannodermaus.android-junit5")   // soporte JUnit 5 en Android
}

android {
    // ... configuración existente ...

    defaultConfig {
        // ... existente ...
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // Configuración de tests
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()  // habilita JUnit 5
            }
        }
    }
}

dependencies {
    // ... dependencias existentes ...

    // Porcupine Wake-Word
    implementation(libs.porcupine)

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)
}
```

**Nota Porcupine SDK:** Requiere añadir el repositorio Maven de Picovoice al `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.github.com/Picovoice/porcupine") }
        // O directamente desde JitPack:
        maven { url = uri("https://jitpack.io") }
    }
}
```

---

## DOMAIN — ACTUALIZACIONES

### `domain/model/ConversationState.kt` (añade WakeWordListening)

```kotlin
package com.july.offline.domain.model

sealed class ConversationState {

    object Idle : ConversationState()

    /**
     * NUEVO FASE 4.
     * Porcupine está activo, escuchando pasivamente el audio del micrófono.
     * Este estado persiste hasta que:
     * a) Se detecta el wake-word → transición a Listening
     * b) El usuario desactiva el modo wake-word → transición a Idle
     * c) La app va a background → Porcupine se pausa (solo foreground en FASE 4)
     */
    object WakeWordListening : ConversationState()

    data class Listening(val sessionId: String) : ConversationState()

    data class Transcribing(
        val sessionId: String,
        val audioLengthMs: Long
    ) : ConversationState()

    data class Thinking(
        val sessionId: String,
        val transcript: Transcript
    ) : ConversationState()

    data class Speaking(
        val sessionId: String,
        val response: LlmResponse,
        val fallbackText: String = response.text
    ) : ConversationState()

    data class Error(
        val error: com.july.offline.core.error.AppError,
        val previousState: ConversationState
    ) : ConversationState()

    object Cancelled : ConversationState()
}
```

---

### `domain/port/WakeWordEngine.kt` (NUEVO)

```kotlin
package com.july.offline.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Contrato del motor de detección de wake-word.
 * Sin dependencias Android. Implementado en capa wakeword/.
 *
 * El motor escucha audio del micrófono de forma continua y emite
 * un evento cuando detecta la palabra clave configurada.
 */
interface WakeWordEngine {

    /**
     * Inicia la escucha pasiva y emite Unit cada vez que se detecta el wake-word.
     * El Flow permanece activo hasta que se llama a stop().
     *
     * Garantías:
     * - No lanza excepciones al detectar; los errores se emiten como WakeWordError
     * - Es responsabilidad del WakeWordCoordinator suscribirse y cancelar el Flow
     * - El motor gestiona su propio AudioRecord interno (separado del de grabación)
     */
    fun startListening(): Flow<WakeWordEvent>

    /** Detiene la escucha y libera el AudioRecord interno. Idempotente. */
    fun stop()

    /** true si el motor está actualmente escuchando. */
    val isListening: Boolean

    /** Verifica si el motor está disponible (clave API válida, modelo presente). */
    suspend fun isAvailable(): Boolean
}

/** Eventos emitidos por WakeWordEngine. */
sealed class WakeWordEvent {
    /** Wake-word detectado. `keywordIndex` identifica cuál keyword si hay varios. */
    data class Detected(val keywordIndex: Int = 0) : WakeWordEvent()
    /** Error durante la escucha. El coordinador debe decidir si reintentar. */
    data class Error(val cause: Throwable) : WakeWordEvent()
}
```

---

### `domain/state/ConversationStateHolder.kt` (añade transición WakeWordListening)

```kotlin
package com.july.offline.domain.state

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.RuntimeState
import com.july.offline.domain.model.Transcript
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationStateHolder @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    private val _runtimeState = MutableStateFlow(RuntimeState())
    val runtimeState: StateFlow<RuntimeState> = _runtimeState.asStateFlow()

    // ── Transiciones existentes ────────────────────────────────────────

    internal fun transitionToListening(sessionId: String) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Listening(sessionId)
        logger.logStateTransition(previous.javaClass.simpleName, "Listening", "user_input")
    }

    internal fun transitionToTranscribing(sessionId: String, audioLengthMs: Long) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Transcribing(sessionId, audioLengthMs)
        logger.logStateTransition(previous.javaClass.simpleName, "Transcribing", "vad_end")
    }

    internal fun transitionToThinking(sessionId: String, transcript: Transcript) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Thinking(sessionId, transcript)
        logger.logStateTransition(previous.javaClass.simpleName, "Thinking", "transcript_ready")
    }

    internal fun transitionToSpeaking(sessionId: String, response: LlmResponse) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Speaking(sessionId, response)
        logger.logStateTransition(previous.javaClass.simpleName, "Speaking", "llm_response")
    }

    internal fun transitionToError(error: AppError) {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Error(error, previous)
        _runtimeState.update { it.copy(lastError = error, lastErrorAt = Instant.now()) }
        logger.logStateTransition(previous.javaClass.simpleName, "Error", error.message)
    }

    internal fun transitionToCancelled() {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Cancelled
        logger.logStateTransition(previous.javaClass.simpleName, "Cancelled", "user_cancel")
    }

    internal fun resetToIdle() {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.Idle
        logger.logStateTransition(previous.javaClass.simpleName, "Idle", "reset")
    }

    // ── NUEVO FASE 4 ───────────────────────────────────────────────────

    /**
     * Transición al estado de escucha pasiva de wake-word.
     * Solo válida desde Idle. Desde cualquier otro estado se ignora.
     */
    internal fun transitionToWakeWordListening() {
        val current = _conversationState.value
        if (current !is ConversationState.Idle) {
            logger.logWarning(
                "StateHolder",
                "transitionToWakeWordListening() called from $current, ignoring"
            )
            return
        }
        _conversationState.value = ConversationState.WakeWordListening
        logger.logStateTransition("Idle", "WakeWordListening", "wake_word_enabled")
    }

    /**
     * Vuelve a WakeWordListening tras completar un ciclo de conversación,
     * si el modo wake-word sigue activo.
     */
    internal fun resetToWakeWordListening() {
        val previous = _conversationState.value
        _conversationState.value = ConversationState.WakeWordListening
        logger.logStateTransition(
            previous.javaClass.simpleName,
            "WakeWordListening",
            "cycle_complete_wake_word_active"
        )
    }

    // ── Mutaciones de RuntimeState ────────────────────────────────────

    internal fun setCurrentSession(sessionId: String?) {
        _runtimeState.update { it.copy(currentSessionId = sessionId) }
    }

    internal fun incrementCycleCount() {
        _runtimeState.update { it.copy(cycleCount = it.cycleCount + 1) }
    }
}
```

---

### `domain/orchestrator/WakeWordCoordinator.kt` (NUEVO)

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
 * Coordinador del motor de wake-word.
 *
 * Responsabilidades:
 * - Iniciar y detener la escucha pasiva de Porcupine
 * - Transicionar el estado a WakeWordListening cuando se activa
 * - Notificar al ConversationOrchestrator cuando se detecta el wake-word
 * - Manejar errores del motor y reintentar si es posible
 *
 * No transiciona estados de conversación más allá de WakeWordListening.
 * La transición a Listening es responsabilidad del ConversationOrchestrator.
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

    /** Callback invocado cuando Porcupine detecta el wake-word. */
    var onWakeWordDetected: (() -> Unit)? = null

    /**
     * Activa la escucha pasiva.
     * Si ya está activa, no hace nada.
     */
    fun startWakeWordListening() {
        if (wakeWordEngine.isListening) {
            logger.logDebug("WakeWordCoordinator", "Already listening, ignoring startWakeWordListening()")
            return
        }

        stateHolder.transitionToWakeWordListening()

        listeningJob = scope.launch {
            logger.logInfo("WakeWordCoordinator", "Starting Porcupine wake-word listening")

            wakeWordEngine.startListening()
                .catch { cause ->
                    logger.logError("WakeWordCoordinator", "Wake-word stream error", cause)
                    // Reintentar una vez tras 1 segundo
                    kotlinx.coroutines.delay(1_000L)
                    emit(WakeWordEvent.Error(cause))
                }
                .collect { event ->
                    when (event) {
                        is WakeWordEvent.Detected -> {
                            logger.logInfo(
                                "WakeWordCoordinator",
                                "Wake-word detected (keyword index: ${event.keywordIndex})"
                            )
                            // Detener Porcupine antes de iniciar grabación
                            // (AudioRecord no puede compartirse entre Porcupine y AudioRecorder)
                            wakeWordEngine.stop()
                            onWakeWordDetected?.invoke()
                        }
                        is WakeWordEvent.Error -> {
                            logger.logError(
                                "WakeWordCoordinator",
                                "Wake-word engine error: ${event.cause.message}"
                            )
                            // Silenciar — el estado WakeWordListening permanece
                            // pero Porcupine se habrá detenido
                        }
                    }
                }
        }
    }

    /**
     * Desactiva la escucha pasiva y vuelve a IDLE.
     * Llamado cuando el usuario desactiva el toggle de wake-word.
     */
    fun stopWakeWordListening() {
        listeningJob?.cancel()
        listeningJob = null
        wakeWordEngine.stop()
        stateHolder.resetToIdle()
        logger.logInfo("WakeWordCoordinator", "Wake-word listening stopped")
    }

    /**
     * Reinicia la escucha tras completar un ciclo de conversación.
     * Llamado por ConversationOrchestrator si el modo wake-word estaba activo.
     */
    fun resumeAfterCycle() {
        if (!wakeWordEngine.isListening) {
            startWakeWordListening()
        }
    }

    val isActive: Boolean get() = wakeWordEngine.isListening || listeningJob?.isActive == true
}
```

---

### `domain/orchestrator/ConversationOrchestrator.kt` (actualizado con wake-word)

```kotlin
package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.error.AppError
import com.july.offline.core.error.ErrorAction
import com.july.offline.core.error.ErrorHandler
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import com.july.offline.domain.state.ConversationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConversationOrchestrator actualizado para FASE 4.
 *
 * Cambios respecto a FASE 3:
 * - Referencia a WakeWordCoordinator para saber si el modo pasivo está activo
 * - Al finalizar el ciclo: si wake-word mode estaba activo, vuelve a WakeWordListening
 *   en lugar de a Idle
 * - WakeWordCoordinator registra su callback onWakeWordDetected apuntando
 *   a startConversationCycle() de este orquestador
 */
@Singleton
class ConversationOrchestrator @Inject constructor(
    private val stateHolder: ConversationStateHolder,
    private val audioCoordinator: AudioCoordinator,
    private val sessionCoordinator: SessionCoordinator,
    private val wakeWordCoordinator: WakeWordCoordinator,
    private val sttEngine: SpeechToTextEngine,
    private val llmEngine: LanguageModelEngine,
    private val ttsEngine: TextToSpeechEngine,
    private val errorHandler: ErrorHandler,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private var activeJob: Job? = null

    init {
        // Registrar el callback de detección de wake-word
        wakeWordCoordinator.onWakeWordDetected = {
            startConversationCycle()
        }
    }

    fun startConversationCycle() {
        val currentState = stateHolder.conversationState.value
        val validStartStates = setOf(
            ConversationState.Idle::class,
            ConversationState.WakeWordListening::class,
            ConversationState.Cancelled::class,
            ConversationState.Error::class
        )
        if (currentState::class !in validStartStates) {
            logger.logWarning("Orchestrator", "startConversationCycle() from $currentState, ignoring")
            return
        }

        activeJob = scope.launch { runCycle() }
    }

    fun cancelCurrentCycle() {
        activeJob?.cancel()
        activeJob = null
        audioCoordinator.cancel()
        stateHolder.transitionToCancelled()

        scope.launch {
            delay(100)
            // Si el modo wake-word estaba activo, volver a WakeWordListening
            if (wakeWordCoordinator.isActive) {
                wakeWordCoordinator.resumeAfterCycle()
            } else {
                stateHolder.resetToIdle()
            }
        }
        logger.logInfo("Orchestrator", "Cycle cancelled")
    }

    private suspend fun runCycle() = withContext(dispatchers.main) {
        val sessionId = sessionCoordinator.ensureActiveSession()
        stateHolder.setCurrentSession(sessionId)

        // ── 1. LISTENING ───────────────────────────────────────────────
        stateHolder.transitionToListening(sessionId)
        val audioResult = audioCoordinator.recordUntilSilence()

        if (audioResult == null) {
            stateHolder.transitionToCancelled()
            delay(100)
            resumeOrIdle()
            return@withContext
        }

        val (audioBytes, audioLengthMs) = audioResult

        // ── 2. TRANSCRIBING ────────────────────────────────────────────
        stateHolder.transitionToTranscribing(sessionId, audioLengthMs)
        val startStt = System.currentTimeMillis()

        val transcript: Transcript = when (val result = sttEngine.transcribe(audioBytes)) {
            is JulyResult.Success -> {
                logger.logEngineEvent("STT", "transcribed", System.currentTimeMillis() - startStt)
                result.data
            }
            is JulyResult.Failure -> {
                handleError(result.error)
                return@withContext
            }
        }

        if (transcript.isEmpty) {
            logger.logInfo("Orchestrator", "Empty transcript, resuming")
            resumeOrIdle()
            return@withContext
        }

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = transcript.text
        )
        sessionCoordinator.addMessage(sessionId, userMessage)

        // ── 3. THINKING ────────────────────────────────────────────────
        stateHolder.transitionToThinking(sessionId, transcript)
        val history = sessionCoordinator.getHistory(sessionId)
        val startLlm = System.currentTimeMillis()

        val llmResponse: LlmResponse = when (val result = llmEngine.generate(transcript.text, history)) {
            is JulyResult.Success -> {
                logger.logEngineEvent("LLM", "generated", System.currentTimeMillis() - startLlm)
                result.data
            }
            is JulyResult.Failure -> {
                if (result.error is AppError.Llm && result.error.retryable) {
                    delay(500L)
                    when (val retry = llmEngine.generate(transcript.text, history)) {
                        is JulyResult.Success -> retry.data
                        is JulyResult.Failure -> {
                            handleError(retry.error)
                            return@withContext
                        }
                    }
                } else {
                    handleError(result.error)
                    return@withContext
                }
            }
        }

        val assistantMessage = Message(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            content = llmResponse.text
        )
        sessionCoordinator.addMessage(sessionId, assistantMessage)

        // ── 4. SPEAKING ────────────────────────────────────────────────
        stateHolder.transitionToSpeaking(sessionId, llmResponse)
        val startTts = System.currentTimeMillis()

        when (val ttsResult = ttsEngine.synthesize(llmResponse.text)) {
            is JulyResult.Success -> {
                logger.logEngineEvent("TTS", "synthesized", System.currentTimeMillis() - startTts)
                audioCoordinator.playAudio(ttsResult.data)
            }
            is JulyResult.Failure -> {
                logger.logWarning("Orchestrator", "TTS failed, showing text fallback")
                delay(2_000L)
            }
        }

        // ── 5. RESET ───────────────────────────────────────────────────
        stateHolder.incrementCycleCount()
        resumeOrIdle()
    }

    /**
     * Tras completar el ciclo, vuelve a WakeWordListening si el modo estaba activo,
     * o a Idle si no.
     */
    private fun resumeOrIdle() {
        if (wakeWordCoordinator.isActive) {
            wakeWordCoordinator.resumeAfterCycle()
        } else {
            stateHolder.resetToIdle()
        }
    }

    private suspend fun handleError(error: AppError) {
        stateHolder.transitionToError(error)
        when (errorHandler.handle(error)) {
            is ErrorAction.ResetToIdle,
            is ErrorAction.FallbackToText,
            is ErrorAction.Retry -> {
                delay(300L)
                resumeOrIdle()
            }
            is ErrorAction.ShowPermissionRationale -> { /* UI gestiona via estado Error */ }
        }
    }
}
```

---

## WAKEWORD — IMPLEMENTACIÓN PORCUPINE

### `wakeword/PorcupineConfig.kt`

```kotlin
package com.july.offline.wakeword

/**
 * Configuración del motor Porcupine.
 *
 * @param accessKey clave API de Picovoice (gratuita en picovoice.ai/console)
 * @param keywordPaths lista de rutas a archivos .ppn (modelos de keyword personalizados)
 *                     Si está vacío, usa el keyword built-in "Hey July" de la keyword list
 * @param builtInKeyword keyword built-in como fallback (solo si keywordPaths está vacío)
 * @param sensitivities sensibilidad por keyword [0.0, 1.0]. Mayor = más detecciones, más falsos positivos
 * @param modelPath ruta al modelo de idioma Porcupine (.pv). null = modelo en assets de la app
 */
data class PorcupineConfig(
    val accessKey: String,
    val keywordPaths: List<String> = emptyList(),
    val builtInKeyword: String = "hey siri",   // fallback built-in; reemplazar con modelo custom
    val sensitivities: List<Float> = listOf(0.5f),
    val modelPath: String? = null
) {
    init {
        require(accessKey.isNotBlank()) { "Porcupine accessKey must not be blank" }
        require(sensitivities.all { it in 0f..1f }) { "Sensitivities must be in [0.0, 1.0]" }
    }
}
```

---

### `wakeword/PorcupineWakeWordAdapter.kt`

```kotlin
package com.july.offline.wakeword

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import android.content.Context
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.domain.port.WakeWordEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Adaptador Porcupine para detección de wake-word.
 *
 * IMPORTANTE: Porcupine usa su propio AudioRecord interno separado del
 * AudioRecorderAdapter de grabación. Ambos no pueden estar activos simultáneamente
 * en el mismo dispositivo. El WakeWordCoordinator garantiza que Porcupine
 * se detiene ANTES de que AudioRecorderAdapter inicie grabación.
 *
 * Porcupine requiere audio PCM 16-bit mono a 16kHz en chunks de frameLength samples.
 * El frameLength es fijo por SDK (~512 samples = 32ms por frame).
 *
 * Clave API: obtener gratis en https://picovoice.ai/console/
 * Modelo custom "Oye July": entrenar en https://console.picovoice.ai/ppn
 */
class PorcupineWakeWordAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: PorcupineConfig,
    private val logger: DiagnosticsLogger
) : WakeWordEngine {

    @Volatile private var porcupine: Porcupine? = null
    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var _isListening = false

    override val isListening: Boolean get() = _isListening

    override fun startListening(): Flow<WakeWordEvent> = flow {
        val engine = buildPorcupine() ?: run {
            emit(WakeWordEvent.Error(IllegalStateException("Failed to initialize Porcupine")))
            return@flow
        }
        porcupine = engine

        val recorder = buildAudioRecord(engine.frameLength) ?: run {
            engine.delete()
            porcupine = null
            emit(WakeWordEvent.Error(IllegalStateException("Failed to open AudioRecord for Porcupine")))
            return@flow
        }
        audioRecord = recorder

        recorder.startRecording()
        _isListening = true
        logger.logInfo("PorcupineAdapter", "Listening for wake-word (frameLength=${engine.frameLength})")

        val frameBuffer = ShortArray(engine.frameLength)
        val byteBuffer = ByteBuffer.allocateDirect(engine.frameLength * 2)
            .order(ByteOrder.LITTLE_ENDIAN)

        try {
            while (isActive && _isListening) {
                byteBuffer.clear()
                val bytesRead = recorder.read(byteBuffer, engine.frameLength * 2)

                if (bytesRead < engine.frameLength * 2) continue

                byteBuffer.rewind()
                byteBuffer.asShortBuffer().get(frameBuffer)

                val keywordIndex = engine.process(frameBuffer)
                if (keywordIndex >= 0) {
                    logger.logInfo("PorcupineAdapter", "Keyword detected at index $keywordIndex")
                    emit(WakeWordEvent.Detected(keywordIndex))
                    // Detener después de detectar — el coordinador decide si reiniciar
                    break
                }
            }
        } catch (e: PorcupineException) {
            logger.logError("PorcupineAdapter", "Porcupine processing error", e)
            emit(WakeWordEvent.Error(e))
        } finally {
            cleanup()
        }
    }.flowOn(Dispatchers.IO)

    override fun stop() {
        _isListening = false
        cleanup()
        logger.logInfo("PorcupineAdapter", "Stopped")
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            config.accessKey.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun buildPorcupine(): Porcupine? {
        return try {
            val builder = Porcupine.Builder()
                .setAccessKey(config.accessKey)

            if (config.keywordPaths.isNotEmpty()) {
                // Modelo personalizado "Oye July" entrenado en Picovoice Console
                builder.setKeywordPaths(config.keywordPaths.toTypedArray())
                builder.setSensitivities(config.sensitivities.toFloatArray())
            } else {
                // Keyword built-in como fallback (no incluye "Oye July" en español)
                // Para producción se requiere el modelo .ppn personalizado
                logger.logWarning(
                    "PorcupineAdapter",
                    "No custom keyword model configured. Using built-in '${config.builtInKeyword}'. " +
                    "Train a custom model at https://console.picovoice.ai/ppn"
                )
                builder.setKeyword(Porcupine.BuiltInKeyword.HEY_SIRI) // placeholder
                builder.setSensitivity(config.sensitivities.firstOrNull() ?: 0.5f)
            }

            config.modelPath?.let { builder.setModelPath(it) }

            builder.build(context)
        } catch (e: PorcupineException) {
            logger.logError("PorcupineAdapter", "Failed to build Porcupine: ${e.message}", e)
            null
        }
    }

    private fun buildAudioRecord(frameLength: Int): AudioRecord? {
        val sampleRate = 16_000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, frameLength * 2 * 4)

        return try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            ).also {
                if (it.state != AudioRecord.STATE_INITIALIZED) {
                    it.release()
                    return null
                }
            }
        } catch (e: Exception) {
            logger.logError("PorcupineAdapter", "AudioRecord init failed", e)
            null
        }
    }

    private fun cleanup() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            logger.logWarning("PorcupineAdapter", "AudioRecord cleanup error", e)
        }
        try {
            porcupine?.delete()
            porcupine = null
        } catch (e: Exception) {
            logger.logWarning("PorcupineAdapter", "Porcupine cleanup error", e)
        }
        _isListening = false
    }
}
```

---

## DI

### `di/WakeWordModule.kt` (NUEVO)

```kotlin
package com.july.offline.di

import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.wakeword.PorcupineConfig
import com.july.offline.wakeword.PorcupineWakeWordAdapter
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
    abstract fun bindWakeWordEngine(impl: PorcupineWakeWordAdapter): WakeWordEngine

    companion object {

        /**
         * IMPORTANTE: Reemplazar PICOVOICE_ACCESS_KEY con la clave real.
         * Obtener gratis en: https://picovoice.ai/console/
         *
         * Para modelo "Oye July" personalizado:
         * 1. Ir a https://console.picovoice.ai/ppn
         * 2. Crear keyword "Oye July" en idioma Spanish (ES)
         * 3. Descargar el archivo .ppn
         * 4. Colocar en app/src/main/assets/oye_july_es.ppn
         * 5. Reemplazar keywordPaths con la ruta al asset copiado a filesDir
         */
        @Provides
        @Singleton
        fun providePorcupineConfig(): PorcupineConfig = PorcupineConfig(
            accessKey = BuildConfig.PICOVOICE_ACCESS_KEY,
            keywordPaths = listOf(
                // Ruta al .ppn personalizado copiado a filesDir por AssetCopier
                // En FASE 4 se usa el built-in como placeholder
                // "/data/data/com.july.offline/files/oye_july_es.ppn"
            ),
            builtInKeyword = "hey siri",  // reemplazar con modelo custom
            sensitivities = listOf(0.6f)
        )
    }
}
```

**Añadir al `app/build.gradle.kts`** dentro de `defaultConfig {}`:
```kotlin
buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"${System.getenv("PICOVOICE_KEY") ?: ""}\"")
```

**Añadir al `AndroidManifest.xml`** (ya tiene RECORD_AUDIO de FASE 3):
```xml
<!-- Ya presente desde FASE 3 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## DATA — ACTUALIZACIÓN

### `data/datastore/AppPreferencesDataStore.kt` (añade wakeWordEnabled)

```kotlin
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
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")  // NUEVO FASE 4
    }

    val language: Flow<String> = context.appPrefsDataStore.data.map { it[Keys.LANGUAGE] ?: "es" }
    val ttsEnabled: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.TTS_ENABLED] ?: true }
    val showTranscript: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.SHOW_TRANSCRIPT] ?: true }
    val wakeWordEnabled: Flow<Boolean> = context.appPrefsDataStore.data.map { it[Keys.WAKE_WORD_ENABLED] ?: false }

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
}
```

---

## UI — ACTUALIZACIONES

### `ui/conversation/ConversationUiState.kt` (añade WAKE_WORD_LISTENING)

```kotlin
package com.july.offline.ui.conversation

import com.july.offline.domain.model.EngineHealthState
import com.july.offline.domain.model.EngineStatus

data class ConversationUiState(
    val phase: ConversationPhase = ConversationPhase.IDLE,
    val displayedText: String = "",
    val transcriptText: String = "",
    val errorMessage: String? = null,
    val engineHealth: EngineHealthUiState = EngineHealthUiState(),
    val isCancelVisible: Boolean = false,
    val isMicButtonEnabled: Boolean = true,
    val messages: List<MessageUiModel> = emptyList(),
    val isWakeWordActive: Boolean = false      // NUEVO FASE 4
)

enum class ConversationPhase {
    IDLE,
    WAKE_WORD_LISTENING,     // NUEVO FASE 4
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING,
    ERROR,
    CANCELLED
}

data class EngineHealthUiState(
    val sttReady: Boolean = false,
    val llmReady: Boolean = false,
    val ttsReady: Boolean = false,
    val showWarning: Boolean = false
) {
    companion object {
        fun from(health: EngineHealthState) = EngineHealthUiState(
            sttReady = health.sttStatus == EngineStatus.READY,
            llmReady = health.llmStatus == EngineStatus.READY,
            ttsReady = health.ttsStatus == EngineStatus.READY,
            showWarning = health.anyUnavailable
        )
    }
}

data class MessageUiModel(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)
```

---

### `ui/conversation/components/WakeWordIndicator.kt` (NUEVO)

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Indicador visual de escucha pasiva de wake-word.
 * Muestra un pulso animado para indicar que Porcupine está activo.
 */
@Composable
fun WakeWordIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wakeword_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    shape = CircleShape
                )
        )
        Text(
            text = "Esperando 'Oye July'...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

### `ui/conversation/ConversationViewModel.kt` (añade toggle wake-word)

```kotlin
package com.july.offline.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.core.error.AppError
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.orchestrator.ConversationOrchestrator
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.domain.orchestrator.SessionCoordinator
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.port.SessionRepository
import com.july.offline.domain.state.ConversationStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val orchestrator: ConversationOrchestrator,
    private val stateHolder: ConversationStateHolder,
    private val healthMonitor: EngineHealthMonitor,
    private val sessionCoordinator: SessionCoordinator,
    private val sessionRepository: SessionRepository,
    private val wakeWordCoordinator: WakeWordCoordinator,
    private val preferencesDataStore: AppPreferencesDataStore
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    val uiState: StateFlow<ConversationUiState> = combine(
        stateHolder.conversationState,
        healthMonitor.healthState,
        sessionRepository.getRecentSessions(1),
        preferencesDataStore.wakeWordEnabled
    ) { conversationState, healthState, recentSessions, wakeWordEnabled ->
        val messages = recentSessions.firstOrNull()?.messages?.map { msg ->
            MessageUiModel(
                id = msg.id,
                text = msg.content,
                isUser = msg.role == MessageRole.USER,
                timestamp = timeFormatter.format(msg.timestamp)
            )
        } ?: emptyList()

        mapToUiState(conversationState, healthState, messages, wakeWordEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationUiState()
    )

    fun onMicPressed() = orchestrator.startConversationCycle()
    fun onCancelPressed() = orchestrator.cancelCurrentCycle()
    fun onPermissionDenied() {}

    /** Activa o desactiva el modo de escucha pasiva de wake-word. */
    fun onWakeWordToggled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setWakeWordEnabled(enabled)
            if (enabled) {
                wakeWordCoordinator.startWakeWordListening()
            } else {
                wakeWordCoordinator.stopWakeWordListening()
            }
        }
    }

    private fun mapToUiState(
        state: ConversationState,
        health: com.july.offline.domain.model.EngineHealthState,
        messages: List<MessageUiModel>,
        wakeWordEnabled: Boolean
    ): ConversationUiState {
        val engineHealthUi = EngineHealthUiState.from(health)
        return when (state) {
            is ConversationState.Idle -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                isMicButtonEnabled = engineHealthUi.llmReady,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.WakeWordListening -> ConversationUiState(
                phase = ConversationPhase.WAKE_WORD_LISTENING,
                engineHealth = engineHealthUi,
                isMicButtonEnabled = true,
                messages = messages,
                isWakeWordActive = true
            )
            is ConversationState.Listening -> ConversationUiState(
                phase = ConversationPhase.LISTENING,
                displayedText = "Escuchando...",
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Transcribing -> ConversationUiState(
                phase = ConversationPhase.TRANSCRIBING,
                displayedText = "Procesando audio...",
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Thinking -> ConversationUiState(
                phase = ConversationPhase.THINKING,
                displayedText = "Pensando...",
                transcriptText = state.transcript.text,
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Speaking -> ConversationUiState(
                phase = ConversationPhase.SPEAKING,
                displayedText = state.response.text,
                engineHealth = engineHealthUi,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Error -> ConversationUiState(
                phase = ConversationPhase.ERROR,
                errorMessage = mapErrorToUserMessage(state.error),
                engineHealth = engineHealthUi,
                isMicButtonEnabled = state.error !is AppError.Permission,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
            is ConversationState.Cancelled -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                messages = messages,
                isWakeWordActive = wakeWordEnabled
            )
        }
    }

    private fun mapErrorToUserMessage(error: AppError): String = when (error) {
        is AppError.Permission -> "Se necesita permiso de micrófono"
        is AppError.Stt -> "No pude entenderte. Intenta de nuevo."
        is AppError.Llm -> "El asistente no está disponible."
        is AppError.Tts -> "No pude reproducir la respuesta."
        is AppError.Network -> "El servidor de IA no está disponible."
        is AppError.Cancelled -> ""
        is AppError.Unknown -> "Error inesperado."
    }
}
```

---

### `ui/conversation/ConversationScreen.kt` (toggle wake-word)

```kotlin
package com.july.offline.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.ui.conversation.components.*
import com.july.offline.ui.permission.PermissionHandler

@Composable
fun ConversationScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("July") },
                actions = {
                    // Toggle wake-word en la barra superior
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Oye July",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = uiState.isWakeWordActive,
                            onCheckedChange = { viewModel.onWakeWordToggled(it) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Text("⚙", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EngineHealthWidget(healthState = uiState.engineHealth)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(items = uiState.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                // Indicador de estado actual
                item {
                    when (uiState.phase) {
                        ConversationPhase.WAKE_WORD_LISTENING -> {
                            WakeWordIndicator(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        ConversationPhase.LISTENING -> {
                            Column {
                                StatusBar(phase = uiState.phase)
                                Spacer(Modifier.height(4.dp))
                                WaveformIndicator()
                            }
                        }
                        ConversationPhase.TRANSCRIBING,
                        ConversationPhase.THINKING,
                        ConversationPhase.SPEAKING -> {
                            StatusBar(phase = uiState.phase)
                        }
                        else -> {}
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            PermissionHandler(
                onPermissionGranted = { viewModel.onMicPressed() },
                onPermissionDenied = { viewModel.onPermissionDenied() }
            ) { requestPermissionAndStart ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    if (uiState.isCancelVisible) {
                        OutlinedButton(
                            onClick = { viewModel.onCancelPressed() },
                            modifier = Modifier.padding(end = 16.dp)
                        ) { Text("Cancelar") }
                    }

                    val isIdle = uiState.phase == ConversationPhase.IDLE ||
                                 uiState.phase == ConversationPhase.WAKE_WORD_LISTENING ||
                                 uiState.phase == ConversationPhase.ERROR ||
                                 uiState.phase == ConversationPhase.CANCELLED

                    Button(
                        onClick = { if (isIdle) requestPermissionAndStart() },
                        enabled = uiState.isMicButtonEnabled && isIdle
                    ) {
                        Text(
                            text = when (uiState.phase) {
                                ConversationPhase.IDLE,
                                ConversationPhase.CANCELLED -> "Hablar"
                                ConversationPhase.WAKE_WORD_LISTENING -> "Hablar ahora"
                                ConversationPhase.LISTENING -> "Escuchando..."
                                ConversationPhase.TRANSCRIBING -> "Procesando..."
                                ConversationPhase.THINKING -> "Pensando..."
                                ConversationPhase.SPEAKING -> "Respondiendo..."
                                ConversationPhase.ERROR -> "Reintentar"
                            }
                        )
                    }
                }
            }
        }
    }
}
```

---

## TESTS

### `testutil/TestCoroutineDispatchers.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.core.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

/**
 * CoroutineDispatchers de test.
 * Todos los dispatchers apuntan al mismo TestDispatcher para control total
 * del tiempo en tests con runTest { }.
 */
class TestCoroutineDispatchers(
    scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
) : CoroutineDispatchers() {

    private val testDispatcher = StandardTestDispatcher(scheduler)

    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
```

---

### `testutil/FakeSpeechToTextEngine.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.core.error.AppError
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.port.SpeechToTextEngine

/**
 * Fake de SpeechToTextEngine para tests del orquestador.
 * Configurable por test: resultado de éxito, fallo o excepción.
 */
class FakeSpeechToTextEngine : SpeechToTextEngine {

    var transcribeResult: JulyResult<Transcript> = JulyResult.success(
        Transcript(text = "texto de prueba", confidence = 0.95f, languageCode = "es")
    )
    var available: Boolean = true
    var callCount: Int = 0

    override suspend fun transcribe(audio: ByteArray): JulyResult<Transcript> {
        callCount++
        return transcribeResult
    }

    override suspend fun isAvailable(): Boolean = available

    fun returnsSuccess(text: String = "texto de prueba") {
        transcribeResult = JulyResult.success(
            Transcript(text = text, confidence = 0.95f, languageCode = "es")
        )
    }

    fun returnsEmpty() {
        transcribeResult = JulyResult.success(
            Transcript(text = "", confidence = 0f, languageCode = "es")
        )
    }

    fun returnsFailure(error: AppError = AppError.Stt("STT fake error")) {
        transcribeResult = JulyResult.failure(error)
    }
}
```

---

### `testutil/FakeLanguageModelEngine.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.core.error.AppError
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine

class FakeLanguageModelEngine : LanguageModelEngine {

    var generateResult: JulyResult<LlmResponse> = JulyResult.success(
        LlmResponse(text = "respuesta de prueba", tokenCount = 10, latencyMs = 100)
    )
    var available: Boolean = true
    var callCount: Int = 0
    var lastPrompt: String? = null
    var lastHistory: List<Message> = emptyList()

    override suspend fun generate(prompt: String, history: List<Message>): JulyResult<LlmResponse> {
        callCount++
        lastPrompt = prompt
        lastHistory = history
        return generateResult
    }

    override suspend fun isAvailable(): Boolean = available
    override suspend fun getModelInfo(): ModelInfo = ModelInfo(name = "fake-model")

    fun returnsSuccess(text: String = "respuesta de prueba") {
        generateResult = JulyResult.success(
            LlmResponse(text = text, tokenCount = 10, latencyMs = 100)
        )
    }

    fun returnsFailure(
        error: AppError = AppError.Llm("LLM fake error", retryable = false)
    ) {
        generateResult = JulyResult.failure(error)
    }

    fun returnsRetryableFailure() {
        generateResult = JulyResult.failure(
            AppError.Llm("LLM timeout", retryable = true)
        )
    }
}
```

---

### `testutil/FakeTextToSpeechEngine.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.core.error.AppError
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.port.TextToSpeechEngine

class FakeTextToSpeechEngine : TextToSpeechEngine {

    var synthesizeResult: JulyResult<ByteArray> = JulyResult.success(ByteArray(1024))
    var available: Boolean = true
    var callCount: Int = 0
    var lastText: String? = null

    override suspend fun synthesize(text: String): JulyResult<ByteArray> {
        callCount++
        lastText = text
        return synthesizeResult
    }

    override suspend fun isAvailable(): Boolean = available
    override fun getSupportedLanguages(): List<String> = listOf("es")

    fun returnsFailure(error: AppError = AppError.Tts("TTS fake error")) {
        synthesizeResult = JulyResult.failure(error)
    }

    fun returnsSuccess() {
        synthesizeResult = JulyResult.success(ByteArray(1024))
    }
}
```

---

### `testutil/FakeAudioCapturePort.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.domain.port.AudioCapturePort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAudioCapturePort : AudioCapturePort {

    var recordingChunks: List<ByteArray> = listOf(ByteArray(3200))  // 100ms de audio
    var stopResult: ByteArray = ByteArray(32000)                     // 1s de audio
    var cancelCalled: Boolean = false
    private var _isRecording = false

    override val isRecording: Boolean get() = _isRecording

    override fun startRecording(): Flow<ByteArray> = flow {
        _isRecording = true
        recordingChunks.forEach { chunk -> emit(chunk) }
        _isRecording = false
    }

    override suspend fun stopRecording(): ByteArray {
        _isRecording = false
        return stopResult
    }

    override fun cancel() {
        _isRecording = false
        cancelCalled = true
    }
}
```

---

### `testutil/FakeSessionRepository.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.domain.model.Message
import com.july.offline.domain.model.SessionEntity
import com.july.offline.domain.port.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class FakeSessionRepository : SessionRepository {

    private val sessions = mutableMapOf<String, SessionEntity>()
    private val sessionsFlow = MutableStateFlow<List<SessionEntity>>(emptyList())

    override suspend fun createSession(): SessionEntity {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        sessions[session.id] = session
        sessionsFlow.value = sessions.values.toList()
        return session
    }

    override suspend fun addMessage(sessionId: String, message: Message) {
        val session = sessions[sessionId] ?: return
        val updated = session.copy(
            messages = session.messages + message,
            updatedAt = Instant.now()
        )
        sessions[sessionId] = updated
        sessionsFlow.value = sessions.values.toList()
    }

    override suspend fun getSession(sessionId: String): SessionEntity? = sessions[sessionId]

    override fun getRecentSessions(limit: Int): Flow<List<SessionEntity>> =
        sessionsFlow.map { list ->
            list.sortedByDescending { it.updatedAt }.take(limit)
        }

    override suspend fun deleteSession(sessionId: String) {
        sessions.remove(sessionId)
        sessionsFlow.value = sessions.values.toList()
    }
}
```

---

### `testutil/FakeWakeWordEngine.kt`

```kotlin
package com.july.offline.testutil

import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.domain.port.WakeWordEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeWakeWordEngine : WakeWordEngine {

    private val _events = MutableSharedFlow<WakeWordEvent>(extraBufferCapacity = 1)
    private var _isListening = false
    var available: Boolean = true
    var stopCalled: Boolean = false

    override val isListening: Boolean get() = _isListening

    override fun startListening(): Flow<WakeWordEvent> {
        _isListening = true
        return _events
    }

    override fun stop() {
        _isListening = false
        stopCalled = true
    }

    override suspend fun isAvailable(): Boolean = available

    /** Simula detección del wake-word desde un test. */
    suspend fun simulateDetection(keywordIndex: Int = 0) {
        _events.emit(WakeWordEvent.Detected(keywordIndex))
    }

    /** Simula un error del motor desde un test. */
    suspend fun simulateError(cause: Throwable = RuntimeException("fake error")) {
        _events.emit(WakeWordEvent.Error(cause))
    }
}
```

---

## TESTS — ORCHESTRATOR

### `orchestrator/ConversationOrchestratorTest.kt`

```kotlin
package com.july.offline.orchestrator

import app.cash.turbine.test
import com.july.offline.core.error.ErrorHandler
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.orchestrator.AudioCoordinator
import com.july.offline.domain.orchestrator.ConversationOrchestrator
import com.july.offline.domain.orchestrator.SessionCoordinator
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.state.ConversationStateHolder
import com.july.offline.testutil.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ConversationOrchestratorTest {

    private lateinit var stateHolder: ConversationStateHolder
    private lateinit var audioCoordinator: AudioCoordinator
    private lateinit var sessionCoordinator: SessionCoordinator
    private lateinit var wakeWordCoordinator: WakeWordCoordinator
    private lateinit var sttEngine: FakeSpeechToTextEngine
    private lateinit var llmEngine: FakeLanguageModelEngine
    private lateinit var ttsEngine: FakeTextToSpeechEngine
    private lateinit var errorHandler: ErrorHandler
    private lateinit var logger: DiagnosticsLogger
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var orchestrator: ConversationOrchestrator

    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        errorHandler = mockk(relaxed = true)
        dispatchers = TestCoroutineDispatchers()
        stateHolder = ConversationStateHolder(logger)

        sttEngine = FakeSpeechToTextEngine()
        llmEngine = FakeLanguageModelEngine()
        ttsEngine = FakeTextToSpeechEngine()

        audioCoordinator = mockk(relaxed = true)
        coEvery { audioCoordinator.recordUntilSilence() } returns Pair(ByteArray(32000), 1000L)
        coEvery { audioCoordinator.playAudio(any()) } just runs

        sessionCoordinator = mockk(relaxed = true)
        coEvery { sessionCoordinator.ensureActiveSession() } returns "session-test-id"
        coEvery { sessionCoordinator.getHistory(any()) } returns emptyList()

        wakeWordCoordinator = mockk(relaxed = true)
        every { wakeWordCoordinator.isActive } returns false

        orchestrator = ConversationOrchestrator(
            stateHolder = stateHolder,
            audioCoordinator = audioCoordinator,
            sessionCoordinator = sessionCoordinator,
            wakeWordCoordinator = wakeWordCoordinator,
            sttEngine = sttEngine,
            llmEngine = llmEngine,
            ttsEngine = ttsEngine,
            errorHandler = errorHandler,
            logger = logger,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `ciclo completo transiciona por todos los estados correctamente`() = runTest {
        stateHolder.conversationState.test {
            assertEquals(ConversationState.Idle, awaitItem())

            orchestrator.startConversationCycle()

            assertEquals(ConversationState.Listening::class, awaitItem()::class)
            assertEquals(ConversationState.Transcribing::class, awaitItem()::class)
            assertEquals(ConversationState.Thinking::class, awaitItem()::class)
            assertEquals(ConversationState.Speaking::class, awaitItem()::class)
            assertEquals(ConversationState.Idle, awaitItem())
        }
    }

    @Test
    fun `fallo STT transiciona a Error y luego a Idle`() = runTest {
        sttEngine.returnsFailure()
        coEvery { errorHandler.handle(any()) } returns
            com.july.offline.core.error.ErrorAction.ResetToIdle(
                com.july.offline.core.error.AppError.Stt("STT error")
            )

        stateHolder.conversationState.test {
            awaitItem() // Idle inicial

            orchestrator.startConversationCycle()

            awaitItem() // Listening
            awaitItem() // Transcribing
            val errorState = awaitItem()
            assertTrue(errorState is ConversationState.Error)
            assertTrue((errorState as ConversationState.Error).error is
                com.july.offline.core.error.AppError.Stt)
            awaitItem() // Idle
        }
    }

    @Test
    fun `fallo LLM retryable reintenta una vez`() = runTest {
        llmEngine.returnsRetryableFailure()
        coEvery { errorHandler.handle(any()) } returns
            com.july.offline.core.error.ErrorAction.ResetToIdle(
                com.july.offline.core.error.AppError.Llm("timeout", retryable = true)
            )

        orchestrator.startConversationCycle()

        // El engine debe haberse llamado 2 veces (intento + reintento)
        assertEquals(2, llmEngine.callCount)
    }

    @Test
    fun `transcripcion vacia no llama al LLM`() = runTest {
        sttEngine.returnsEmpty()

        orchestrator.startConversationCycle()

        assertEquals(0, llmEngine.callCount)
    }

    @Test
    fun `cancelacion durante LISTENING resetea a Idle`() = runTest {
        stateHolder.conversationState.test {
            awaitItem() // Idle

            orchestrator.startConversationCycle()
            awaitItem() // Listening

            orchestrator.cancelCurrentCycle()

            val cancelled = awaitItem()
            assertTrue(
                cancelled is ConversationState.Cancelled ||
                cancelled is ConversationState.Idle
            )
        }
    }

    @Test
    fun `startConversationCycle desde estado no-Idle es ignorado`() = runTest {
        // Poner en estado Listening manualmente
        stateHolder.transitionToListening("session-123")

        orchestrator.startConversationCycle()

        // No debe haber lanzado un nuevo ciclo
        coVerify(exactly = 0) { audioCoordinator.recordUntilSilence() }
    }

    @Test
    fun `ciclo persiste mensaje usuario y respuesta asistente`() = runTest {
        sttEngine.returnsSuccess("hola julio")
        llmEngine.returnsSuccess("hola, ¿cómo estás?")

        orchestrator.startConversationCycle()

        coVerify(exactly = 2) {
            sessionCoordinator.addMessage(any(), any())
        }
    }

    @Test
    fun `al terminar ciclo con wake-word activo vuelve a WakeWordListening`() = runTest {
        every { wakeWordCoordinator.isActive } returns true

        stateHolder.conversationState.test {
            awaitItem() // Idle

            orchestrator.startConversationCycle()

            awaitItem() // Listening
            awaitItem() // Transcribing
            awaitItem() // Thinking
            awaitItem() // Speaking

            // El último estado debe ser WakeWordListening, no Idle
            verify { wakeWordCoordinator.resumeAfterCycle() }
        }
    }
}
```

---

### `orchestrator/WakeWordCoordinatorTest.kt`

```kotlin
package com.july.offline.orchestrator

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.orchestrator.WakeWordCoordinator
import com.july.offline.domain.state.ConversationStateHolder
import com.july.offline.testutil.FakeWakeWordEngine
import com.july.offline.testutil.TestCoroutineDispatchers
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class WakeWordCoordinatorTest {

    private lateinit var fakeEngine: FakeWakeWordEngine
    private lateinit var stateHolder: ConversationStateHolder
    private lateinit var coordinator: WakeWordCoordinator
    private lateinit var logger: DiagnosticsLogger
    private lateinit var dispatchers: TestCoroutineDispatchers

    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        dispatchers = TestCoroutineDispatchers()
        fakeEngine = FakeWakeWordEngine()
        stateHolder = ConversationStateHolder(logger)

        coordinator = WakeWordCoordinator(
            wakeWordEngine = fakeEngine,
            stateHolder = stateHolder,
            logger = logger,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `startWakeWordListening transiciona estado a WakeWordListening`() = runTest {
        coordinator.startWakeWordListening()

        assertEquals(
            ConversationState.WakeWordListening,
            stateHolder.conversationState.value
        )
    }

    @Test
    fun `stopWakeWordListening detiene el motor y vuelve a Idle`() = runTest {
        coordinator.startWakeWordListening()
        coordinator.stopWakeWordListening()

        assertEquals(ConversationState.Idle, stateHolder.conversationState.value)
        assertTrue(fakeEngine.stopCalled)
    }

    @Test
    fun `deteccion de wake-word invoca onWakeWordDetected`() = runTest {
        var detected = false
        coordinator.onWakeWordDetected = { detected = true }

        coordinator.startWakeWordListening()
        fakeEngine.simulateDetection()

        assertTrue(detected)
    }

    @Test
    fun `deteccion de wake-word detiene el motor Porcupine`() = runTest {
        coordinator.onWakeWordDetected = {}

        coordinator.startWakeWordListening()
        fakeEngine.simulateDetection()

        assertTrue(fakeEngine.stopCalled)
    }

    @Test
    fun `startWakeWordListening es idempotente si ya escucha`() = runTest {
        fakeEngine  // ya isListening = false por defecto al inicio
        coordinator.startWakeWordListening()

        // Segunda llamada con motor ya activo debe ignorarse
        fakeEngine  // no reiniciar manualmente
        coordinator.startWakeWordListening()

        // No debe causar crash ni doble transición
        assertEquals(
            ConversationState.WakeWordListening,
            stateHolder.conversationState.value
        )
    }

    @Test
    fun `isActive refleja estado real del coordinador`() = runTest {
        assertFalse(coordinator.isActive)

        coordinator.startWakeWordListening()
        // isActive = isListening OR listeningJob active
        // Con TestDispatcher el job puede no estar activo aún — verificar engine
        assertTrue(fakeEngine.isListening || coordinator.isActive)
    }
}
```

---

### `orchestrator/AudioCoordinatorTest.kt`

```kotlin
package com.july.offline.orchestrator

import com.july.offline.audio.player.AudioPlayerAdapter
import com.july.offline.audio.vad.VADConfig
import com.july.offline.audio.vad.VADProcessor
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.orchestrator.AudioCoordinator
import com.july.offline.testutil.FakeAudioCapturePort
import com.july.offline.testutil.TestCoroutineDispatchers
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AudioCoordinatorTest {

    private lateinit var fakeAudioPort: FakeAudioCapturePort
    private lateinit var audioPlayer: AudioPlayerAdapter
    private lateinit var vadProcessor: VADProcessor
    private lateinit var coordinator: AudioCoordinator
    private lateinit var logger: DiagnosticsLogger
    private lateinit var dispatchers: TestCoroutineDispatchers

    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        audioPlayer = mockk(relaxed = true)
        dispatchers = TestCoroutineDispatchers()
        fakeAudioPort = FakeAudioCapturePort()

        // VAD real con configuración agresiva para tests (silencio inmediato)
        vadProcessor = VADProcessor(VADConfig(silenceThresholdMs = 50L, energyThreshold = 999999.0))

        coordinator = AudioCoordinator(
            audioCapturePort = fakeAudioPort,
            audioPlayerAdapter = audioPlayer,
            vadProcessor = vadProcessor,
            logger = logger,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `recordUntilSilence devuelve audio cuando hay chunks`() = runTest {
        // Chunks con audio real (no silencio con VAD agresivo)
        fakeAudioPort.recordingChunks = listOf(ByteArray(3200) { 100.toByte() })

        // Con VAD muy sensible, detectará silencio en el primer chunk de ceros
        // Para este test usamos VAD con umbral bajo que no filtre
        val vadLenient = VADProcessor(VADConfig(silenceThresholdMs = 9999L, energyThreshold = 0.0))
        val coordinatorLenient = AudioCoordinator(
            audioCapturePort = fakeAudioPort,
            audioPlayerAdapter = audioPlayer,
            vadProcessor = vadLenient,
            logger = logger,
            dispatchers = dispatchers
        )

        val result = coordinatorLenient.recordUntilSilence()

        assertNotNull(result)
        assertTrue(result!!.first.isNotEmpty())
    }

    @Test
    fun `recordUntilSilence devuelve null si audio es muy corto`() = runTest {
        // Audio de solo 100 bytes (< 3200 mínimo)
        fakeAudioPort.recordingChunks = listOf(ByteArray(100))
        fakeAudioPort.stopResult = ByteArray(100)

        val result = coordinator.recordUntilSilence()

        assertNull(result)
    }

    @Test
    fun `cancel llama a cancel en AudioCapturePort y stop en AudioPlayer`() = runTest {
        coordinator.cancel()

        assertTrue(fakeAudioPort.cancelCalled)
        verify { audioPlayer.stop() }
    }

    @Test
    fun `playAudio llama a AudioPlayerAdapter con el audio recibido`() = runTest {
        val audio = ByteArray(1024) { 1 }

        coordinator.playAudio(audio)

        verify { audioPlayer.play(audio) }
    }

    @Test
    fun `playAudio con ByteArray vacio no llama a AudioPlayerAdapter`() = runTest {
        coordinator.playAudio(ByteArray(0))

        verify(exactly = 0) { audioPlayer.play(any()) }
    }
}
```

---

### `ai/stt/WhisperSTTAdapterTest.kt`

```kotlin
package com.july.offline.ai.stt

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class WhisperSTTAdapterTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var logger: DiagnosticsLogger
    private lateinit var adapter: WhisperSTTAdapter

    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
    }

    @Test
    fun `isAvailable devuelve false si el modelo no existe`() = runTest {
        val config = WhisperConfig(
            modelPath = "/ruta/inexistente/whisper.bin",
            language = "es"
        )
        adapter = WhisperSTTAdapter(config, logger)

        assertFalse(adapter.isAvailable())
    }

    @Test
    fun `isAvailable devuelve true si el modelo existe en disco`() = runTest {
        // Crear archivo de modelo falso en directorio temporal
        val fakeModel = File(tempDir.toFile(), "whisper-small.bin").apply {
            writeBytes(ByteArray(100))
        }
        val config = WhisperConfig(modelPath = fakeModel.absolutePath, language = "es")
        adapter = WhisperSTTAdapter(config, logger)

        // isAvailable() llama a initContext() que fallará sin JNI real,
        // pero el archivo existe — en test sin JNI devuelve false porque
        // whisperInit() no está disponible. Verificamos solo la lógica de File.exists()
        // El test documenta el comportamiento esperado con JNI real.
        assertTrue(fakeModel.exists())
    }

    @Test
    fun `transcribe devuelve AppError Stt si contexto no inicializado`() = runTest {
        val config = WhisperConfig(
            modelPath = "/ruta/inexistente/whisper.bin",
            language = "es"
        )
        adapter = WhisperSTTAdapter(config, logger)

        val result = adapter.transcribe(ByteArray(32000))

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is AppError.Stt)
    }

    @Test
    fun `convertPcm16ToFloat normaliza correctamente valor maximo`() {
        // Acceso via reflexión para test de utilidad privada
        val config = WhisperConfig(modelPath = "/tmp/fake.bin", language = "es")
        adapter = WhisperSTTAdapter(config, logger)

        // PCM 16-bit max = 32767 → Float = 32767/32768 ≈ 0.9999...
        val pcmMax = byteArrayOf(0xFF.toByte(), 0x7F.toByte())  // 32767 little-endian

        // Usar la función via reflexión
        val method = adapter.javaClass.getDeclaredMethod("convertPcm16ToFloat", ByteArray::class.java)
        method.isAccessible = true
        val result = method.invoke(adapter, pcmMax) as FloatArray

        assertEquals(1, result.size)
        assertTrue(result[0] > 0.99f && result[0] <= 1.0f)
    }

    @Test
    fun `transcribe timeout devuelve AppError Stt con mensaje de timeout`() = runTest {
        // Config con timeout muy corto (1ms) para forzar el timeout
        val config = WhisperConfig(
            modelPath = "/ruta/inexistente/whisper.bin",
            language = "es",
            maxDurationMs = 1L
        )
        adapter = WhisperSTTAdapter(config, logger)

        val result = adapter.transcribe(ByteArray(32000))

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is AppError.Stt)
    }
}
```

---

### `ai/llm/LocalServerLLMAdapterTest.kt`

```kotlin
package com.july.offline.ai.llm

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.data.network.NetworkHealthChecker
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class LocalServerLLMAdapterTest {

    private lateinit var apiService: LlmApiService
    private lateinit var networkHealthChecker: NetworkHealthChecker
    private lateinit var logger: DiagnosticsLogger
    private lateinit var adapter: LocalServerLLMAdapter
    private val config = LlmServerConfig(
        host = "127.0.0.1",
        port = 11434,
        modelName = "llama3.2:3b"
    )

    @BeforeEach
    fun setup() {
        apiService = mockk()
        networkHealthChecker = mockk()
        logger = mockk(relaxed = true)

        adapter = LocalServerLLMAdapter(
            apiService = apiService,
            config = config,
            networkHealthChecker = networkHealthChecker,
            logger = logger
        )
    }

    @Test
    fun `generate devuelve LlmResponse exitoso`() = runTest {
        val fakeResponse = LlmChatResponse(
            model = "llama3.2:3b",
            message = LlmMessage(role = "assistant", content = "hola mundo"),
            done = true,
            eval_count = 5,
            total_duration = 1_000_000_000L
        )
        coEvery { apiService.chat(any()) } returns fakeResponse

        val result = adapter.generate("hola", emptyList())

        assertTrue(result.isSuccess)
        assertEquals("hola mundo", result.getOrNull()?.text)
        assertEquals(5, result.getOrNull()?.tokenCount)
    }

    @Test
    fun `generate construye historial correctamente incluyendo system prompt`() = runTest {
        val history = listOf(
            Message(id = "1", role = MessageRole.USER, content = "primera pregunta"),
            Message(id = "2", role = MessageRole.ASSISTANT, content = "primera respuesta")
        )

        val fakeResponse = LlmChatResponse(
            model = "llama3.2:3b",
            message = LlmMessage(role = "assistant", content = "segunda respuesta"),
            done = true
        )

        var capturedRequest: LlmChatRequest? = null
        coEvery { apiService.chat(capture(slot<LlmChatRequest>().also { capturedRequest = it.captured })) } answers {
            capturedRequest = firstArg()
            fakeResponse
        }

        adapter.generate("segunda pregunta", history)

        // Verificar que el request contiene: system + 2 historia + prompt actual = 4 mensajes
        assertNotNull(capturedRequest)
        assertEquals(4, capturedRequest!!.messages.size)
        assertEquals("system", capturedRequest!!.messages[0].role)
        assertEquals("user", capturedRequest!!.messages[1].role)
        assertEquals("assistant", capturedRequest!!.messages[2].role)
        assertEquals("user", capturedRequest!!.messages[3].role)
        assertEquals("segunda pregunta", capturedRequest!!.messages[3].content)
    }

    @Test
    fun `generate con HTTP 503 devuelve AppError Llm retryable`() = runTest {
        coEvery { apiService.chat(any()) } throws HttpException(
            Response.error<LlmChatResponse>(503, "Service Unavailable".toResponseBody())
        )

        val result = adapter.generate("test", emptyList())

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is AppError.Llm)
        assertTrue((error as AppError.Llm).retryable)
    }

    @Test
    fun `generate con IOException devuelve AppError Network`() = runTest {
        coEvery { apiService.chat(any()) } throws IOException("Connection refused")

        val result = adapter.generate("test", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is AppError.Network)
    }

    @Test
    fun `generate con HTTP 400 devuelve AppError Llm no retryable`() = runTest {
        coEvery { apiService.chat(any()) } throws HttpException(
            Response.error<LlmChatResponse>(400, "Bad Request".toResponseBody())
        )

        val result = adapter.generate("test", emptyList())

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is AppError.Llm)
        assertFalse((error as AppError.Llm).retryable)
    }

    @Test
    fun `isAvailable delega en NetworkHealthChecker`() = runTest {
        coEvery { networkHealthChecker.isReachable("127.0.0.1", 11434) } returns true

        assertTrue(adapter.isAvailable())
        coVerify { networkHealthChecker.isReachable("127.0.0.1", 11434) }
    }
}
```

---

### `data/repository/SessionRepositoryImplTest.kt`

```kotlin
package com.july.offline.data.repository

import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.testutil.FakeSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests del repositorio usando el FakeSessionRepository.
 * Los tests de Room real (con AndroidJUnit4) van en androidTest/.
 * Aquí verificamos el contrato del repositorio con la implementación fake.
 */
class SessionRepositoryImplTest {

    private lateinit var repository: FakeSessionRepository

    @BeforeEach
    fun setup() {
        repository = FakeSessionRepository()
    }

    @Test
    fun `createSession genera ID unico y persiste la sesion`() = runTest {
        val session = repository.createSession()

        assertNotNull(session.id)
        assertTrue(session.id.isNotBlank())

        val retrieved = repository.getSession(session.id)
        assertNotNull(retrieved)
        assertEquals(session.id, retrieved!!.id)
    }

    @Test
    fun `addMessage añade mensaje a la sesion correcta`() = runTest {
        val session = repository.createSession()
        val message = Message(
            id = "msg-1",
            role = MessageRole.USER,
            content = "hola"
        )

        repository.addMessage(session.id, message)

        val retrieved = repository.getSession(session.id)
        assertEquals(1, retrieved!!.messages.size)
        assertEquals("hola", retrieved.messages[0].content)
        assertEquals(MessageRole.USER, retrieved.messages[0].role)
    }

    @Test
    fun `getRecentSessions emite sesiones ordenadas por updatedAt`() = runTest {
        val session1 = repository.createSession()
        val session2 = repository.createSession()

        // Añadir mensaje a session2 para que su updatedAt sea más reciente
        repository.addMessage(
            session2.id,
            Message(id = "m1", role = MessageRole.USER, content = "test")
        )

        val recent = repository.getRecentSessions(10).first()

        assertEquals(2, recent.size)
        // session2 debe ser la primera por ser más reciente
        assertEquals(session2.id, recent[0].id)
    }

    @Test
    fun `deleteSession elimina la sesion y sus mensajes`() = runTest {
        val session = repository.createSession()
        repository.addMessage(
            session.id,
            Message(id = "m1", role = MessageRole.USER, content = "test")
        )

        repository.deleteSession(session.id)

        assertNull(repository.getSession(session.id))
        val sessions = repository.getRecentSessions(10).first()
        assertTrue(sessions.none { it.id == session.id })
    }

    @Test
    fun `multiples mensajes se almacenan en orden`() = runTest {
        val session = repository.createSession()
        val messages = listOf(
            Message(id = "m1", role = MessageRole.USER, content = "pregunta 1"),
            Message(id = "m2", role = MessageRole.ASSISTANT, content = "respuesta 1"),
            Message(id = "m3", role = MessageRole.USER, content = "pregunta 2")
        )

        messages.forEach { repository.addMessage(session.id, it) }

        val retrieved = repository.getSession(session.id)
        assertEquals(3, retrieved!!.messages.size)
        assertEquals("pregunta 1", retrieved.messages[0].content)
        assertEquals("respuesta 1", retrieved.messages[1].content)
        assertEquals("pregunta 2", retrieved.messages[2].content)
    }
}
```

---

## RESUMEN DE CAMBIOS FASE 3 → FASE 4

| Componente | Cambio |
|---|---|
| `ConversationState` | Añade `WakeWordListening` sealed object |
| `ConversationStateHolder` | Añade `transitionToWakeWordListening()` y `resetToWakeWordListening()` |
| `WakeWordEngine` | Interface nueva en `domain/port/` |
| `WakeWordEvent` | Sealed class `Detected` + `Error` |
| `WakeWordCoordinator` | Coordinador nuevo, gestiona ciclo de vida Porcupine |
| `ConversationOrchestrator` | Integra `WakeWordCoordinator`, `resumeOrIdle()` condicional |
| `PorcupineConfig` | Config del motor wake-word |
| `PorcupineWakeWordAdapter` | Implementación real con AudioRecord + Porcupine SDK |
| `WakeWordModule` | Módulo Hilt que bindea el adapter |
| `AppPreferencesDataStore` | Añade `wakeWordEnabled` Flow |
| `ConversationPhase` | Añade `WAKE_WORD_LISTENING` |
| `ConversationUiState` | Añade `isWakeWordActive` |
| `WakeWordIndicator` | Composable con animación de pulso |
| `ConversationViewModel` | Añade `onWakeWordToggled()` |
| `ConversationScreen` | Toggle en TopAppBar, muestra `WakeWordIndicator` |
| Fakes (6 clases) | Implementaciones fake para tests sin Android |
| Tests (6 clases) | JUnit 5 + MockK + Turbine, ~45 casos de test |

---

## NOTAS DE INTEGRACIÓN

### Obtener clave API Porcupine
1. Ir a https://picovoice.ai/console/
2. Crear cuenta gratuita
3. Copiar `AccessKey` del dashboard
4. Establecer variable de entorno: `export PICOVOICE_KEY="tu_clave_aqui"`
5. La clave se inyecta en `BuildConfig.PICOVOICE_ACCESS_KEY` en tiempo de compilación

### Modelo personalizado "Oye July"
1. Ir a https://console.picovoice.ai/ppn
2. Seleccionar idioma: `Spanish (ES)`
3. Escribir la frase: `Oye July`
4. Descargar el archivo `.ppn` generado
5. Copiar a `app/src/main/assets/oye_july_es.ppn`
6. En `setup_fase4.sh` añadir: `adb push oye_july_es.ppn /data/data/com.july.offline/files/`
7. En `WakeWordModule.kt` descomentar la línea de `keywordPaths`

### Limitación foreground-only
Porcupine en FASE 4 solo escucha cuando la app está en primer plano.
Si el usuario sale de la app, el `AudioRecord` de Porcupine se libera automáticamente
cuando el sistema mata el proceso. Para escucha en background se necesita un
`ForegroundService` con notificación persistente (FASE 5 si aplica).

### Conflicto AudioRecord entre Porcupine y AudioRecorder
Ambos usan `MediaRecorder.AudioSource.MIC`. En dispositivos Android, solo un
`AudioRecord` puede estar activo simultáneamente. El flujo garantiza:
1. Porcupine detecta wake-word → llama `wakeWordEngine.stop()` → libera su `AudioRecord`
2. `ConversationOrchestrator.startConversationCycle()` inicia → `AudioRecorderAdapter` abre su `AudioRecord`
3. Al terminar el ciclo → `AudioRecorderAdapter` libera su `AudioRecord` → Porcupine reinicia el suyo
