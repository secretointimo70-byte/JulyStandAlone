# JULY OFFLINE — FASE 2
## Implementación Base Completa
### `com.july.offline`

**Versiones:**
- Kotlin 2.0.21
- Compose BOM 2024.11.00
- Hilt 2.52
- Room 2.6.1
- DataStore 1.1.1
- Retrofit 2.11.0 / OkHttp 4.12.0
- Coroutines 1.9.0
- minSdk 26 / targetSdk 35

---

## ÍNDICE DE ARCHIVOS

### CONFIGURACIÓN
- `build.gradle.kts` (proyecto)
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

### CORE
- `core/error/AppError.kt`
- `core/error/ErrorHandler.kt`
- `core/result/JulyResult.kt`
- `core/logging/DiagnosticsLogger.kt`
- `core/coroutines/CoroutineDispatchers.kt`
- `core/extensions/FlowExtensions.kt`

### DOMAIN — MODELS
- `domain/model/ConversationState.kt`
- `domain/model/RuntimeState.kt`
- `domain/model/EngineHealthState.kt`
- `domain/model/SessionEntity.kt`
- `domain/model/Message.kt`
- `domain/model/Transcript.kt`
- `domain/model/LlmResponse.kt`
- `domain/model/ModelInfo.kt`

### DOMAIN — PORTS (interfaces)
- `domain/port/SpeechToTextEngine.kt`
- `domain/port/LanguageModelEngine.kt`
- `domain/port/TextToSpeechEngine.kt`
- `domain/port/AudioCapturePort.kt`
- `domain/port/SessionRepository.kt`

### DOMAIN — STATE
- `domain/state/ConversationStateHolder.kt`

### DOMAIN — ORCHESTRATOR
- `domain/orchestrator/ConversationOrchestrator.kt`
- `domain/orchestrator/AudioCoordinator.kt`
- `domain/orchestrator/SessionCoordinator.kt`
- `domain/orchestrator/EngineHealthMonitor.kt`

### AI (esqueletos con TODOs de JNI/HTTP)
- `ai/stt/WhisperConfig.kt`
- `ai/stt/WhisperSTTAdapter.kt`
- `ai/llm/LlmServerConfig.kt`
- `ai/llm/LlmApiService.kt`
- `ai/llm/LocalServerLLMAdapter.kt`
- `ai/tts/PiperConfig.kt`
- `ai/tts/PiperTTSAdapter.kt`

### AUDIO (esqueletos)
- `audio/recorder/AudioRecorderConfig.kt`
- `audio/recorder/AudioRecorderAdapter.kt`
- `audio/player/AudioPlayerAdapter.kt`
- `audio/vad/VADConfig.kt`
- `audio/vad/VADProcessor.kt`

### DATA
- `data/db/JulyDatabase.kt`
- `data/db/entity/SessionDbEntity.kt`
- `data/db/entity/MessageDbEntity.kt`
- `data/db/entity/DiagnosticsDbEntity.kt`
- `data/db/dao/SessionDao.kt`
- `data/db/dao/MessageDao.kt`
- `data/db/dao/DiagnosticsDao.kt`
- `data/datastore/AppPreferencesDataStore.kt`
- `data/datastore/SystemConfigDataStore.kt`
- `data/network/LocalNetworkClient.kt`
- `data/network/NetworkHealthChecker.kt`
- `data/repository/SessionRepositoryImpl.kt`

### DI
- `di/EngineModule.kt`
- `di/DatabaseModule.kt`
- `di/NetworkModule.kt`
- `di/DataStoreModule.kt`
- `di/AudioModule.kt`
- `di/CoroutineModule.kt`

### SETTINGS
- `settings/AppSettings.kt`

### NAVIGATION
- `navigation/JulyDestination.kt`
- `navigation/JulyNavGraph.kt`

### UI
- `ui/conversation/ConversationUiState.kt`
- `ui/conversation/ConversationViewModel.kt`
- `ui/conversation/ConversationScreen.kt`
- `ui/conversation/components/WaveformIndicator.kt`
- `ui/conversation/components/StatusBar.kt`
- `ui/conversation/components/MessageBubble.kt`
- `ui/conversation/components/EngineHealthWidget.kt`
- `ui/settings/SettingsViewModel.kt`
- `ui/settings/SettingsScreen.kt`

### APP
- `JulyApplication.kt`
- `MainActivity.kt`
- `AndroidManifest.xml`

---

## CONFIGURACIÓN

### `build.gradle.kts` (raíz del proyecto)

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

---

### `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "2.0.21"
agp = "8.7.3"
composeBom = "2024.11.00"
hilt = "2.52"
ksp = "2.0.21-1.0.28"
room = "2.6.1"
datastore = "1.1.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
coroutines = "1.9.0"
lifecycle = "2.8.7"
navigation = "2.8.5"
activity = "1.9.3"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }

# Activity / Lifecycle
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Retrofit + OkHttp
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }

# Gson
gson = { group = "com.google.code.gson", name = "gson", version = "2.11.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.july.offline"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.july.offline"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Soporte para binarios JNI nativos (Whisper.cpp, Piper)
    // Los .so se colocan en src/main/jniLibs/{abi}/
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    // Activity / Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)

    // Gson
    implementation(libs.gson)
}
```

---

## CORE

### `core/error/AppError.kt`

```kotlin
package com.july.offline.core.error

/**
 * Jerarquía sellada de errores del dominio.
 * Toda excepción externa debe mapearse a AppError antes de cruzar la frontera de capa.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {

    /** Permiso de micrófono denegado por el sistema operativo. */
    data class Permission(
        override val message: String = "Microphone permission denied"
    ) : AppError(message)

    /** Fallo en el motor de reconocimiento de voz. */
    data class Stt(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /**
     * Fallo en el modelo de lenguaje.
     * @param retryable true si un reintento automático tiene sentido (timeout, 503).
     *                  false para errores permanentes (400, modelo no cargado).
     */
    data class Llm(
        override val message: String,
        override val cause: Throwable? = null,
        val retryable: Boolean = false
    ) : AppError(message, cause)

    /** Fallo en el motor de síntesis de voz. */
    data class Tts(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Servidor LLM local no disponible en 127.0.0.1. */
    data class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** El usuario canceló explícitamente la operación en curso. */
    object Cancelled : AppError("Operation cancelled by user")

    /** Error inesperado no clasificado. */
    data class Unknown(
        override val message: String = "Unknown error",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
```

---

### `core/error/ErrorHandler.kt`

```kotlin
package com.july.offline.core.error

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centraliza la política de manejo de errores:
 * - Decide si reintentar (solo AppError.Llm retryable, máximo 1 intento, backoff 500ms)
 * - Delega logging a DiagnosticsLogger
 * - Devuelve la acción que el orquestador debe ejecutar
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    companion object {
        private const val LLM_RETRY_DELAY_MS = 500L
        private const val MAX_LLM_RETRIES = 1
    }

    /**
     * Maneja un AppError y devuelve la acción recomendada al orquestador.
     */
    suspend fun handle(error: AppError): ErrorAction = withContext(dispatchers.io) {
        logger.logError(
            tag = "ErrorHandler",
            message = error.message,
            cause = error.cause
        )

        when (error) {
            is AppError.Permission -> ErrorAction.ShowPermissionRationale
            is AppError.Stt -> ErrorAction.ResetToIdle(error)
            is AppError.Llm -> {
                if (error.retryable) ErrorAction.Retry(delayMs = LLM_RETRY_DELAY_MS)
                else ErrorAction.ResetToIdle(error)
            }
            is AppError.Tts -> ErrorAction.FallbackToText(error)
            is AppError.Network -> ErrorAction.ResetToIdle(error)
            is AppError.Cancelled -> ErrorAction.ResetToIdle(error)
            is AppError.Unknown -> ErrorAction.ResetToIdle(error)
        }
    }

    /** Ejecuta un bloque con política de reintento para errores LLM retryables. */
    suspend fun <T> withLlmRetry(block: suspend () -> T): Result<T> {
        repeat(MAX_LLM_RETRIES + 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == MAX_LLM_RETRIES) {
                    return Result.failure(e)
                }
                delay(LLM_RETRY_DELAY_MS)
            }
        }
        return Result.failure(IllegalStateException("Retry loop exhausted"))
    }
}

/** Acción que el orquestador ejecuta tras recibir el resultado de ErrorHandler. */
sealed class ErrorAction {
    /** Resetear estado a IDLE y mostrar el error en UI. */
    data class ResetToIdle(val error: AppError) : ErrorAction()
    /** Reintentar la operación tras un delay. */
    data class Retry(val delayMs: Long) : ErrorAction()
    /** TTS falló pero LLM respondió: mostrar texto como fallback. */
    data class FallbackToText(val error: AppError) : ErrorAction()
    /** Mostrar explicación de permisos al usuario. */
    object ShowPermissionRationale : ErrorAction()
}
```

---

### `core/result/JulyResult.kt`

```kotlin
package com.july.offline.core.result

import com.july.offline.core.error.AppError

/**
 * Resultado tipado que fuerza el manejo explícito de errores en los contratos del dominio.
 * Las interfaces de motores devuelven JulyResult en lugar de lanzar excepciones.
 */
sealed class JulyResult<out T> {
    data class Success<T>(val data: T) : JulyResult<T>()
    data class Failure(val error: AppError) : JulyResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data

    fun errorOrNull(): AppError? = (this as? Failure)?.error

    inline fun onSuccess(action: (T) -> Unit): JulyResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (AppError) -> Unit): JulyResult<T> {
        if (this is Failure) action(error)
        return this
    }

    inline fun <R> map(transform: (T) -> R): JulyResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    companion object {
        fun <T> success(data: T): JulyResult<T> = Success(data)
        fun failure(error: AppError): JulyResult<Nothing> = Failure(error)
    }
}
```

---

### `core/logging/DiagnosticsLogger.kt`

```kotlin
package com.july.offline.core.logging

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger estructurado del sistema.
 * En FASE 2 escribe en Logcat. En FASE 3 persistirá en DiagnosticsDao.
 * No contiene lógica de negocio. Solo registra hechos.
 */
@Singleton
class DiagnosticsLogger @Inject constructor() {

    companion object {
        private const val APP_TAG = "JulyOffline"
    }

    fun logInfo(tag: String, message: String) {
        Log.i(APP_TAG, "[$tag] $message")
    }

    fun logDebug(tag: String, message: String) {
        Log.d(APP_TAG, "[$tag] $message")
    }

    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        Log.w(APP_TAG, "[$tag] $message", cause)
    }

    fun logError(tag: String, message: String, cause: Throwable? = null) {
        Log.e(APP_TAG, "[$tag] $message", cause)
    }

    fun logStateTransition(from: String, to: String, trigger: String) {
        Log.i(APP_TAG, "[State] $from → $to (trigger: $trigger)")
    }

    fun logEngineEvent(engine: String, event: String, latencyMs: Long? = null) {
        val latencyStr = latencyMs?.let { " [${it}ms]" } ?: ""
        Log.i(APP_TAG, "[Engine:$engine] $event$latencyStr")
    }
}
```

---

### `core/coroutines/CoroutineDispatchers.kt`

```kotlin
package com.july.offline.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstracción de dispatchers para permitir sustitución en tests.
 * Inyectado por Hilt en todas las clases que necesiten control de threading.
 */
@Singleton
class CoroutineDispatchers @Inject constructor() {
    val main: CoroutineDispatcher = Dispatchers.Main
    val io: CoroutineDispatcher = Dispatchers.IO
    val default: CoroutineDispatcher = Dispatchers.Default
    val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
```

---

### `core/extensions/FlowExtensions.kt`

```kotlin
package com.july.offline.core.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.july.offline.core.error.AppError
import com.july.offline.core.result.JulyResult

/**
 * Operadores utilitarios sobre Flow para uso interno del dominio.
 */

/** Envuelve cada emisión en JulyResult.Success y captura excepciones como JulyResult.Failure. */
fun <T> Flow<T>.asJulyResult(
    mapError: (Throwable) -> AppError = { AppError.Unknown(it.message ?: "Unknown", it) }
): Flow<JulyResult<T>> = map<T, JulyResult<T>> { JulyResult.success(it) }
    .catch { emit(JulyResult.failure(mapError(it))) }
```

---

## DOMAIN — MODELS

### `domain/model/Message.kt`

```kotlin
package com.july.offline.domain.model

import java.time.Instant

/** Rol del emisor del mensaje en la conversación. */
enum class MessageRole { USER, ASSISTANT, SYSTEM }

/**
 * Mensaje individual de la conversación.
 * Inmutable. Toda modificación produce una nueva instancia.
 */
data class Message(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant = Instant.now()
)
```

---

### `domain/model/Transcript.kt`

```kotlin
package com.july.offline.domain.model

/**
 * Resultado del motor STT.
 * @param text texto reconocido
 * @param confidence valor entre 0.0 y 1.0 (-1.0 si el motor no lo provee)
 * @param languageCode código ISO 639-1 del idioma detectado (ej: "es", "en")
 * @param durationMs duración del audio procesado en milisegundos
 */
data class Transcript(
    val text: String,
    val confidence: Float = -1f,
    val languageCode: String = "es",
    val durationMs: Long = 0L
) {
    val isEmpty: Boolean get() = text.isBlank()
}
```

---

### `domain/model/LlmResponse.kt`

```kotlin
package com.july.offline.domain.model

/**
 * Respuesta del motor LLM.
 * @param text texto generado por el modelo
 * @param tokenCount número de tokens generados (-1 si no disponible)
 * @param latencyMs tiempo total de generación en milisegundos
 * @param modelName nombre del modelo que generó la respuesta
 */
data class LlmResponse(
    val text: String,
    val tokenCount: Int = -1,
    val latencyMs: Long = 0L,
    val modelName: String = ""
) {
    val isEmpty: Boolean get() = text.isBlank()
}
```

---

### `domain/model/ModelInfo.kt`

```kotlin
package com.july.offline.domain.model

/**
 * Metadatos del modelo LLM activo.
 */
data class ModelInfo(
    val name: String,
    val version: String = "",
    val parameterCount: String = "",
    val contextLength: Int = 0
)
```

---

### `domain/model/SessionEntity.kt`

```kotlin
package com.july.offline.domain.model

import java.time.Instant

/**
 * Entidad de sesión de conversación en el dominio.
 * Una sesión agrupa todos los mensajes de una conversación continua.
 */
data class SessionEntity(
    val id: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val messages: List<Message> = emptyList(),
    val title: String = ""
) {
    val messageCount: Int get() = messages.size
    val lastMessage: Message? get() = messages.lastOrNull()
}
```

---

### `domain/model/EngineHealthState.kt`

```kotlin
package com.july.offline.domain.model

import java.time.Instant

/** Estado de disponibilidad de un motor individual. */
enum class EngineStatus {
    /** Motor disponible y respondiendo correctamente. */
    READY,
    /** Motor no disponible (proceso caído, binario no encontrado, red inalcanzable). */
    UNAVAILABLE,
    /** Motor disponible pero con degradación (latencia alta, errores intermitentes). */
    DEGRADED,
    /** Estado no verificado aún (arranque de la app). */
    UNKNOWN
}

/**
 * Estado de salud de los tres motores del sistema.
 * Emitido por EngineHealthMonitor cada 30 segundos o ante fallo detectado.
 */
data class EngineHealthState(
    val sttStatus: EngineStatus = EngineStatus.UNKNOWN,
    val llmStatus: EngineStatus = EngineStatus.UNKNOWN,
    val ttsStatus: EngineStatus = EngineStatus.UNKNOWN,
    val lastCheckedAt: Instant = Instant.now()
) {
    val allReady: Boolean
        get() = sttStatus == EngineStatus.READY &&
                llmStatus == EngineStatus.READY &&
                ttsStatus == EngineStatus.READY

    val anyUnavailable: Boolean
        get() = sttStatus == EngineStatus.UNAVAILABLE ||
                llmStatus == EngineStatus.UNAVAILABLE ||
                ttsStatus == EngineStatus.UNAVAILABLE
}
```

---

### `domain/model/ConversationState.kt`

```kotlin
package com.july.offline.domain.model

/**
 * Estados del ciclo de conversación.
 * Solo ConversationOrchestrator puede producir transiciones.
 * La secuencia válida es:
 * Idle → Listening → Transcribing → Thinking → Speaking → Idle
 * Cualquier estado puede transicionar a Error o Cancelled.
 */
sealed class ConversationState {

    /** Sistema en reposo. Esperando activación. */
    object Idle : ConversationState()

    /**
     * Grabando audio del usuario.
     * @param sessionId ID de la sesión activa
     */
    data class Listening(
        val sessionId: String
    ) : ConversationState()

    /**
     * Procesando el audio con el motor STT.
     * @param sessionId ID de la sesión activa
     * @param audioLengthMs duración del audio capturado
     */
    data class Transcribing(
        val sessionId: String,
        val audioLengthMs: Long
    ) : ConversationState()

    /**
     * Generando respuesta con el LLM.
     * @param sessionId ID de la sesión activa
     * @param transcript resultado del STT
     */
    data class Thinking(
        val sessionId: String,
        val transcript: Transcript
    ) : ConversationState()

    /**
     * Reproduciendo respuesta con TTS.
     * @param sessionId ID de la sesión activa
     * @param response respuesta generada por el LLM
     * @param fallbackText texto a mostrar si TTS falla (siempre disponible)
     */
    data class Speaking(
        val sessionId: String,
        val response: LlmResponse,
        val fallbackText: String = response.text
    ) : ConversationState()

    /**
     * Error en alguna etapa del ciclo.
     * @param error error clasificado
     * @param previousState estado en el que ocurrió el error
     */
    data class Error(
        val error: com.july.offline.core.error.AppError,
        val previousState: ConversationState
    ) : ConversationState()

    /** El usuario canceló la operación en curso. */
    object Cancelled : ConversationState()
}
```

---

### `domain/model/RuntimeState.kt`

```kotlin
package com.july.offline.domain.model

import com.july.offline.core.error.AppError
import java.time.Instant

/**
 * Estado de runtime de la sesión activa.
 * Complementa ConversationState con métricas y contexto de sesión.
 */
data class RuntimeState(
    val currentSessionId: String? = null,
    val cycleCount: Int = 0,
    val lastErrorAt: Instant? = null,
    val lastError: AppError? = null,
    val appStartedAt: Instant = Instant.now()
)
```

---

## DOMAIN — PORTS

### `domain/port/SpeechToTextEngine.kt`

```kotlin
package com.july.offline.domain.port

import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript

/**
 * Contrato del motor de reconocimiento de voz.
 * Sin dependencias Android. Implementado en capa ai/.
 */
interface SpeechToTextEngine {

    /**
     * Transcribe audio PCM 16-bit a texto.
     * @param audio bytes de audio en formato PCM 16-bit, mono, 16kHz
     * @return Transcript con texto reconocido o AppError clasificado
     */
    suspend fun transcribe(audio: ByteArray): JulyResult<Transcript>

    /**
     * Verifica si el motor está disponible (binario cargado, modelo en memoria).
     * Llamado por EngineHealthMonitor. No lanza excepciones.
     */
    suspend fun isAvailable(): Boolean
}
```

---

### `domain/port/LanguageModelEngine.kt`

```kotlin
package com.july.offline.domain.port

import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.ModelInfo

/**
 * Contrato del motor de modelo de lenguaje.
 * Sin dependencias Android. Implementado en capa ai/.
 */
interface LanguageModelEngine {

    /**
     * Genera una respuesta dado un prompt y el historial de conversación.
     * @param prompt texto del turno actual del usuario (ya transcrito)
     * @param history mensajes previos de la sesión para contexto
     * @return LlmResponse con texto generado o AppError clasificado
     */
    suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse>

    /**
     * Verifica si el servidor LLM local está disponible.
     * Intenta conexión TCP al host/puerto configurado. No lanza excepciones.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Devuelve metadatos del modelo activo.
     * Puede devolver ModelInfo con valores vacíos si el servidor no responde.
     */
    suspend fun getModelInfo(): ModelInfo
}
```

---

### `domain/port/TextToSpeechEngine.kt`

```kotlin
package com.july.offline.domain.port

import com.july.offline.core.result.JulyResult

/**
 * Contrato del motor de síntesis de voz.
 * Sin dependencias Android. Implementado en capa ai/.
 */
interface TextToSpeechEngine {

    /**
     * Sintetiza texto a audio PCM.
     * @param text texto a sintetizar
     * @return ByteArray con audio PCM 16-bit listo para AudioPlayerAdapter, o AppError
     */
    suspend fun synthesize(text: String): JulyResult<ByteArray>

    /**
     * Verifica si el motor TTS está disponible (binario Piper cargado, modelo presente).
     * No lanza excepciones.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Códigos de idioma soportados por el modelo TTS cargado (ej: ["es", "en"]).
     */
    fun getSupportedLanguages(): List<String>
}
```

---

### `domain/port/AudioCapturePort.kt`

```kotlin
package com.july.offline.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de captura de audio del micrófono.
 * Implementado en capa audio/. Consume AudioRecorder del sistema.
 */
interface AudioCapturePort {

    /**
     * Inicia la grabación y emite chunks de audio PCM en tiempo real.
     * El Flow completa cuando se llama stopRecording() o cancel().
     * @return Flow de chunks ByteArray PCM 16-bit
     */
    fun startRecording(): Flow<ByteArray>

    /**
     * Detiene la grabación y devuelve el audio completo concatenado.
     * Idempotente: llamadas adicionales devuelven ByteArray vacío.
     */
    suspend fun stopRecording(): ByteArray

    /** Cancela la grabación sin devolver audio. Libera recursos inmediatamente. */
    fun cancel()

    /** true si hay grabación activa en este momento. */
    val isRecording: Boolean
}
```

---

### `domain/port/SessionRepository.kt`

```kotlin
package com.july.offline.domain.port

import com.july.offline.domain.model.Message
import com.july.offline.domain.model.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistencia de sesiones y mensajes.
 * Implementado en capa data/.
 */
interface SessionRepository {

    /** Crea una nueva sesión y la persiste. Devuelve la entidad creada. */
    suspend fun createSession(): SessionEntity

    /** Añade un mensaje a una sesión existente. */
    suspend fun addMessage(sessionId: String, message: Message)

    /** Recupera una sesión por ID incluyendo sus mensajes. null si no existe. */
    suspend fun getSession(sessionId: String): SessionEntity?

    /**
     * Flow reactivo de sesiones recientes ordenadas por updatedAt DESC.
     * @param limit número máximo de sesiones a devolver
     */
    fun getRecentSessions(limit: Int = 20): Flow<List<SessionEntity>>

    /** Elimina una sesión y todos sus mensajes. */
    suspend fun deleteSession(sessionId: String)
}
```

---

## DOMAIN — STATE

### `domain/state/ConversationStateHolder.kt`

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

/**
 * FUENTE ÚNICA DE VERDAD del estado del sistema.
 *
 * Singleton inyectado por Hilt. Solo ConversationOrchestrator debe llamar
 * a los métodos de mutación. La UI y ViewModel solo leen los StateFlow.
 *
 * Thread-safe: MutableStateFlow garantiza visibilidad y atomicidad de escrituras.
 * El orquestador escribe siempre desde Dispatchers.Main para evitar races.
 */
@Singleton
class ConversationStateHolder @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    private val _runtimeState = MutableStateFlow(RuntimeState())
    val runtimeState: StateFlow<RuntimeState> = _runtimeState.asStateFlow()

    // ── Transiciones de estado conversacional ─────────────────────────────

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

    // ── Mutaciones de RuntimeState ────────────────────────────────────────

    internal fun setCurrentSession(sessionId: String?) {
        _runtimeState.update { it.copy(currentSessionId = sessionId) }
    }

    internal fun incrementCycleCount() {
        _runtimeState.update { it.copy(cycleCount = it.cycleCount + 1) }
    }
}
```

---

## DOMAIN — ORCHESTRATOR

### `domain/orchestrator/ConversationOrchestrator.kt`

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
 * Orquestador central del ciclo de conversación.
 *
 * ÚNICO actor autorizado a llamar métodos de mutación de ConversationStateHolder.
 * Coordina en orden: VAD_END → STT → LLM → TTS → IDLE.
 * Maneja errores mediante ErrorHandler. Delega audio a AudioCoordinator
 * y persistencia a SessionCoordinator.
 */
@Singleton
class ConversationOrchestrator @Inject constructor(
    private val stateHolder: ConversationStateHolder,
    private val audioCoordinator: AudioCoordinator,
    private val sessionCoordinator: SessionCoordinator,
    private val sttEngine: SpeechToTextEngine,
    private val llmEngine: LanguageModelEngine,
    private val ttsEngine: TextToSpeechEngine,
    private val errorHandler: ErrorHandler,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private var activeJob: Job? = null

    /**
     * Inicia un ciclo de conversación completo.
     * Si ya hay un ciclo activo, lo ignora (el usuario debe cancelar primero).
     */
    fun startConversationCycle() {
        if (stateHolder.conversationState.value !is ConversationState.Idle) {
            logger.logWarning("Orchestrator", "startConversationCycle called in non-Idle state, ignoring")
            return
        }

        activeJob = scope.launch {
            runCycle()
        }
    }

    /** Cancela el ciclo activo en cualquier punto. Resetea a IDLE. */
    fun cancelCurrentCycle() {
        activeJob?.cancel()
        activeJob = null
        audioCoordinator.cancel()
        stateHolder.transitionToCancelled()
        scope.launch {
            delay(100)
            stateHolder.resetToIdle()
        }
        logger.logInfo("Orchestrator", "Cycle cancelled by user")
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
            stateHolder.resetToIdle()
            return@withContext
        }

        val audioBytes = audioResult.first
        val audioLengthMs = audioResult.second

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
            logger.logInfo("Orchestrator", "Empty transcript, resetting to Idle")
            stateHolder.resetToIdle()
            return@withContext
        }

        // Persistir mensaje del usuario
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
                // Intento de reintento si es retryable
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

        // Persistir respuesta del asistente
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
                // Fallback: TTS falla pero la respuesta ya está en Speaking.fallbackText
                logger.logWarning("Orchestrator", "TTS failed, showing text fallback")
                val action = errorHandler.handle(ttsResult.error)
                if (action is ErrorAction.FallbackToText) {
                    // La UI ya muestra fallbackText desde ConversationState.Speaking
                    delay(2000L) // Dar tiempo al usuario para leer
                }
            }
        }

        // ── 5. RESET → IDLE ────────────────────────────────────────────
        stateHolder.incrementCycleCount()
        stateHolder.resetToIdle()
    }

    private suspend fun handleError(error: AppError) {
        stateHolder.transitionToError(error)
        when (val action = errorHandler.handle(error)) {
            is ErrorAction.ResetToIdle -> {
                delay(300L)
                stateHolder.resetToIdle()
            }
            is ErrorAction.ShowPermissionRationale -> {
                // La UI observa ConversationState.Error(AppError.Permission)
                // y muestra el diálogo de permisos
            }
            is ErrorAction.FallbackToText,
            is ErrorAction.Retry -> {
                delay(300L)
                stateHolder.resetToIdle()
            }
        }
    }
}
```

---

### `domain/orchestrator/AudioCoordinator.kt`

```kotlin
package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.AudioCapturePort
import com.july.offline.audio.player.AudioPlayerAdapter
import com.july.offline.audio.vad.VADProcessor
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinador de audio.
 * Gestiona el ciclo de grabación con detección de silencio (VAD).
 * Devuelve el audio completo al orquestador cuando detecta fin de voz.
 * No transiciona estados de conversación.
 */
@Singleton
class AudioCoordinator @Inject constructor(
    private val audioCapturePort: AudioCapturePort,
    private val audioPlayerAdapter: AudioPlayerAdapter,
    private val vadProcessor: VADProcessor,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    /**
     * Graba hasta detectar silencio via VAD.
     * @return Pair<ByteArray, Long> con audio PCM y duración en ms, o null si se canceló.
     */
    suspend fun recordUntilSilence(): Pair<ByteArray, Long>? = withContext(dispatchers.io) {
        val startTime = System.currentTimeMillis()
        val audioBuffer = mutableListOf<Byte>()

        try {
            audioCapturePort.startRecording().collect { chunk ->
                audioBuffer.addAll(chunk.toList())
                val isSilent = vadProcessor.isSilence(chunk)
                if (isSilent) {
                    // VAD detectó fin de voz
                    return@collect
                }
            }

            val audioBytes = audioBuffer.toByteArray()
            val durationMs = System.currentTimeMillis() - startTime

            if (audioBytes.isEmpty()) {
                logger.logWarning("AudioCoordinator", "Empty audio captured")
                return@withContext null
            }

            logger.logInfo("AudioCoordinator", "Captured ${audioBytes.size} bytes in ${durationMs}ms")
            Pair(audioBytes, durationMs)

        } catch (e: Exception) {
            logger.logError("AudioCoordinator", "Recording failed", e)
            null
        }
    }

    /** Reproduce audio PCM via AudioPlayerAdapter. */
    suspend fun playAudio(audio: ByteArray) = withContext(dispatchers.io) {
        audioPlayerAdapter.play(audio)
    }

    /** Cancela grabación y reproducción activas. */
    fun cancel() {
        audioCapturePort.cancel()
        audioPlayerAdapter.stop()
        logger.logInfo("AudioCoordinator", "Cancelled")
    }
}
```

---

### `domain/orchestrator/SessionCoordinator.kt`

```kotlin
package com.july.offline.domain.orchestrator

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.SessionEntity
import com.july.offline.domain.port.SessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinador de sesiones.
 * Crea y mantiene la sesión activa. Persiste mensajes.
 * No transiciona estados de conversación.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logger: DiagnosticsLogger
) {

    private var currentSession: SessionEntity? = null

    /**
     * Garantiza que hay una sesión activa.
     * Si no existe, crea una nueva.
     * @return ID de la sesión activa
     */
    suspend fun ensureActiveSession(): String {
        val session = currentSession ?: sessionRepository.createSession().also {
            currentSession = it
            logger.logInfo("SessionCoordinator", "Created session: ${it.id}")
        }
        return session.id
    }

    /** Añade un mensaje a la sesión activa y actualiza la caché local. */
    suspend fun addMessage(sessionId: String, message: Message) {
        sessionRepository.addMessage(sessionId, message)
        currentSession = currentSession?.let { session ->
            session.copy(messages = session.messages + message)
        }
        logger.logInfo("SessionCoordinator", "Added ${message.role} message to $sessionId")
    }

    /**
     * Devuelve el historial de mensajes de la sesión activa.
     * Usado por el orquestador para construir el contexto del LLM.
     */
    suspend fun getHistory(sessionId: String): List<Message> {
        return currentSession?.messages
            ?: sessionRepository.getSession(sessionId)?.messages
            ?: emptyList()
    }

    /** Cierra la sesión activa. El próximo ensureActiveSession() creará una nueva. */
    fun closeCurrentSession() {
        logger.logInfo("SessionCoordinator", "Closing session: ${currentSession?.id}")
        currentSession = null
    }
}
```

---

### `domain/orchestrator/EngineHealthMonitor.kt`

```kotlin
package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.EngineHealthState
import com.july.offline.domain.model.EngineStatus
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor de salud de los tres motores del sistema.
 * Verifica disponibilidad cada 30 segundos de forma continua.
 * Expone EngineHealthState como StateFlow observable por la UI.
 */
@Singleton
class EngineHealthMonitor @Inject constructor(
    private val sttEngine: SpeechToTextEngine,
    private val llmEngine: LanguageModelEngine,
    private val ttsEngine: TextToSpeechEngine,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {

    companion object {
        private const val CHECK_INTERVAL_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _healthState = MutableStateFlow(EngineHealthState())
    val healthState: StateFlow<EngineHealthState> = _healthState.asStateFlow()

    /** Inicia el ciclo de monitoreo. Llamado desde JulyApplication.onCreate(). */
    fun startMonitoring() {
        scope.launch {
            while (true) {
                checkAll()
                delay(CHECK_INTERVAL_MS)
            }
        }
        logger.logInfo("EngineHealthMonitor", "Monitoring started")
    }

    /** Fuerza una verificación inmediata. Útil tras un error de motor. */
    suspend fun forceCheck() {
        checkAll()
    }

    private suspend fun checkAll() {
        val sttStatus = checkEngine("STT") { sttEngine.isAvailable() }
        val llmStatus = checkEngine("LLM") { llmEngine.isAvailable() }
        val ttsStatus = checkEngine("TTS") { ttsEngine.isAvailable() }

        val newHealth = EngineHealthState(
            sttStatus = sttStatus,
            llmStatus = llmStatus,
            ttsStatus = ttsStatus,
            lastCheckedAt = Instant.now()
        )

        _healthState.value = newHealth

        if (newHealth.anyUnavailable) {
            logger.logWarning(
                "EngineHealthMonitor",
                "Degraded health — STT:$sttStatus LLM:$llmStatus TTS:$ttsStatus"
            )
        }
    }

    private suspend fun checkEngine(name: String, check: suspend () -> Boolean): EngineStatus {
        return try {
            if (check()) EngineStatus.READY else EngineStatus.UNAVAILABLE
        } catch (e: Exception) {
            logger.logError("EngineHealthMonitor", "$name check failed", e)
            EngineStatus.UNAVAILABLE
        }
    }
}
```

---

## AI — ESQUELETOS

### `ai/stt/WhisperConfig.kt`

```kotlin
package com.july.offline.ai.stt

/**
 * Configuración del motor Whisper.cpp.
 * @param modelPath ruta absoluta al archivo .bin del modelo (ej: /data/data/.../whisper-tiny.bin)
 * @param language código ISO 639-1 del idioma ("es", "en", "auto")
 * @param threads número de hilos para inferencia (recomendado: 4)
 * @param maxDurationMs duración máxima de audio a procesar antes de timeout
 */
data class WhisperConfig(
    val modelPath: String,
    val language: String = "es",
    val threads: Int = 4,
    val maxDurationMs: Long = 30_000L
)
```

---

### `ai/stt/WhisperSTTAdapter.kt`

```kotlin
package com.july.offline.ai.stt

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.port.SpeechToTextEngine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Adaptador STT que delega a Whisper.cpp via JNI.
 *
 * INTEGRACIÓN JNI (FASE 3):
 * - Cargar libwhisper.so desde jniLibs/
 * - Llamar a WhisperJNI.transcribe(audio, modelPath, language, threads)
 * - El modelo se carga una vez en memoria y se reutiliza
 *
 * En FASE 2 el método transcribe devuelve un placeholder para permitir compilación.
 */
class WhisperSTTAdapter @Inject constructor(
    private val config: WhisperConfig,
    private val logger: DiagnosticsLogger
) : SpeechToTextEngine {

    // TODO FASE 3: companion object { init { System.loadLibrary("whisper") } }
    // TODO FASE 3: private val whisperContext = WhisperJNI.init(config.modelPath, config.threads)

    override suspend fun transcribe(audio: ByteArray): JulyResult<Transcript> {
        return try {
            withTimeout(config.maxDurationMs) {
                logger.logInfo("WhisperSTT", "Transcribing ${audio.size} bytes")

                // TODO FASE 3: Reemplazar con llamada JNI real:
                // val rawText = WhisperJNI.transcribe(whisperContext, audio, config.language)

                // Placeholder FASE 2 — simulación sin JNI
                val mockText = "[STT_PLACEHOLDER - Whisper JNI not integrated yet]"

                JulyResult.success(
                    Transcript(
                        text = mockText,
                        confidence = -1f,
                        languageCode = config.language,
                        durationMs = audio.size.toLong() / 32 // estimación
                    )
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            JulyResult.failure(AppError.Stt("Whisper timeout after ${config.maxDurationMs}ms", e))
        } catch (e: Exception) {
            JulyResult.failure(AppError.Stt("Whisper transcription failed: ${e.message}", e))
        }
    }

    override suspend fun isAvailable(): Boolean {
        // TODO FASE 3: verificar que whisperContext != null && modelPath existe en disco
        return java.io.File(config.modelPath).exists()
    }
}
```

---

### `ai/llm/LlmServerConfig.kt`

```kotlin
package com.july.offline.ai.llm

/**
 * Configuración del servidor LLM local.
 * Compatible con la API de Ollama y LM Studio.
 * @param host dirección del servidor (siempre 127.0.0.1 en producción)
 * @param port puerto del servidor
 * @param modelName nombre del modelo a usar (ej: "llama3.2:3b", "mistral:7b")
 * @param connectTimeoutSeconds timeout de conexión TCP
 * @param readTimeoutSeconds timeout de lectura de respuesta completa
 * @param systemPrompt prompt de sistema enviado en cada conversación
 */
data class LlmServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 11434,
    val modelName: String = "llama3.2:3b",
    val connectTimeoutSeconds: Long = 5L,
    val readTimeoutSeconds: Long = 60L,
    val systemPrompt: String = "Eres July, un asistente de voz offline. Responde de forma concisa y natural."
) {
    val baseUrl: String get() = "http://$host:$port/"
}
```

---

### `ai/llm/LlmApiService.kt`

```kotlin
package com.july.offline.ai.llm

import retrofit2.http.Body
import retrofit2.http.POST

/** Interfaz Retrofit para el endpoint de chat del servidor LLM local. */
interface LlmApiService {

    /**
     * Endpoint compatible con Ollama /api/chat y LM Studio /v1/chat/completions.
     * Usamos el formato Ollama ya que es más simple para respuestas no-streaming.
     */
    @POST("api/chat")
    suspend fun chat(@Body request: LlmChatRequest): LlmChatResponse
}

data class LlmChatRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val stream: Boolean = false,
    val options: LlmOptions = LlmOptions()
)

data class LlmMessage(
    val role: String,   // "system", "user", "assistant"
    val content: String
)

data class LlmOptions(
    val temperature: Float = 0.7f,
    val num_predict: Int = 512
)

data class LlmChatResponse(
    val model: String,
    val message: LlmMessage,
    val done: Boolean,
    val total_duration: Long = 0L,    // nanosegundos
    val eval_count: Int = 0           // tokens generados
)
```

---

### `ai/llm/LocalServerLLMAdapter.kt`

```kotlin
package com.july.offline.ai.llm

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.data.network.NetworkHealthChecker
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Adaptador LLM que consume un servidor HTTP local compatible con la API de Ollama.
 * Construye el payload con historial + system prompt y mapea la respuesta al dominio.
 */
class LocalServerLLMAdapter @Inject constructor(
    private val apiService: LlmApiService,
    private val config: LlmServerConfig,
    private val networkHealthChecker: NetworkHealthChecker,
    private val logger: DiagnosticsLogger
) : LanguageModelEngine {

    override suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {
        return try {
            val messages = buildMessageList(prompt, history)
            val request = LlmChatRequest(
                model = config.modelName,
                messages = messages,
                stream = false
            )

            logger.logInfo("LocalServerLLM", "Generating for model: ${config.modelName}")
            val startMs = System.currentTimeMillis()
            val response = apiService.chat(request)
            val latencyMs = System.currentTimeMillis() - startMs

            logger.logEngineEvent("LLM", "response received", latencyMs)

            JulyResult.success(
                LlmResponse(
                    text = response.message.content,
                    tokenCount = response.eval_count,
                    latencyMs = latencyMs,
                    modelName = response.model
                )
            )

        } catch (e: HttpException) {
            val retryable = e.code() in listOf(503, 429, 500)
            JulyResult.failure(
                AppError.Llm(
                    message = "LLM HTTP error ${e.code()}: ${e.message()}",
                    cause = e,
                    retryable = retryable
                )
            )
        } catch (e: IOException) {
            JulyResult.failure(
                AppError.Network(
                    message = "Cannot reach LLM server at ${config.baseUrl}: ${e.message}",
                    cause = e
                )
            )
        } catch (e: Exception) {
            JulyResult.failure(
                AppError.Llm(
                    message = "LLM unexpected error: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        return networkHealthChecker.isReachable(config.host, config.port)
    }

    override suspend fun getModelInfo(): ModelInfo {
        return try {
            ModelInfo(name = config.modelName)
        } catch (e: Exception) {
            ModelInfo(name = config.modelName, version = "unknown")
        }
    }

    private fun buildMessageList(prompt: String, history: List<Message>): List<LlmMessage> {
        val messages = mutableListOf<LlmMessage>()

        // System prompt siempre primero
        messages.add(LlmMessage(role = "system", content = config.systemPrompt))

        // Historial de conversación
        history.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messages.add(LlmMessage(role = role, content = msg.content))
        }

        // Turno actual del usuario
        messages.add(LlmMessage(role = "user", content = prompt))

        return messages
    }
}
```

---

### `ai/tts/PiperConfig.kt`

```kotlin
package com.july.offline.ai.tts

/**
 * Configuración del motor Piper TTS.
 * @param modelPath ruta al archivo .onnx del modelo de voz
 * @param modelConfigPath ruta al archivo .json de configuración del modelo
 * @param sampleRate frecuencia de muestreo de salida (por defecto 22050 Hz)
 * @param speakerId ID del speaker para modelos multi-hablante (0 para modelos mono)
 */
data class PiperConfig(
    val modelPath: String,
    val modelConfigPath: String,
    val sampleRate: Int = 22050,
    val speakerId: Int = 0
)
```

---

### `ai/tts/PiperTTSAdapter.kt`

```kotlin
package com.july.offline.ai.tts

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.port.TextToSpeechEngine
import javax.inject.Inject

/**
 * Adaptador TTS que delega a Piper via JNI.
 *
 * INTEGRACIÓN JNI (FASE 3):
 * - Cargar libpiper.so desde jniLibs/
 * - Llamar a PiperJNI.synthesize(text, modelPath, modelConfigPath, speakerId)
 * - Devuelve PCM 16-bit al sampleRate configurado
 *
 * En FASE 2 devuelve ByteArray vacío para permitir compilación.
 */
class PiperTTSAdapter @Inject constructor(
    private val config: PiperConfig,
    private val logger: DiagnosticsLogger
) : TextToSpeechEngine {

    // TODO FASE 3: companion object { init { System.loadLibrary("piper") } }
    // TODO FASE 3: private val piperContext = PiperJNI.init(config.modelPath, config.modelConfigPath)

    override suspend fun synthesize(text: String): JulyResult<ByteArray> {
        return try {
            logger.logInfo("PiperTTS", "Synthesizing: ${text.take(50)}...")

            // TODO FASE 3: Reemplazar con llamada JNI real:
            // val pcmAudio = PiperJNI.synthesize(piperContext, text, config.speakerId)

            // Placeholder FASE 2 — ByteArray vacío
            JulyResult.success(ByteArray(0))

        } catch (e: Exception) {
            JulyResult.failure(AppError.Tts("Piper synthesis failed: ${e.message}", e))
        }
    }

    override suspend fun isAvailable(): Boolean {
        return java.io.File(config.modelPath).exists() &&
               java.io.File(config.modelConfigPath).exists()
    }

    override fun getSupportedLanguages(): List<String> {
        // TODO FASE 3: leer del modelConfigPath (JSON con campo "language")
        return listOf("es")
    }
}
```

---

## AUDIO — ESQUELETOS

### `audio/recorder/AudioRecorderConfig.kt`

```kotlin
package com.july.offline.audio.recorder

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Configuración del grabador de audio.
 * Valores optimizados para Whisper.cpp (requiere PCM 16-bit mono 16kHz).
 */
data class AudioRecorderConfig(
    val sampleRate: Int = 16_000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val chunkSizeMs: Int = 100  // tamaño de cada chunk emitido por el Flow
)
```

---

### `audio/recorder/AudioRecorderAdapter.kt`

```kotlin
package com.july.offline.audio.recorder

import android.media.AudioRecord
import android.media.AudioFormat
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.AudioCapturePort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementación de AudioCapturePort usando AudioRecord de Android.
 * Emite chunks PCM 16-bit en tiempo real via Flow.
 * Thread-safe: isRecording es @Volatile.
 */
class AudioRecorderAdapter @Inject constructor(
    private val config: AudioRecorderConfig,
    private val logger: DiagnosticsLogger
) : AudioCapturePort {

    @Volatile
    private var _isRecording = false
    override val isRecording: Boolean get() = _isRecording

    private val bufferSize: Int by lazy {
        val minBuffer = AudioRecord.getMinBufferSize(
            config.sampleRate,
            config.channelConfig,
            config.audioEncoding
        )
        // chunk de 100ms
        val chunkBytes = config.sampleRate * 2 * config.chunkSizeMs / 1000
        maxOf(minBuffer, chunkBytes) * 4
    }

    private var audioRecord: AudioRecord? = null
    private val fullAudioBuffer = mutableListOf<Byte>()

    override fun startRecording(): Flow<ByteArray> = flow {
        val recorder = AudioRecord(
            config.audioSource,
            config.sampleRate,
            config.channelConfig,
            config.audioEncoding,
            bufferSize
        )

        audioRecord = recorder
        fullAudioBuffer.clear()
        recorder.startRecording()
        _isRecording = true
        logger.logInfo("AudioRecorder", "Recording started at ${config.sampleRate}Hz")

        val chunkBuffer = ByteArray(bufferSize / 4)

        try {
            while (_isRecording) {
                val bytesRead = recorder.read(chunkBuffer, 0, chunkBuffer.size)
                if (bytesRead > 0) {
                    val chunk = chunkBuffer.copyOf(bytesRead)
                    fullAudioBuffer.addAll(chunk.toList())
                    emit(chunk)
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
            audioRecord = null
            logger.logInfo("AudioRecorder", "Recording stopped. Total: ${fullAudioBuffer.size} bytes")
        }
    }

    override suspend fun stopRecording(): ByteArray {
        _isRecording = false
        return fullAudioBuffer.toByteArray()
    }

    override fun cancel() {
        _isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        fullAudioBuffer.clear()
    }
}
```

---

### `audio/player/AudioPlayerAdapter.kt`

```kotlin
package com.july.offline.audio.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.july.offline.core.logging.DiagnosticsLogger
import javax.inject.Inject

/**
 * Reproduce audio PCM 16-bit mono via AudioTrack de Android.
 * Usado para reproducir la salida de PiperTTSAdapter.
 */
class AudioPlayerAdapter @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    companion object {
        private const val SAMPLE_RATE = 22_050  // debe coincidir con PiperConfig.sampleRate
    }

    @Volatile private var audioTrack: AudioTrack? = null

    /**
     * Reproduce audio PCM 16-bit de forma síncrona.
     * Bloquea hasta completar la reproducción.
     */
    fun play(audio: ByteArray) {
        if (audio.isEmpty()) {
            logger.logWarning("AudioPlayer", "Empty audio, skipping playback")
            return
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, audio.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        track.write(audio, 0, audio.size)
        track.play()
        logger.logInfo("AudioPlayer", "Playing ${audio.size} bytes")

        // Esperar hasta completar
        while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            Thread.sleep(50)
        }

        track.release()
        audioTrack = null
    }

    /** Detiene la reproducción inmediatamente. */
    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
```

---

### `audio/vad/VADConfig.kt`

```kotlin
package com.july.offline.audio.vad

/**
 * Configuración del detector de actividad de voz (VAD).
 * @param silenceThresholdMs milisegundos de silencio continuo para declarar fin de voz
 * @param energyThreshold umbral de energía RMS por debajo del cual se considera silencio
 * @param minAudioDurationMs duración mínima de audio para considerar que hubo voz real
 */
data class VADConfig(
    val silenceThresholdMs: Long = 1_200L,
    val energyThreshold: Double = 150.0,
    val minAudioDurationMs: Long = 300L
)
```

---

### `audio/vad/VADProcessor.kt`

```kotlin
package com.july.offline.audio.vad

import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Detector de actividad de voz por energía RMS.
 * Algoritmo simple y eficiente: calcula la energía del chunk PCM
 * y lo compara con el umbral configurado.
 *
 * En FASE 3 puede reemplazarse por WebRTC VAD via JNI para mayor precisión.
 */
class VADProcessor @Inject constructor(
    private val config: VADConfig
) {

    private var silenceStartMs: Long? = null

    /**
     * Determina si el chunk de audio representa silencio.
     * Gestiona internamente el contador de silencio continuo.
     * @param chunk bytes PCM 16-bit
     * @return true si se ha detectado silencio durante >= silenceThresholdMs
     */
    fun isSilence(chunk: ByteArray): Boolean {
        val energy = calculateRmsEnergy(chunk)
        val now = System.currentTimeMillis()

        return if (energy < config.energyThreshold) {
            val silenceStart = silenceStartMs ?: run {
                silenceStartMs = now
                now
            }
            (now - silenceStart) >= config.silenceThresholdMs
        } else {
            silenceStartMs = null
            false
        }
    }

    /** Resetea el contador de silencio. Llamar al inicio de cada ciclo. */
    fun reset() {
        silenceStartMs = null
    }

    /**
     * Calcula la energía RMS de un chunk PCM 16-bit little-endian.
     * @param pcm bytes en formato PCM 16-bit signed
     * @return valor RMS en el rango [0, 32768]
     */
    private fun calculateRmsEnergy(pcm: ByteArray): Double {
        if (pcm.size < 2) return 0.0

        var sum = 0.0
        var sampleCount = 0

        var i = 0
        while (i + 1 < pcm.size) {
            // Reconstruir sample PCM 16-bit (little-endian)
            val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
            sum += sample.toDouble() * sample.toDouble()
            sampleCount++
            i += 2
        }

        return if (sampleCount > 0) sqrt(sum / sampleCount) else 0.0
    }
}
```

---

## DATA

### `data/db/entity/SessionDbEntity.kt`

```kotlin
package com.july.offline.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionDbEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,        // Instant.toEpochMilli()
    val updatedAt: Long,
    val title: String
)
```

---

### `data/db/entity/MessageDbEntity.kt`

```kotlin
package com.july.offline.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionDbEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageDbEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,       // "USER", "ASSISTANT", "SYSTEM"
    val content: String,
    val timestamp: Long     // Instant.toEpochMilli()
)
```

---

### `data/db/entity/DiagnosticsDbEntity.kt`

```kotlin
package com.july.offline.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostics")
data class DiagnosticsDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String,      // "INFO", "WARNING", "ERROR"
    val tag: String,
    val message: String,
    val stackTrace: String? = null
)
```

---

### `data/db/dao/SessionDao.kt`

```kotlin
package com.july.offline.data.db.dao

import androidx.room.*
import com.july.offline.data.db.entity.SessionDbEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionDbEntity)

    @Update
    suspend fun update(session: SessionDbEntity)

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SessionDbEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SessionDbEntity>>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

---

### `data/db/dao/MessageDao.kt`

```kotlin
package com.july.offline.data.db.dao

import androidx.room.*
import com.july.offline.data.db.entity.MessageDbEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageDbEntity)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: String): List<MessageDbEntity>

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
```

---

### `data/db/dao/DiagnosticsDao.kt`

```kotlin
package com.july.offline.data.db.dao

import androidx.room.*
import com.july.offline.data.db.entity.DiagnosticsDbEntity

@Dao
interface DiagnosticsDao {

    @Insert
    suspend fun insert(entry: DiagnosticsDbEntity)

    @Query("DELETE FROM diagnostics WHERE timestamp < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)

    @Query("SELECT COUNT(*) FROM diagnostics")
    suspend fun count(): Int
}
```

---

### `data/db/JulyDatabase.kt`

```kotlin
package com.july.offline.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.entity.DiagnosticsDbEntity
import com.july.offline.data.db.entity.MessageDbEntity
import com.july.offline.data.db.entity.SessionDbEntity

/**
 * Base de datos Room del sistema.
 * version=1 en FASE 2. Migraciones destructivas activas hasta FASE 3 (datos de prueba).
 * En FASE 3 se añadirán migraciones declarativas antes de llegar a usuarios reales.
 */
@Database(
    entities = [
        SessionDbEntity::class,
        MessageDbEntity::class,
        DiagnosticsDbEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class JulyDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun diagnosticsDao(): DiagnosticsDao
}
```

---

### `data/datastore/AppPreferencesDataStore.kt`

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

/**
 * DataStore de preferencias del usuario.
 * Expone Flow reactivos para que los ViewModels se suscriban.
 */
@Singleton
class AppPreferencesDataStore @Inject constructor(
    private val context: Context
) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val SHOW_TRANSCRIPT = booleanPreferencesKey("show_transcript")
    }

    val language: Flow<String> = context.appPrefsDataStore.data
        .map { it[Keys.LANGUAGE] ?: "es" }

    val ttsEnabled: Flow<Boolean> = context.appPrefsDataStore.data
        .map { it[Keys.TTS_ENABLED] ?: true }

    val showTranscript: Flow<Boolean> = context.appPrefsDataStore.data
        .map { it[Keys.SHOW_TRANSCRIPT] ?: true }

    suspend fun setLanguage(lang: String) {
        context.appPrefsDataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.TTS_ENABLED] = enabled }
    }

    suspend fun setShowTranscript(show: Boolean) {
        context.appPrefsDataStore.edit { it[Keys.SHOW_TRANSCRIPT] = show }
    }
}
```

---

### `data/datastore/SystemConfigDataStore.kt`

```kotlin
package com.july.offline.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sysConfigDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "system_config")

/**
 * DataStore de configuración de motores del sistema.
 * Leído por Hilt al construir los adaptadores de motores.
 */
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
    }

    val llmHost: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.LLM_HOST] ?: "127.0.0.1" }
    val llmPort: Flow<Int> = context.sysConfigDataStore.data.map { it[Keys.LLM_PORT] ?: 11434 }
    val llmModel: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.LLM_MODEL] ?: "llama3.2:3b" }
    val sttModelPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.STT_MODEL_PATH] ?: "" }
    val ttsModelPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.TTS_MODEL_PATH] ?: "" }
    val ttsConfigPath: Flow<String> = context.sysConfigDataStore.data.map { it[Keys.TTS_CONFIG_PATH] ?: "" }

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
}
```

---

### `data/network/LocalNetworkClient.kt`

```kotlin
package com.july.offline.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Fábrica del OkHttpClient configurado para el servidor LLM local.
 * Instanciada como singleton en NetworkModule.
 */
object LocalNetworkClient {

    fun create(
        connectTimeoutSeconds: Long = 5L,
        readTimeoutSeconds: Long = 60L,
        enableLogging: Boolean = true
    ): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (enableLogging) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }
}
```

---

### `data/network/NetworkHealthChecker.kt`

```kotlin
package com.july.offline.data.network

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verificador de conectividad TCP al servidor LLM local.
 * Usado por LocalServerLLMAdapter.isAvailable() y EngineHealthMonitor.
 */
@Singleton
class NetworkHealthChecker @Inject constructor(
    private val logger: DiagnosticsLogger
) {

    companion object {
        private const val TCP_TIMEOUT_MS = 3_000
    }

    /**
     * Intenta conexión TCP al host:port.
     * @return true si el puerto está abierto y acepta conexiones
     */
    suspend fun isReachable(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TCP_TIMEOUT_MS)
                true
            }
        } catch (e: Exception) {
            logger.logDebug("NetworkHealthChecker", "Unreachable $host:$port — ${e.message}")
            false
        }
    }
}
```

---

### `data/repository/SessionRepositoryImpl.kt`

```kotlin
package com.july.offline.data.repository

import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.entity.MessageDbEntity
import com.july.offline.data.db.entity.SessionDbEntity
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.SessionEntity
import com.july.offline.domain.port.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Implementación de SessionRepository.
 * Traduce entre modelos de dominio (SessionEntity, Message) y entidades Room.
 */
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao
) : SessionRepository {

    override suspend fun createSession(): SessionEntity {
        val now = Instant.now()
        val entity = SessionDbEntity(
            id = UUID.randomUUID().toString(),
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            title = ""
        )
        sessionDao.insert(entity)
        return entity.toDomain(emptyList())
    }

    override suspend fun addMessage(sessionId: String, message: Message) {
        messageDao.insert(message.toDbEntity(sessionId))
        // Actualizar updatedAt de la sesión
        sessionDao.getById(sessionId)?.let { session ->
            sessionDao.update(session.copy(updatedAt = Instant.now().toEpochMilli()))
        }
    }

    override suspend fun getSession(sessionId: String): SessionEntity? {
        val session = sessionDao.getById(sessionId) ?: return null
        val messages = messageDao.getBySession(sessionId).map { it.toDomain() }
        return session.toDomain(messages)
    }

    override fun getRecentSessions(limit: Int): Flow<List<SessionEntity>> {
        return sessionDao.getRecent(limit).map { sessions ->
            sessions.map { it.toDomain(emptyList()) }
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteById(sessionId)
    }

    // ── Mappers ────────────────────────────────────────────────────────

    private fun SessionDbEntity.toDomain(messages: List<Message>) = SessionEntity(
        id = id,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        messages = messages,
        title = title
    )

    private fun MessageDbEntity.toDomain() = Message(
        id = id,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = Instant.ofEpochMilli(timestamp)
    )

    private fun Message.toDbEntity(sessionId: String) = MessageDbEntity(
        id = id,
        sessionId = sessionId,
        role = role.name,
        content = content,
        timestamp = timestamp.toEpochMilli()
    )
}
```

---

## DI

### `di/CoroutineModule.kt`

```kotlin
package com.july.offline.di

import com.july.offline.core.coroutines.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = CoroutineDispatchers()
}
```

---

### `di/DatabaseModule.kt`

```kotlin
package com.july.offline.di

import android.content.Context
import androidx.room.Room
import com.july.offline.data.db.JulyDatabase
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJulyDatabase(@ApplicationContext context: Context): JulyDatabase =
        Room.databaseBuilder(
            context,
            JulyDatabase::class.java,
            "july_offline.db"
        )
        .fallbackToDestructiveMigration() // FASE 2: datos de prueba, ok borrar
        .build()

    @Provides
    fun provideSessionDao(db: JulyDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMessageDao(db: JulyDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideDiagnosticsDao(db: JulyDatabase): DiagnosticsDao = db.diagnosticsDao()
}
```

---

### `di/NetworkModule.kt`

```kotlin
package com.july.offline.di

import com.july.offline.ai.llm.LlmApiService
import com.july.offline.ai.llm.LlmServerConfig
import com.july.offline.data.network.LocalNetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLlmServerConfig(): LlmServerConfig = LlmServerConfig()

    @Provides
    @Singleton
    fun provideRetrofit(config: LlmServerConfig): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(
                LocalNetworkClient.create(
                    connectTimeoutSeconds = config.connectTimeoutSeconds,
                    readTimeoutSeconds = config.readTimeoutSeconds
                )
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideLlmApiService(retrofit: Retrofit): LlmApiService =
        retrofit.create(LlmApiService::class.java)
}
```

---

### `di/DataStoreModule.kt`

```kotlin
package com.july.offline.di

import android.content.Context
import com.july.offline.data.datastore.AppPreferencesDataStore
import com.july.offline.data.datastore.SystemConfigDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideAppPreferencesDataStore(
        @ApplicationContext context: Context
    ): AppPreferencesDataStore = AppPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideSystemConfigDataStore(
        @ApplicationContext context: Context
    ): SystemConfigDataStore = SystemConfigDataStore(context)
}
```

---

### `di/AudioModule.kt`

```kotlin
package com.july.offline.di

import com.july.offline.audio.player.AudioPlayerAdapter
import com.july.offline.audio.recorder.AudioRecorderAdapter
import com.july.offline.audio.recorder.AudioRecorderConfig
import com.july.offline.audio.vad.VADConfig
import com.july.offline.audio.vad.VADProcessor
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
        fun provideAudioPlayerAdapter(
            // Necesita Logger — Hilt lo inyecta automáticamente
        ): AudioPlayerAdapter = AudioPlayerAdapter(
            com.july.offline.core.logging.DiagnosticsLogger()
        )
    }
}
```

---

### `di/EngineModule.kt`

```kotlin
package com.july.offline.di

import com.july.offline.ai.llm.LocalServerLLMAdapter
import com.july.offline.ai.stt.WhisperConfig
import com.july.offline.ai.stt.WhisperSTTAdapter
import com.july.offline.ai.tts.PiperConfig
import com.july.offline.ai.tts.PiperTTSAdapter
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    /** Vincula la implementación STT concreta a la interface del dominio. */
    @Binds
    @Singleton
    abstract fun bindSpeechToTextEngine(impl: WhisperSTTAdapter): SpeechToTextEngine

    /** Vincula la implementación LLM concreta a la interface del dominio. */
    @Binds
    @Singleton
    abstract fun bindLanguageModelEngine(impl: LocalServerLLMAdapter): LanguageModelEngine

    /** Vincula la implementación TTS concreta a la interface del dominio. */
    @Binds
    @Singleton
    abstract fun bindTextToSpeechEngine(impl: PiperTTSAdapter): TextToSpeechEngine

    companion object {

        /**
         * Configuración de Whisper.
         * NOTA: modelPath debe apuntar a un archivo .bin válido en el dispositivo.
         * En FASE 2 la ruta puede ser vacía (isAvailable() devolverá false).
         */
        @Provides
        @Singleton
        fun provideWhisperConfig(): WhisperConfig = WhisperConfig(
            modelPath = "/data/data/com.july.offline/files/whisper-tiny.bin",
            language = "es",
            threads = 4
        )

        /**
         * Configuración de Piper.
         * NOTA: rutas de modelo deben existir en el dispositivo.
         */
        @Provides
        @Singleton
        fun providePiperConfig(): PiperConfig = PiperConfig(
            modelPath = "/data/data/com.july.offline/files/es_ES-carlfm-x_low.onnx",
            modelConfigPath = "/data/data/com.july.offline/files/es_ES-carlfm-x_low.onnx.json"
        )
    }
}
```

---

## SETTINGS

### `settings/AppSettings.kt`

```kotlin
package com.july.offline.settings

/**
 * Configuración del usuario leída por ViewModels desde AppPreferencesDataStore.
 * Modelo de presentación de preferencias. Inmutable.
 */
data class AppSettings(
    val language: String = "es",
    val ttsEnabled: Boolean = true,
    val showTranscript: Boolean = true
)
```

---

## NAVIGATION

### `navigation/JulyDestination.kt`

```kotlin
package com.july.offline.navigation

/** Destinos de navegación de la app. */
sealed class JulyDestination(val route: String) {
    object Conversation : JulyDestination("conversation")
    object Settings : JulyDestination("settings")
}
```

---

### `navigation/JulyNavGraph.kt`

```kotlin
package com.july.offline.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.july.offline.ui.conversation.ConversationScreen
import com.july.offline.ui.settings.SettingsScreen

@Composable
fun JulyNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = JulyDestination.Conversation.route
    ) {
        composable(JulyDestination.Conversation.route) {
            ConversationScreen(
                onNavigateToSettings = {
                    navController.navigate(JulyDestination.Settings.route)
                }
            )
        }
        composable(JulyDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

## UI

### `ui/conversation/ConversationUiState.kt`

```kotlin
package com.july.offline.ui.conversation

import com.july.offline.domain.model.EngineHealthState
import com.july.offline.domain.model.EngineStatus

/**
 * Modelos de presentación para ConversationScreen.
 * Sin lógica de negocio. Solo datos listos para renderizar.
 */

/** Estado visual de la pantalla de conversación. */
data class ConversationUiState(
    val phase: ConversationPhase = ConversationPhase.IDLE,
    val displayedText: String = "",
    val transcriptText: String = "",
    val errorMessage: String? = null,
    val engineHealth: EngineHealthUiState = EngineHealthUiState(),
    val isCancelVisible: Boolean = false,
    val isMicButtonEnabled: Boolean = true
)

/** Fase visual (mapeada 1:1 desde ConversationState para evitar lógica en UI). */
enum class ConversationPhase {
    IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, ERROR, CANCELLED
}

/** Estado visual de salud de motores. */
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

/** Burbuja de mensaje para el historial visual. */
data class MessageUiModel(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)
```

---

### `ui/conversation/ConversationViewModel.kt`

```kotlin
package com.july.offline.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.core.error.AppError
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.orchestrator.ConversationOrchestrator
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.domain.state.ConversationStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel de la pantalla de conversación.
 *
 * Responsabilidades:
 * - Combinar ConversationState + EngineHealthState en un único ConversationUiState
 * - Reenviar eventos de usuario al orquestador
 * - No contiene lógica de negocio
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val orchestrator: ConversationOrchestrator,
    private val stateHolder: ConversationStateHolder,
    private val healthMonitor: EngineHealthMonitor
) : ViewModel() {

    val uiState: StateFlow<ConversationUiState> = combine(
        stateHolder.conversationState,
        healthMonitor.healthState
    ) { conversationState, healthState ->
        mapToUiState(conversationState, healthState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationUiState()
    )

    /** El usuario presionó el botón del micrófono. */
    fun onMicPressed() {
        orchestrator.startConversationCycle()
    }

    /** El usuario presionó cancelar. */
    fun onCancelPressed() {
        orchestrator.cancelCurrentCycle()
    }

    private fun mapToUiState(
        state: ConversationState,
        health: com.july.offline.domain.model.EngineHealthState
    ): ConversationUiState {
        val engineHealthUi = EngineHealthUiState.from(health)

        return when (state) {
            is ConversationState.Idle -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                isMicButtonEnabled = engineHealthUi.llmReady
            )
            is ConversationState.Listening -> ConversationUiState(
                phase = ConversationPhase.LISTENING,
                displayedText = "Escuchando...",
                engineHealth = engineHealthUi,
                isCancelVisible = true
            )
            is ConversationState.Transcribing -> ConversationUiState(
                phase = ConversationPhase.TRANSCRIBING,
                displayedText = "Procesando audio...",
                engineHealth = engineHealthUi,
                isCancelVisible = true
            )
            is ConversationState.Thinking -> ConversationUiState(
                phase = ConversationPhase.THINKING,
                displayedText = "Pensando...",
                transcriptText = state.transcript.text,
                engineHealth = engineHealthUi,
                isCancelVisible = true
            )
            is ConversationState.Speaking -> ConversationUiState(
                phase = ConversationPhase.SPEAKING,
                displayedText = state.response.text,
                engineHealth = engineHealthUi,
                isCancelVisible = false
            )
            is ConversationState.Error -> ConversationUiState(
                phase = ConversationPhase.ERROR,
                errorMessage = mapErrorToUserMessage(state.error),
                engineHealth = engineHealthUi,
                isMicButtonEnabled = state.error !is AppError.Permission
            )
            is ConversationState.Cancelled -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi
            )
        }
    }

    private fun mapErrorToUserMessage(error: AppError): String = when (error) {
        is AppError.Permission -> "Se necesita permiso de micrófono"
        is AppError.Stt -> "No pude entenderte. Intenta de nuevo."
        is AppError.Llm -> "El asistente no está disponible en este momento."
        is AppError.Tts -> "No pude reproducir la respuesta."
        is AppError.Network -> "El servidor de IA no está disponible. Verifica que esté activo."
        is AppError.Cancelled -> ""
        is AppError.Unknown -> "Ocurrió un error inesperado."
    }
}
```

---

### `ui/conversation/ConversationScreen.kt`

```kotlin
package com.july.offline.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.july.offline.ui.conversation.components.EngineHealthWidget
import com.july.offline.ui.conversation.components.StatusBar
import com.july.offline.ui.conversation.components.WaveformIndicator

/**
 * Pantalla principal de conversación.
 * Sin lógica de negocio. Solo renderiza UiState y delega eventos al ViewModel.
 */
@Composable
fun ConversationScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("July") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Text("⚙")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Estado de salud de motores
            EngineHealthWidget(healthState = uiState.engineHealth)

            Spacer(modifier = Modifier.height(24.dp))

            // Zona central: texto + waveform
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                StatusBar(phase = uiState.phase)

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.phase == ConversationPhase.LISTENING) {
                    WaveformIndicator()
                }

                if (uiState.displayedText.isNotEmpty()) {
                    Text(
                        text = uiState.displayedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Controles inferiores
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isCancelVisible) {
                    OutlinedButton(
                        onClick = { viewModel.onCancelPressed() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Cancelar")
                    }
                }

                Button(
                    onClick = { viewModel.onMicPressed() },
                    enabled = uiState.isMicButtonEnabled &&
                              uiState.phase == ConversationPhase.IDLE
                ) {
                    Text(
                        text = when (uiState.phase) {
                            ConversationPhase.IDLE -> "Hablar"
                            ConversationPhase.LISTENING -> "Escuchando..."
                            ConversationPhase.TRANSCRIBING -> "Procesando..."
                            ConversationPhase.THINKING -> "Pensando..."
                            ConversationPhase.SPEAKING -> "Respondiendo..."
                            ConversationPhase.ERROR -> "Reintentar"
                            ConversationPhase.CANCELLED -> "Hablar"
                        }
                    )
                }
            }
        }
    }
}
```

---

### `ui/conversation/components/WaveformIndicator.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Indicador visual animado de grabación activa. Barras que oscilan en altura. */
@Composable
fun WaveformIndicator(modifier: Modifier = Modifier) {
    val barCount = 5
    Row(
        modifier = modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "bar$index")
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + index * 80,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "height$index"
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(height.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
```

---

### `ui/conversation/components/StatusBar.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.july.offline.ui.conversation.ConversationPhase

/** Texto de estado del sistema mapeado desde ConversationPhase. */
@Composable
fun StatusBar(phase: ConversationPhase) {
    val label = when (phase) {
        ConversationPhase.IDLE -> "Listo"
        ConversationPhase.LISTENING -> "Escuchando"
        ConversationPhase.TRANSCRIBING -> "Transcribiendo"
        ConversationPhase.THINKING -> "Procesando"
        ConversationPhase.SPEAKING -> "Respondiendo"
        ConversationPhase.ERROR -> "Error"
        ConversationPhase.CANCELLED -> "Cancelado"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = when (phase) {
            ConversationPhase.ERROR -> MaterialTheme.colorScheme.error
            ConversationPhase.LISTENING -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}
```

---

### `ui/conversation/components/MessageBubble.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.july.offline.ui.conversation.MessageUiModel

/** Burbuja de mensaje individual para el historial de conversación. */
@Composable
fun MessageBubble(message: MessageUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

---

### `ui/conversation/components/EngineHealthWidget.kt`

```kotlin
package com.july.offline.ui.conversation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.july.offline.ui.conversation.EngineHealthUiState

/**
 * Widget compacto de salud de motores.
 * Muestra STT / LLM / TTS con indicador de disponibilidad.
 */
@Composable
fun EngineHealthWidget(
    healthState: EngineHealthUiState,
    modifier: Modifier = Modifier
) {
    if (!healthState.showWarning) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Motores:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EngineIndicator("STT", healthState.sttReady)
        EngineIndicator("LLM", healthState.llmReady)
        EngineIndicator("TTS", healthState.ttsReady)
    }
}

@Composable
private fun EngineIndicator(name: String, isReady: Boolean) {
    Text(
        text = "$name ${if (isReady) "✓" else "✗"}",
        style = MaterialTheme.typography.labelSmall,
        color = if (isReady)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.error
    )
}
```

---

### `ui/settings/SettingsViewModel.kt`

```kotlin
package com.july.offline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.data.datastore.AppPreferencesDataStore
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
    private val preferencesDataStore: AppPreferencesDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = combine(
        preferencesDataStore.language,
        preferencesDataStore.ttsEnabled,
        preferencesDataStore.showTranscript
    ) { language, ttsEnabled, showTranscript ->
        AppSettings(
            language = language,
            ttsEnabled = ttsEnabled,
            showTranscript = showTranscript
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
}
```

---

### `ui/settings/SettingsScreen.kt`

```kotlin
package com.july.offline.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

            Divider()
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

## APP

### `JulyApplication.kt`

```kotlin
package com.july.offline

import android.app.Application
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class del sistema.
 * Inicia el monitoreo de salud de motores al arrancar.
 */
@HiltAndroidApp
class JulyApplication : Application() {

    @Inject
    lateinit var engineHealthMonitor: EngineHealthMonitor

    override fun onCreate() {
        super.onCreate()
        engineHealthMonitor.startMonitoring()
    }
}
```

---

### `MainActivity.kt`

```kotlin
package com.july.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.july.offline.navigation.JulyNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            JulyNavGraph(navController = navController)
        }
    }
}
```

---

### `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permisos requeridos -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <!-- Solo red local — no se usa internet externo -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Hardware requerido -->
    <uses-feature
        android:name="android.hardware.microphone"
        android:required="true" />

    <application
        android:name=".JulyApplication"
        android:allowBackup="false"
        android:label="July"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

---

## NOTAS DE IMPLEMENTACIÓN

### TODOs pendientes para FASE 3

1. **WhisperSTTAdapter.kt** — Integrar llamada JNI real a `libwhisper.so`
2. **PiperTTSAdapter.kt** — Integrar llamada JNI real a `libpiper.so`
3. **AudioCoordinator.kt** — El Flow de `startRecording()` no termina solo con VAD actual; implementar lógica de corte correcta con `takeWhile` sobre el flow de chunks
4. **AudioModule.kt** — Limpiar la instanciación manual de `AudioPlayerAdapter`; usar `@Inject` correctamente
5. **DiagnosticsLogger.kt** — Conectar con `DiagnosticsDao` para persistencia real de logs
6. **JulyDatabase.kt** — Cambiar `fallbackToDestructiveMigration()` por migraciones declarativas antes de release
7. **NetworkModule.kt** — Leer `LlmServerConfig` desde `SystemConfigDataStore` en lugar de valores hardcoded
8. **EngineModule.kt** — Leer rutas de modelo desde `SystemConfigDataStore` en lugar de strings hardcoded
9. **ConversationScreen.kt** — Implementar lista scrollable de mensajes del historial
10. **Permisos de micrófono** — Implementar solicitud de permiso en `ConversationScreen` antes del primer ciclo

### Estructura de archivos de modelo (para pruebas)
```
/data/data/com.july.offline/files/
├── whisper-tiny.bin          # ~75MB — descargar desde Hugging Face
├── es_ES-carlfm-x_low.onnx  # ~63MB — Piper voice model
└── es_ES-carlfm-x_low.onnx.json
```
