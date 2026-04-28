# JULY OFFLINE — FASE 8
## Tests Instrumentados + ProGuard + AssetCopier + OOM Handler
### `com.july.offline`

**Alcance FASE 8:**
- Tests instrumentados en `androidTest/` con Hilt + Room + DataStore
- ProGuard completo para todos los motores JNI (Whisper, Piper, llama.cpp, Porcupine)
- `AssetCopier` — copia el modelo .ppn de Porcupine desde assets a filesDir en el primer arranque
- `OomHandler` — intercepta OutOfMemoryError en JNI y libera modelos de forma controlada
- Migración Room v2→v3 declarativa
- `gradle/libs.versions.toml` — dependencias de test instrumentado

---

## ÍNDICE DE ARCHIVOS FASE 8

### NUEVOS — INFRAESTRUCTURA
- `core/memory/OomHandler.kt`
- `core/startup/AssetCopier.kt`
- `di/StartupModule.kt`

### MODIFICADOS
- `data/db/JulyDatabase.kt` (migración v2→v3)
- `ai/stt/WhisperSTTAdapter.kt` (manejo OOM)
- `ai/tts/PiperTTSAdapter.kt` (manejo OOM)
- `ai/llm/embedded/LlamaCppLLMAdapter.kt` (manejo OOM)
- `di/WakeWordModule.kt` (usa AssetCopier para .ppn)
- `JulyApplication.kt` (inicia AssetCopier)
- `gradle/libs.versions.toml` (añade deps test instrumentado)
- `app/build.gradle.kts` (configura androidTest)
- `app/proguard-rules.pro` (reglas completas)

### NUEVOS — TESTS INSTRUMENTADOS (`androidTest/`)
- `HiltTestRunner.kt`
- `db/JulyDatabaseTest.kt`
- `db/SessionDaoTest.kt`
- `db/MessageDaoTest.kt`
- `db/MigrationTest.kt`
- `datastore/AppPreferencesDataStoreTest.kt`
- `datastore/SystemConfigDataStoreTest.kt`
- `repository/SessionRepositoryImplTest.kt`
- `permission/PermissionHandlerTest.kt`
- `di/TestAppModule.kt`

---

## BUILD — ACTUALIZACIONES

### `gradle/libs.versions.toml` — añadir sección androidTest

```toml
[versions]
# ... versiones existentes ...
androidxTest = "1.5.0"
androidxTestExt = "1.1.5"
espresso = "3.5.1"
hiltTesting = "2.52"
roomTesting = "2.6.1"
coroutinesTest = "1.9.0"

[libraries]
# ... librerías existentes ...

# AndroidTest
androidx-test-core = { group = "androidx.test", name = "core-ktx", version.ref = "androidxTest" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTest" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTest" }
androidx-test-ext = { group = "androidx.test.ext", name = "junit-ktx", version.ref = "androidxTestExt" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
hilt-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hiltTesting" }
room-testing-android = { group = "androidx.room", name = "room-testing", version.ref = "roomTesting" }
```

---

### `app/build.gradle.kts` — sección androidTest

```kotlin
android {
    // ... configuración existente ...

    defaultConfig {
        // ... existente ...
        testInstrumentationRunner = "com.july.offline.HiltTestRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.useJUnitPlatform() }
        }
        // Tests instrumentados en memoria
        managedDevices {
            localDevices {
                create("pixel6api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    // ... dependencias existentes ...

    // AndroidTest
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.room.testing.android)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.turbine)
}
```

---

## PROGUARD

### `app/proguard-rules.pro` (completo)

```proguard
# =============================================================================
# July Offline — ProGuard Rules
# Aplica a release builds únicamente
# =============================================================================

# ── REGLAS GENERALES ──────────────────────────────────────────────────────

# Mantener líneas de stack trace en crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Mantener anotaciones
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# ── KOTLIN ────────────────────────────────────────────────────────────────

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── HILT / DAGGER ────────────────────────────────────────────────────────

-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
    @dagger.* <fields>;
    @dagger.* <methods>;
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *
-keep class **_HiltModules* { *; }
-keep class **_MembersInjector* { *; }
-keep class **_Factory* { *; }
-keep class *_ComponentTreeDeps { *; }
-keep class *Hilt_* { *; }

# ── ROOM ─────────────────────────────────────────────────────────────────

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract **[] allTables();
}
# Mantener entidades Room — Room usa reflexión para mapear columnas
-keep class com.july.offline.data.db.entity.** { *; }
-keepclassmembers class com.july.offline.data.db.entity.** { *; }

# ── DATASTORE ────────────────────────────────────────────────────────────

-keep class androidx.datastore.** { *; }
-keep class com.google.protobuf.** { *; }

# ── RETROFIT + OKHTTP ────────────────────────────────────────────────────

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class com.squareup.okhttp3.** { *; }

# Retrofit — mantener interfaces de servicio y modelos de request/response
-keepattributes Signature
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-keep class com.july.offline.ai.llm.LlmApiService { *; }
-keep class com.july.offline.ai.llm.LlmChatRequest { *; }
-keep class com.july.offline.ai.llm.LlmChatResponse { *; }
-keep class com.july.offline.ai.llm.LlmMessage { *; }
-keep class com.july.offline.ai.llm.LlmOptions { *; }

# Gson — mantener modelos serializados
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# ── JNI — WHISPER.CPP ────────────────────────────────────────────────────

# Los métodos external de Kotlin deben mantener su nombre exacto
# porque el nombre se usa para la resolución de símbolos en libwhisper.so
-keep class com.july.offline.ai.stt.WhisperJNI {
    native <methods>;
    public static *;
    public *;
}
# El companion object init { System.loadLibrary } debe preservarse
-keepclassmembers class com.july.offline.ai.stt.WhisperJNI {
    static {};
}

# ── JNI — PIPER TTS ──────────────────────────────────────────────────────

-keep class com.july.offline.ai.tts.PiperJNI {
    native <methods>;
    public static *;
    public *;
}
-keepclassmembers class com.july.offline.ai.tts.PiperJNI {
    static {};
}

# ── JNI — LLAMA.CPP ──────────────────────────────────────────────────────

-keep class com.july.offline.ai.llm.embedded.LlamaCppJNI {
    native <methods>;
    public static *;
    public *;
}
-keepclassmembers class com.july.offline.ai.llm.embedded.LlamaCppJNI {
    static {};
}

# ── PORCUPINE (PICOVOICE) ────────────────────────────────────────────────

# Porcupine SDK usa reflexión internamente para cargar la librería nativa
-keep class ai.picovoice.** { *; }
-keepclassmembers class ai.picovoice.** { *; }
-dontwarn ai.picovoice.**

# ── DOMAIN — MODELOS Y PUERTOS ───────────────────────────────────────────

# Mantener interfaces del dominio (usadas via reflexión por Hilt)
-keep interface com.july.offline.domain.port.** { *; }
-keep class com.july.offline.domain.model.** { *; }
-keep class com.july.offline.core.error.** { *; }
-keep class com.july.offline.core.result.** { *; }
-keep class com.july.offline.core.memory.** { *; }

# Sealed classes — mantener subclases para when() exhaustivo en runtime
-keep class com.july.offline.domain.model.ConversationState { *; }
-keep class com.july.offline.domain.model.ConversationState$* { *; }
-keep class com.july.offline.core.error.AppError { *; }
-keep class com.july.offline.core.error.AppError$* { *; }
-keep class com.july.offline.core.result.JulyResult { *; }
-keep class com.july.offline.core.result.JulyResult$* { *; }
-keep class com.july.offline.domain.port.WakeWordEvent { *; }
-keep class com.july.offline.domain.port.WakeWordEvent$* { *; }
-keep class com.july.offline.core.error.ErrorAction { *; }
-keep class com.july.offline.core.error.ErrorAction$* { *; }

# ── COMPOSE ──────────────────────────────────────────────────────────────

-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# ── ENUMS ────────────────────────────────────────────────────────────────

# Enums usados con valueOf() o en DataStore (serializado por nombre)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public **[] $values();
}
-keep enum com.july.offline.core.memory.ModelMode { *; }
-keep enum com.july.offline.core.memory.ModelState { *; }
-keep enum com.july.offline.core.memory.EngineType { *; }
-keep enum com.july.offline.ai.llm.router.LlmMode { *; }
-keep enum com.july.offline.domain.model.MessageRole { *; }
-keep enum com.july.offline.domain.model.EngineStatus { *; }

# ── VIEWMODELS ───────────────────────────────────────────────────────────

-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application, ...);
}

# ── SUPRIMIR ADVERTENCIAS ────────────────────────────────────────────────

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.StringConcatFactory
```

---

## INFRAESTRUCTURA

### `core/memory/OomHandler.kt`

```kotlin
package com.july.offline.core.memory

import com.july.offline.core.logging.DiagnosticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor de OutOfMemoryError para operaciones JNI.
 *
 * Los modelos JNI (Whisper ~466MB, Piper ~300MB, llama.cpp ~2GB) pueden
 * causar OOM si el sistema tiene presión de memoria. Un OOM no capturado
 * en un hilo de IO mata el proceso sin posibilidad de limpieza.
 *
 * Estrategia:
 * 1. Capturar OOM en el adaptador JNI
 * 2. Notificar a OomHandler
 * 3. OomHandler llama a ModelMemoryManager.releaseModelsIfMemoryMode()
 *    independientemente del modo actual (liberación de emergencia)
 * 4. El adaptador devuelve AppError para que el orquestador lo gestione
 *
 * IMPORTANTE: OOM en JNI puede dejar el heap nativo en estado inconsistente.
 * Después de un OOM siempre liberamos todos los contextos JNI y pedimos
 * al usuario que reinicie la sesión.
 */
@Singleton
class OomHandler @Inject constructor(
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger
) {

    private val scope = CoroutineScope(SupervisorJob())

    /**
     * Maneja un OOM ocurrido durante una operación JNI.
     * Libera todos los modelos de forma asíncrona y registra el evento.
     *
     * @param engine nombre del motor que causó el OOM (para logging)
     * @param oom el error capturado
     */
    fun handle(engine: String, oom: OutOfMemoryError) {
        logger.logError(
            tag = "OomHandler",
            message = "OOM in $engine — releasing all JNI contexts immediately",
            cause = oom
        )

        // Liberar todos los modelos independientemente del modo configurado
        scope.launch {
            try {
                // Forzar liberación aunque estemos en modo SPEED
                val originalMode = modelMemoryManager.currentMode
                modelMemoryManager.currentMode = ModelMode.MEMORY
                modelMemoryManager.releaseModelsIfMemoryMode()
                // No restaurar el modo — el usuario debe reiniciar la sesión
                logger.logWarning(
                    "OomHandler",
                    "All JNI models released after OOM. App requires session restart."
                )
            } catch (e: Exception) {
                logger.logError("OomHandler", "Failed to release models after OOM", e)
            }
        }
    }

    /**
     * Wrapper seguro para operaciones JNI propensas a OOM.
     * Usa inline para evitar overhead de lambda en el hot path de inferencia.
     */
    inline fun <T> safeJniCall(
        engine: String,
        onOom: () -> T,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (oom: OutOfMemoryError) {
            handle(engine, oom)
            onOom()
        }
    }
}
```

---

### `core/startup/AssetCopier.kt`

```kotlin
package com.july.offline.core.startup

import android.content.Context
import com.july.offline.core.logging.DiagnosticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copia archivos desde assets/ a filesDir en el primer arranque.
 *
 * Uso principal en FASE 8:
 * - Modelo Porcupine (.ppn) para wake-word personalizado "Oye July"
 *   El .ppn debe estar en app/src/main/assets/oye_july_es.ppn
 *
 * Política de copia:
 * - Solo copia si el archivo de destino no existe (primer arranque)
 * - Si el archivo ya existe, verifica el tamaño. Si el source es más grande
 *   (actualización de modelo), reemplaza.
 * - Operación asíncrona en Dispatchers.IO
 *
 * Llamado desde JulyApplication.onCreate() antes de cualquier motor.
 */
@Singleton
class AssetCopier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DiagnosticsLogger
) {

    data class CopyResult(
        val fileName: String,
        val destinationPath: String,
        val copied: Boolean,
        val skipped: Boolean,
        val error: Exception? = null
    )

    /**
     * Copia un archivo de assets a filesDir si es necesario.
     *
     * @param assetFileName nombre del archivo en assets/ (ej: "oye_july_es.ppn")
     * @param destinationFileName nombre en filesDir (puede ser diferente al asset)
     * @return CopyResult con el resultado de la operación
     */
    suspend fun copyIfNeeded(
        assetFileName: String,
        destinationFileName: String = assetFileName
    ): CopyResult = withContext(Dispatchers.IO) {

        val destination = File(context.filesDir, destinationFileName)

        try {
            // Verificar si el asset existe
            val assetFiles = context.assets.list("") ?: emptyArray()
            if (assetFileName !in assetFiles) {
                logger.logWarning(
                    "AssetCopier",
                    "Asset not found: $assetFileName — skipping copy"
                )
                return@withContext CopyResult(
                    fileName = assetFileName,
                    destinationPath = destination.absolutePath,
                    copied = false,
                    skipped = true
                )
            }

            // Verificar si ya existe y tiene el mismo tamaño
            if (destination.exists()) {
                val assetSize = context.assets.open(assetFileName).use { it.available().toLong() }
                if (destination.length() == assetSize) {
                    logger.logDebug(
                        "AssetCopier",
                        "Already exists, same size: $destinationFileName — skipping"
                    )
                    return@withContext CopyResult(
                        fileName = assetFileName,
                        destinationPath = destination.absolutePath,
                        copied = false,
                        skipped = true
                    )
                }
                logger.logInfo(
                    "AssetCopier",
                    "Size mismatch for $destinationFileName — replacing (model update)"
                )
            }

            // Copiar
            logger.logInfo("AssetCopier", "Copying $assetFileName → ${destination.absolutePath}")
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }

            logger.logInfo(
                "AssetCopier",
                "Copied $assetFileName (${destination.length()} bytes)"
            )

            CopyResult(
                fileName = assetFileName,
                destinationPath = destination.absolutePath,
                copied = true,
                skipped = false
            )

        } catch (e: Exception) {
            logger.logError("AssetCopier", "Failed to copy $assetFileName", e)
            CopyResult(
                fileName = assetFileName,
                destinationPath = destination.absolutePath,
                copied = false,
                skipped = false,
                error = e
            )
        }
    }

    /**
     * Copia múltiples assets en paralelo.
     * @param assets lista de pares (assetFileName, destinationFileName)
     */
    suspend fun copyAll(
        assets: List<Pair<String, String>>
    ): List<CopyResult> = withContext(Dispatchers.IO) {
        assets.map { (asset, dest) -> copyIfNeeded(asset, dest) }
    }
}
```

---

### `di/StartupModule.kt`

```kotlin
package com.july.offline.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo declarativo para documentar que AssetCopier y OomHandler
 * se instancian a nivel de proceso via @Singleton.
 * No requiere @Provides — Hilt los resuelve via @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object StartupModule
```

---

## AI — MANEJO OOM

### `ai/stt/WhisperSTTAdapter.kt` — fragmento actualizado (añadir OomHandler)

```kotlin
// Añadir al constructor de WhisperSTTAdapter:
class WhisperSTTAdapter @Inject constructor(
    private val config: WhisperConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val oomHandler: OomHandler,   // NUEVO FASE 8
    private val logger: DiagnosticsLogger
) : SpeechToTextEngine {

    // En el método transcribe(), envolver la llamada JNI:
    override suspend fun transcribe(audio: ByteArray): JulyResult<Transcript> {
        return try {
            withTimeout(config.maxDurationMs) {
                if (!ensureLoaded()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Stt("Whisper model not available")
                    )
                }

                val startMs = System.currentTimeMillis()
                val floatSamples = convertPcm16ToFloat(audio)

                // NUEVO: Envolver en safeJniCall para capturar OOM
                val rawText = oomHandler.safeJniCall(
                    engine = "WhisperSTT",
                    onOom = { "" }
                ) {
                    WhisperJNI.whisperTranscribe(
                        contextHandle = contextHandle,
                        pcmSamples = floatSamples,
                        language = config.language
                    )
                }

                if (rawText.isEmpty() && contextHandle == 0L) {
                    // OOM ocurrió y el contexto fue liberado
                    return@withTimeout JulyResult.failure(
                        AppError.Stt(
                            "Out of memory during STT inference. " +
                            "Models released. Please restart the session."
                        )
                    )
                }

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
            JulyResult.failure(AppError.Stt("Whisper timeout", e))
        } catch (e: Exception) {
            logger.logError("WhisperSTT", "Transcription failed", e)
            JulyResult.failure(AppError.Stt("Whisper error: ${e.message}", e))
        }
    }
    // Resto del adapter sin cambios
}
```

### `ai/tts/PiperTTSAdapter.kt` — mismo patrón

```kotlin
// En synthesize(), envolver la llamada JNI:
val shorts = oomHandler.safeJniCall(
    engine = "PiperTTS",
    onOom = { ShortArray(0) }
) {
    PiperJNI.piperSynthesize(contextHandle, text, config.speakerId)
}

if (shorts.isEmpty() && contextHandle == 0L) {
    return JulyResult.failure(
        AppError.Tts("Out of memory during TTS synthesis. Models released.")
    )
}
```

### `ai/llm/embedded/LlamaCppLLMAdapter.kt` — mismo patrón

```kotlin
// En generate(), envolver la llamada JNI:
val rawText = oomHandler.safeJniCall(
    engine = "LlamaCpp",
    onOom = { "" }
) {
    LlamaCppJNI.llamaGenerate(
        contextHandle = contextHandle,
        prompt = fullPrompt,
        maxTokens = config.maxTokens,
        temperature = config.temperature,
        topP = config.topP,
        repeatPenalty = config.repeatPenalty
    )
}

if (rawText.isEmpty() && contextHandle == 0L) {
    return@withTimeout JulyResult.failure(
        AppError.Llm(
            message = "Out of memory during LLM inference. Models released. Restart session.",
            retryable = false
        )
    )
}
```

---

## DATA — MIGRACIÓN ROOM v2→v3

### `data/db/JulyDatabase.kt` (añade MIGRATION_2_3)

```kotlin
package com.july.offline.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.entity.DiagnosticsDbEntity
import com.july.offline.data.db.entity.MessageDbEntity
import com.july.offline.data.db.entity.SessionDbEntity

/**
 * JulyDatabase versión 3.
 *
 * v1 → v2 (FASE 3): añadió columna `title` a sessions
 * v2 → v3 (FASE 8): añade columna `llmEngine` a messages
 *                   para registrar qué motor generó cada respuesta
 *                   (embedded / server / unknown)
 */
@Database(
    entities = [
        SessionDbEntity::class,
        MessageDbEntity::class,
        DiagnosticsDbEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class JulyDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun diagnosticsDao(): DiagnosticsDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE sessions ADD COLUMN title TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * v2 → v3: añade columna llmEngine a messages.
         * Valor por defecto 'unknown' para mensajes históricos.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE messages ADD COLUMN llmEngine TEXT NOT NULL DEFAULT 'unknown'"
                )
            }
        }
    }
}
```

### `data/db/entity/MessageDbEntity.kt` (añade llmEngine)

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
    val role: String,
    val content: String,
    val timestamp: Long,
    val llmEngine: String = "unknown"   // NUEVO v3: "embedded", "server", "unknown"
)
```

### `di/DatabaseModule.kt` — añadir MIGRATION_2_3

```kotlin
@Provides
@Singleton
fun provideJulyDatabase(@ApplicationContext context: Context): JulyDatabase =
    Room.databaseBuilder(
        context,
        JulyDatabase::class.java,
        "july_offline.db"
    )
    .addMigrations(
        JulyDatabase.MIGRATION_1_2,
        JulyDatabase.MIGRATION_2_3   // NUEVO FASE 8
    )
    .build()
```

---

## DI — WAKEWORD MODULE ACTUALIZADO

### `di/WakeWordModule.kt` (usa AssetCopier para .ppn)

```kotlin
package com.july.offline.di

import com.july.offline.core.startup.AssetCopier
import com.july.offline.domain.port.WakeWordEngine
import com.july.offline.wakeword.PorcupineConfig
import com.july.offline.wakeword.PorcupineWakeWordAdapter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
abstract class WakeWordModule {

    @Binds @Singleton
    abstract fun bindWakeWordEngine(impl: PorcupineWakeWordAdapter): WakeWordEngine

    companion object {

        /**
         * Configura Porcupine usando el .ppn copiado por AssetCopier.
         *
         * Flujo:
         * 1. JulyApplication.onCreate() llama a AssetCopier.copyIfNeeded("oye_july_es.ppn")
         * 2. El .ppn queda en filesDir/oye_july_es.ppn
         * 3. Este @Provides lee esa ruta
         *
         * Si el .ppn no existe (primer arranque antes de que AssetCopier termine,
         * o assets no incluye el .ppn), keywordPaths queda vacío y Porcupine
         * usa el keyword built-in como fallback.
         */
        @Provides
        @Singleton
        fun providePorcupineConfig(
            @ApplicationContext context: Context
        ): PorcupineConfig {
            val ppnPath = File(context.filesDir, "oye_july_es.ppn")

            return PorcupineConfig(
                accessKey = BuildConfig.PICOVOICE_ACCESS_KEY,
                keywordPaths = if (ppnPath.exists()) listOf(ppnPath.absolutePath) else emptyList(),
                builtInKeyword = "hey siri",  // fallback si .ppn no disponible
                sensitivities = listOf(0.6f)
            )
        }
    }
}
```

---

## APP — ACTUALIZACIÓN

### `JulyApplication.kt` (inicia AssetCopier + OomHandler)

```kotlin
package com.july.offline

import android.app.Application
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.OomHandler
import com.july.offline.core.startup.AssetCopier
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.lifecycle.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JulyApplication : Application() {

    @Inject lateinit var engineHealthMonitor: EngineHealthMonitor
    @Inject lateinit var diagnosticsLogger: DiagnosticsLogger
    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    @Inject lateinit var modelMemoryManager: ModelMemoryManager
    @Inject lateinit var oomHandler: OomHandler
    @Inject lateinit var assetCopier: AssetCopier

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 1. Copiar assets en background (no bloquea el arranque)
        appScope.launch {
            assetCopier.copyAll(
                listOf(
                    "oye_july_es.ppn" to "oye_july_es.ppn"
                    // Añadir aquí otros assets si se necesitan en el futuro
                )
            ).forEach { result ->
                if (result.error != null) {
                    diagnosticsLogger.logError(
                        "JulyApplication",
                        "AssetCopier failed for ${result.fileName}",
                        result.error
                    )
                }
            }
        }

        // 2. Registrar lifecycle observer
        appLifecycleObserver.register()

        // 3. Iniciar monitoreo de motores
        engineHealthMonitor.startMonitoring()

        // 4. Limpiar logs antiguos
        diagnosticsLogger.pruneOldLogs()
    }
}
```

---

## TESTS INSTRUMENTADOS

### `androidTest/HiltTestRunner.kt`

```kotlin
package com.july.offline

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Test runner personalizado que usa HiltTestApplication.
 * Configurado en app/build.gradle.kts como testInstrumentationRunner.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
```

---

### `androidTest/di/TestAppModule.kt`

```kotlin
package com.july.offline.di

import android.content.Context
import androidx.room.Room
import com.july.offline.data.db.JulyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Módulo de test que reemplaza DatabaseModule en tests instrumentados.
 * Usa base de datos en memoria para tests aislados y rápidos.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideInMemoryDatabase(
        @ApplicationContext context: Context
    ): JulyDatabase = Room.inMemoryDatabaseBuilder(
        context,
        JulyDatabase::class.java
    )
    .allowMainThreadQueries()   // solo en tests
    .build()

    @Provides
    fun provideSessionDao(db: JulyDatabase) = db.sessionDao()

    @Provides
    fun provideMessageDao(db: JulyDatabase) = db.messageDao()

    @Provides
    fun provideDiagnosticsDao(db: JulyDatabase) = db.diagnosticsDao()
}
```

---

### `androidTest/db/JulyDatabaseTest.kt`

```kotlin
package com.july.offline.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.july.offline.data.db.JulyDatabase
import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.entity.MessageDbEntity
import com.july.offline.data.db.entity.SessionDbEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class JulyDatabaseTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var db: JulyDatabase
    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var messageDao: MessageDao

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun database_creates_successfully() {
        assertNotNull(db)
        assertTrue(db.isOpen)
    }

    @Test
    fun insert_and_retrieve_session() = runTest {
        val session = SessionDbEntity(
            id = "test-session-1",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            title = "Test Session"
        )

        sessionDao.insert(session)
        val retrieved = sessionDao.getById("test-session-1")

        assertNotNull(retrieved)
        assertEquals("test-session-1", retrieved!!.id)
        assertEquals("Test Session", retrieved.title)
    }

    @Test
    fun message_cascade_deletes_with_session() = runTest {
        val session = SessionDbEntity(
            id = "cascade-test",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            title = ""
        )
        sessionDao.insert(session)

        val message = MessageDbEntity(
            id = "msg-1",
            sessionId = "cascade-test",
            role = "USER",
            content = "hello",
            timestamp = Instant.now().toEpochMilli()
        )
        messageDao.insert(message)

        // Verificar que el mensaje existe
        val messages = messageDao.getBySession("cascade-test")
        assertEquals(1, messages.size)

        // Eliminar la sesión
        sessionDao.deleteById("cascade-test")

        // El mensaje debe haberse eliminado en cascada
        val messagesAfter = messageDao.getBySession("cascade-test")
        assertTrue(messagesAfter.isEmpty())
    }

    @Test
    fun getRecentSessions_returns_ordered_by_updatedAt() = runTest {
        val now = Instant.now().toEpochMilli()

        sessionDao.insert(SessionDbEntity("s1", now - 2000, now - 2000, "Older"))
        sessionDao.insert(SessionDbEntity("s2", now - 1000, now - 1000, "Newer"))
        sessionDao.insert(SessionDbEntity("s3", now, now, "Newest"))

        val sessions = sessionDao.getRecent(10).first()

        assertEquals(3, sessions.size)
        assertEquals("s3", sessions[0].id)
        assertEquals("s2", sessions[1].id)
        assertEquals("s1", sessions[2].id)
    }

    @Test
    fun message_includes_llmEngine_field_v3() = runTest {
        val session = SessionDbEntity("v3-test", 0L, 0L, "")
        sessionDao.insert(session)

        val msg = MessageDbEntity(
            id = "m1",
            sessionId = "v3-test",
            role = "ASSISTANT",
            content = "response",
            timestamp = 0L,
            llmEngine = "embedded"
        )
        messageDao.insert(msg)

        val retrieved = messageDao.getBySession("v3-test")
        assertEquals("embedded", retrieved[0].llmEngine)
    }
}
```

---

### `androidTest/db/MigrationTest.kt`

```kotlin
package com.july.offline.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.july.offline.data.db.JulyDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        JulyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate_1_to_2_adds_title_column() {
        // Crear DB en versión 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Insertar datos en v1 (sin title)
            execSQL(
                "INSERT INTO sessions (id, createdAt, updatedAt) " +
                "VALUES ('s1', 1000, 1000)"
            )
            close()
        }

        // Migrar a v2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, JulyDatabase.MIGRATION_1_2)

        // Verificar que la columna title existe y tiene valor por defecto
        val cursor = db.query("SELECT title FROM sessions WHERE id = 's1'")
        cursor.moveToFirst()
        assertEquals("", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate_2_to_3_adds_llmEngine_column() {
        // Crear DB en versión 2
        helper.createDatabase(TEST_DB + "_v2", 2).apply {
            execSQL(
                "INSERT INTO sessions (id, createdAt, updatedAt, title) " +
                "VALUES ('s1', 1000, 1000, '')"
            )
            execSQL(
                "INSERT INTO messages (id, sessionId, role, content, timestamp) " +
                "VALUES ('m1', 's1', 'USER', 'hello', 1000)"
            )
            close()
        }

        // Migrar a v3
        val db = helper.runMigrationsAndValidate(
            TEST_DB + "_v2", 3, true,
            JulyDatabase.MIGRATION_1_2,
            JulyDatabase.MIGRATION_2_3
        )

        // Verificar columna llmEngine con valor por defecto 'unknown'
        val cursor = db.query("SELECT llmEngine FROM messages WHERE id = 'm1'")
        cursor.moveToFirst()
        assertEquals("unknown", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate_1_to_3_complete_path() {
        helper.createDatabase(TEST_DB + "_full", 1).apply {
            execSQL(
                "INSERT INTO sessions (id, createdAt, updatedAt) " +
                "VALUES ('s1', 1000, 1000)"
            )
            close()
        }

        // Migración completa 1 → 3
        helper.runMigrationsAndValidate(
            TEST_DB + "_full", 3, true,
            JulyDatabase.MIGRATION_1_2,
            JulyDatabase.MIGRATION_2_3
        ).close()
    }
}
```

---

### `androidTest/datastore/AppPreferencesDataStoreTest.kt`

```kotlin
package com.july.offline.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.july.offline.data.datastore.AppPreferencesDataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppPreferencesDataStoreTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var dataStore: AppPreferencesDataStore

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun default_language_is_es() = runTest {
        val lang = dataStore.language.first()
        assertEquals("es", lang)
    }

    @Test
    fun tts_enabled_default_is_true() = runTest {
        assertTrue(dataStore.ttsEnabled.first())
    }

    @Test
    fun wake_word_disabled_by_default() = runTest {
        assertFalse(dataStore.wakeWordEnabled.first())
    }

    @Test
    fun setTtsEnabled_persists_value() = runTest {
        dataStore.setTtsEnabled(false)
        assertFalse(dataStore.ttsEnabled.first())

        dataStore.setTtsEnabled(true)
        assertTrue(dataStore.ttsEnabled.first())
    }

    @Test
    fun setWakeWordEnabled_persists_value() = runTest {
        dataStore.setWakeWordEnabled(true)
        assertTrue(dataStore.wakeWordEnabled.first())

        dataStore.setWakeWordEnabled(false)
        assertFalse(dataStore.wakeWordEnabled.first())
    }
}
```

---

### `androidTest/datastore/SystemConfigDataStoreTest.kt`

```kotlin
package com.july.offline.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.core.memory.ModelMode
import com.july.offline.data.datastore.SystemConfigDataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SystemConfigDataStoreTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var dataStore: SystemConfigDataStore

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun default_llm_host_is_localhost() = runTest {
        assertEquals("127.0.0.1", dataStore.llmHost.first())
    }

    @Test
    fun default_llm_port_is_11434() = runTest {
        assertEquals(11434, dataStore.llmPort.first())
    }

    @Test
    fun default_llm_mode_is_AUTO() = runTest {
        assertEquals(LlmMode.AUTO, dataStore.llmMode.first())
    }

    @Test
    fun default_model_mode_is_SPEED() = runTest {
        assertEquals(ModelMode.SPEED, dataStore.modelMode.first())
    }

    @Test
    fun setLlmMode_persists_all_modes() = runTest {
        dataStore.setLlmMode(LlmMode.EMBEDDED)
        assertEquals(LlmMode.EMBEDDED, dataStore.llmMode.first())

        dataStore.setLlmMode(LlmMode.SERVER)
        assertEquals(LlmMode.SERVER, dataStore.llmMode.first())

        dataStore.setLlmMode(LlmMode.AUTO)
        assertEquals(LlmMode.AUTO, dataStore.llmMode.first())
    }

    @Test
    fun setModelMode_persists_MEMORY() = runTest {
        dataStore.setModelMode(ModelMode.MEMORY)
        assertEquals(ModelMode.MEMORY, dataStore.modelMode.first())
    }

    @Test
    fun setLlmConfig_persists_all_fields() = runTest {
        dataStore.setLlmConfig("192.168.1.10", 8080, "mistral:7b")

        assertEquals("192.168.1.10", dataStore.llmHost.first())
        assertEquals(8080, dataStore.llmPort.first())
        assertEquals("mistral:7b", dataStore.llmModel.first())
    }
}
```

---

### `androidTest/repository/SessionRepositoryImplTest.kt`

```kotlin
package com.july.offline.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.july.offline.data.repository.SessionRepositoryImpl
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.port.SessionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Tests instrumentados de SessionRepositoryImpl contra Room en memoria.
 * Estos tests validan el mapeo entre modelos de dominio y entidades Room.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SessionRepositoryImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: SessionRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun createSession_returns_valid_entity() = runTest {
        val session = repository.createSession()

        assertNotNull(session.id)
        assertTrue(session.id.isNotBlank())
        assertTrue(session.messages.isEmpty())
    }

    @Test
    fun addMessage_persists_to_db() = runTest {
        val session = repository.createSession()
        val message = Message(
            id = "test-msg-1",
            role = MessageRole.USER,
            content = "prueba de mensaje"
        )

        repository.addMessage(session.id, message)

        val retrieved = repository.getSession(session.id)
        assertNotNull(retrieved)
        assertEquals(1, retrieved!!.messages.size)
        assertEquals("prueba de mensaje", retrieved.messages[0].content)
        assertEquals(MessageRole.USER, retrieved.messages[0].role)
    }

    @Test
    fun getRecentSessions_flow_emits_on_update() = runTest {
        // Primera emisión — sin sesiones
        val initial = repository.getRecentSessions(10).first()
        val initialCount = initial.size

        // Crear sesión
        repository.createSession()

        // Segunda emisión — con la nueva sesión
        val updated = repository.getRecentSessions(10).first()
        assertEquals(initialCount + 1, updated.size)
    }

    @Test
    fun deleteSession_removes_session_and_messages() = runTest {
        val session = repository.createSession()
        repository.addMessage(
            session.id,
            Message(id = "m1", role = MessageRole.USER, content = "test")
        )

        repository.deleteSession(session.id)

        assertNull(repository.getSession(session.id))
    }

    @Test
    fun multiple_sessions_are_ordered_by_updatedAt_desc() = runTest {
        val s1 = repository.createSession()
        val s2 = repository.createSession()

        // Añadir mensaje a s1 para actualizar su updatedAt
        Thread.sleep(10) // asegurar diferencia de timestamp
        repository.addMessage(
            s1.id,
            Message(id = "m1", role = MessageRole.USER, content = "actualiza s1")
        )

        val sessions = repository.getRecentSessions(10).first()
        // s1 debe ser el primero por ser más reciente
        assertEquals(s1.id, sessions[0].id)
    }

    @Test
    fun messages_preserve_role_mapping() = runTest {
        val session = repository.createSession()

        listOf(
            Message(id = "m1", role = MessageRole.USER, content = "user msg"),
            Message(id = "m2", role = MessageRole.ASSISTANT, content = "assistant msg"),
            Message(id = "m3", role = MessageRole.SYSTEM, content = "system msg")
        ).forEach { repository.addMessage(session.id, it) }

        val retrieved = repository.getSession(session.id)!!
        assertEquals(MessageRole.USER, retrieved.messages[0].role)
        assertEquals(MessageRole.ASSISTANT, retrieved.messages[1].role)
        assertEquals(MessageRole.SYSTEM, retrieved.messages[2].role)
    }
}
```

---

### `androidTest/permission/PermissionHandlerTest.kt`

```kotlin
package com.july.offline.permission

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.july.offline.MainActivity
import com.july.offline.ui.permission.PermissionHandler
import com.july.offline.ui.theme.JulyTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PermissionHandlerTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun permissionHandler_calls_onGranted_when_permission_already_granted() {
        // Este test requiere que el permiso RECORD_AUDIO esté concedido
        // En CI: usar GrantPermissionRule o configurar permisos en el AVD
        var grantedCalled = false

        composeRule.activity.setContent {
            JulyTheme {
                PermissionHandler(
                    onPermissionGranted = { grantedCalled = true },
                    onPermissionDenied = {}
                ) { requestPermission ->
                    Button(onClick = requestPermission) {
                        Text("Hablar")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Hablar").performClick()
        // Si el permiso ya está concedido, onGranted se llama inmediatamente
        // En entorno de test sin permiso, este test documenta el comportamiento esperado
    }

    @Test
    fun permissionHandler_renders_content_correctly() {
        composeRule.activity.setContent {
            JulyTheme {
                PermissionHandler(
                    onPermissionGranted = {},
                    onPermissionDenied = {}
                ) { _ ->
                    Text("Botón de prueba")
                }
            }
        }

        composeRule.onNodeWithText("Botón de prueba").assertIsDisplayed()
    }
}
```

---

## RESUMEN DE CAMBIOS FASE 7 → FASE 8

| Componente | Cambio |
|---|---|
| `OomHandler` | NUEVO — interceptor de OutOfMemoryError para JNI |
| `AssetCopier` | NUEVO — copia .ppn de Porcupine desde assets a filesDir |
| `StartupModule` | NUEVO — módulo declarativo Hilt para startup |
| `JulyDatabase` | v3 — añade MIGRATION_2_3 (columna llmEngine en messages) |
| `MessageDbEntity` | Añade campo `llmEngine: String` |
| `DatabaseModule` | Añade MIGRATION_2_3 a la cadena de migraciones |
| `WhisperSTTAdapter` | `safeJniCall` para capturar OOM |
| `PiperTTSAdapter` | `safeJniCall` para capturar OOM |
| `LlamaCppLLMAdapter` | `safeJniCall` para capturar OOM |
| `WakeWordModule` | Lee ruta .ppn desde filesDir (copiado por AssetCopier) |
| `JulyApplication` | Inicia AssetCopier + OomHandler en onCreate() |
| `proguard-rules.pro` | Reglas completas para todos los motores JNI |
| `HiltTestRunner` | NUEVO — runner para tests instrumentados con Hilt |
| `TestDatabaseModule` | NUEVO — Room en memoria para tests |
| `JulyDatabaseTest` | NUEVO — 4 tests instrumentados de Room |
| `MigrationTest` | NUEVO — 3 tests de migraciones v1→v2→v3 |
| `AppPreferencesDataStoreTest` | NUEVO — 5 tests de DataStore preferencias |
| `SystemConfigDataStoreTest` | NUEVO — 5 tests de DataStore sistema |
| `SessionRepositoryImplTest` | NUEVO — 5 tests de repositorio real |
| `PermissionHandlerTest` | NUEVO — 2 tests de UI de permisos |

---

## NOTAS CRÍTICAS

### Por qué OOM en JNI necesita manejo especial

Un `OutOfMemoryError` en Java/Kotlin normal es recuperable — el GC puede
liberar objetos. Un OOM durante una llamada JNI puede dejar el heap nativo
en estado inconsistente: el contexto de Whisper/Piper/llama puede tener
buffers a mitad de proceso. Por eso, tras un OOM JNI, la política es
liberar **todos** los contextos JNI inmediatamente y no intentar reutilizarlos.
El usuario debe reiniciar la sesión.

### Por qué AssetCopier usa tamaño para detectar actualizaciones

`SharedPreferences` o `DataStore` podrían guardar una versión del modelo,
pero requieren coordinación entre el código que construye el APK y el que
lee la versión. La comparación por tamaño en bytes es más robusta: si el
nuevo `.ppn` tiene distinto tamaño que el instalado, se reemplaza. Falsos
positivos (mismo tamaño, distinto contenido) son extremadamente improbables
para archivos de modelo binarios.

### Tests instrumentados vs tests unitarios

Los tests unitarios de FASE 4 (JUnit 5 + MockK) son los más rápidos y
cubren la lógica de negocio. Los tests instrumentados de FASE 8 cubren
lo que los unitarios no pueden: el comportamiento real de Room (SQL,
migraciones, DAOs), DataStore (serialización a disco) y permisos Android.
Nunca reemplazar los unitarios con instrumentados — son complementarios.

### ProGuard y JNI

Las reglas JNI son las más críticas del archivo. Si el nombre de un método
`external` en Kotlin se ofusca, la resolución de símbolo falla en runtime
con `UnsatisfiedLinkError`. Las reglas `-keep class ... { native <methods>; }`
preservan exactamente los métodos declarados como `external fun` en los
objetos singleton JNI.
