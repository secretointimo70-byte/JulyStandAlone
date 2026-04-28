# JULY OFFLINE — FASE 6
## LLM Embebido (llama.cpp) + Fallback a Servidor Local
### `com.july.offline`

**Alcance FASE 6:**
- `LlamaCppJNI.kt` — bridge JNI hacia libllama.so
- `LlamaCppLLMAdapter` — implementa LanguageModelEngine via llama.cpp
- `LlamaCppConfig` — configuración del modelo GGUF Q4_K_M
- `LlmRouter` — orquesta primario (embebido) → fallback (servidor)
- `LlmModule` actualizado — bindea LlmRouter como LanguageModelEngine
- `LlmMode` enum — EMBEDDED / SERVER / AUTO
- Script de descarga del modelo GGUF
- `ModelMemoryManager` extendido para motor LLM
- `EngineHealthState` con estado LLM embebido
- Settings: selector de modo LLM
- Tests: `LlamaCppLLMAdapterTest`, `LlmRouterTest`

**Decisión de diseño:**
LlmRouter implementa LanguageModelEngine. El orquestador no sabe si usa
llama.cpp o HTTP — el contrato es idéntico. Si el embebido falla al generar
(modelo no cargado, OOM, timeout JNI), LlmRouter intenta el servidor local.
Si ambos fallan, devuelve AppError.Llm normal.

---

## ÍNDICE DE ARCHIVOS FASE 6

### NUEVOS — AI/LLM
- `ai/llm/embedded/LlamaCppConfig.kt`
- `ai/llm/embedded/LlamaCppJNI.kt`
- `ai/llm/embedded/LlamaCppLLMAdapter.kt`
- `ai/llm/router/LlmMode.kt`
- `ai/llm/router/LlmRouter.kt`

### MODIFICADOS
- `core/memory/ModelMemoryManager.kt` (añade LLM engine type)
- `core/memory/ModelState.kt` (sin cambios — ya cubre LLM)
- `core/memory/EngineType.kt` (añade LLM)
- `domain/model/EngineHealthState.kt` (añade llmEmbeddedStatus)
- `data/datastore/SystemConfigDataStore.kt` (añade llmMode)
- `di/LlmModule.kt` (bindea LlmRouter, reemplaza binding anterior)
- `di/EngineModule.kt` (elimina binding LLM — pasa a LlmModule)
- `scripts/download_llm_model.sh`
- `docs/SETUP_FASE6.md`

### NUEVOS — SETTINGS UI
- `settings/AppSettings.kt` (añade llmMode)
- `ui/settings/SettingsViewModel.kt` (añade setLlmMode)
- `ui/settings/SettingsScreen.kt` (selector modo LLM)

### NUEVOS — TESTS
- `ai/llm/LlamaCppLLMAdapterTest.kt`
- `ai/llm/LlmRouterTest.kt`
- `testutil/FakeLlamaCppJNI.kt`

---

## SETUP

### `docs/SETUP_FASE6.md`

```markdown
# Setup FASE 6 — LLM Embebido (llama.cpp)

## Modelo requerido

Formato: GGUF Q4_K_M
Tamaño: ~2.5 GB (modelo 7B parámetros)
Modelo recomendado: Llama-3.2-3B-Instruct (más liviano para móvil, ~2GB)
Alternativa: Mistral-7B-Instruct-v0.3 (~4.1GB)

Ejecutar desde la raíz del proyecto:
    bash scripts/download_llm_model.sh

## Binarios llama.cpp precompilados para Android

Repositorio oficial: https://github.com/ggerganov/llama.cpp
Release con binarios Android: https://github.com/ggerganov/llama.cpp/releases

Archivos necesarios en jniLibs:
    app/src/main/jniLibs/arm64-v8a/libllama.so
    app/src/main/jniLibs/arm64-v8a/libggml.so
    app/src/main/jniLibs/x86_64/libllama.so
    app/src/main/jniLibs/x86_64/libggml.so

Nota: libggml.so es la biblioteca de operaciones matemáticas de llama.cpp.
Debe cargarse ANTES que libllama.so.

## Instalar modelo en el dispositivo

    bash scripts/setup_fase6.sh

Copia el modelo GGUF a /data/data/com.july.offline/files/ via ADB.
Requiere ~2.5 GB libres en el dispositivo.

## Memoria RAM requerida

Llama-3.2-3B Q4_K_M: ~2.0 GB RAM
Whisper-small: ~466 MB
Piper-sharvard: ~300 MB
Proceso Android: ~200 MB
─────────────────────
Total foreground: ~3.0 GB RAM mínimo

Dispositivos recomendados: 6 GB RAM o más.
En dispositivos de 4 GB: usar ModelMode.MEMORY y modelo 3B.

## Verificación en Logcat

[Engine:LLM_EMBEDDED] generated [XXXms]   ← llama.cpp funcionando
[LlmRouter] Using embedded LLM           ← modo embebido activo
[LlmRouter] Embedded failed, trying server fallback ← fallback activado
```

---

### `scripts/download_llm_model.sh`

```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="$SCRIPT_DIR/models"

echo "=== July Offline — Descarga del modelo LLM GGUF ==="
echo ""

mkdir -p "$MODELS_DIR"

# Verificar espacio (~2.5GB necesarios para Llama-3.2-3B Q4_K_M)
AVAILABLE_KB=$(df -k "$MODELS_DIR" | awk 'NR==2 {print $4}')
NEEDED_KB=2600000
if [ "$AVAILABLE_KB" -lt "$NEEDED_KB" ]; then
    echo "ERROR: Espacio insuficiente. Necesitas ~2.5GB libres."
    echo "Disponible: $((AVAILABLE_KB / 1024)) MB"
    exit 1
fi

MODEL_FILE="$MODELS_DIR/Llama-3.2-3B-Instruct-Q4_K_M.gguf"

if [ -f "$MODEL_FILE" ]; then
    echo "Modelo ya existe: $MODEL_FILE"
    echo "Tamaño: $(du -sh "$MODEL_FILE" | cut -f1)"
else
    echo "Descargando Llama-3.2-3B-Instruct Q4_K_M (~2.0 GB)..."
    echo "Fuente: Hugging Face — bartowski/Llama-3.2-3B-Instruct-GGUF"
    echo ""

    MODEL_URL="https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"

    curl -L \
        --progress-bar \
        --retry 3 \
        --retry-delay 5 \
        -o "$MODEL_FILE" \
        "$MODEL_URL"

    echo ""
    echo "OK: $MODEL_FILE ($(du -sh "$MODEL_FILE" | cut -f1))"
fi

echo ""
echo "=== Descarga completada ==="
echo "Siguiente paso: bash scripts/setup_fase6.sh"
```

---

### `scripts/setup_fase6.sh`

```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="$SCRIPT_DIR/models"
APP_PKG="com.july.offline"

MODEL_FILE="$MODELS_DIR/Llama-3.2-3B-Instruct-Q4_K_M.gguf"

echo "=== July Offline — Setup FASE 6: Modelo LLM en dispositivo ==="

if ! command -v adb &> /dev/null; then
    echo "ERROR: adb no encontrado."
    exit 1
fi

DEVICE=$(adb devices | grep -v "List" | grep -v "^$" | head -1 | cut -f1)
if [ -z "$DEVICE" ]; then
    echo "ERROR: No hay dispositivo conectado."
    exit 1
fi
echo "Dispositivo: $DEVICE"

if [ ! -f "$MODEL_FILE" ]; then
    echo "ERROR: Modelo no encontrado: $MODEL_FILE"
    echo "Ejecuta primero: bash scripts/download_llm_model.sh"
    exit 1
fi

echo ""
echo "Copiando modelo (~2 GB, puede tardar varios minutos)..."
adb -s "$DEVICE" push "$MODEL_FILE" "/sdcard/llm_model.gguf"
adb -s "$DEVICE" shell run-as "$APP_PKG" \
    cp /sdcard/llm_model.gguf files/Llama-3.2-3B-Instruct-Q4_K_M.gguf
adb -s "$DEVICE" shell rm /sdcard/llm_model.gguf
echo "OK"

echo ""
echo "Archivos en el dispositivo:"
adb -s "$DEVICE" shell run-as "$APP_PKG" ls -lh files/
echo ""
echo "Setup completado. Compila y ejecuta desde Android Studio."
```

---

## CORE — ACTUALIZACIONES

### `core/memory/ModelMemoryManager.kt` (añade soporte LLM)

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
    private val dispatchers: CoroutineDispatchers
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _sttState = MutableStateFlow(ModelState.UNLOADED)
    val sttState: StateFlow<ModelState> = _sttState.asStateFlow()

    private val _ttsState = MutableStateFlow(ModelState.UNLOADED)
    val ttsState: StateFlow<ModelState> = _ttsState.asStateFlow()

    // NUEVO FASE 6 — estado del LLM embebido
    private val _llmState = MutableStateFlow(ModelState.UNLOADED)
    val llmState: StateFlow<ModelState> = _llmState.asStateFlow()

    @Volatile var currentMode: ModelMode = ModelMode.SPEED

    val sttMutex = Mutex()
    val ttsMutex = Mutex()
    val llmMutex = Mutex()   // NUEVO FASE 6

    private var sttReleaseCallback: (suspend () -> Unit)? = null
    private var ttsReleaseCallback: (suspend () -> Unit)? = null
    private var llmReleaseCallback: (suspend () -> Unit)? = null   // NUEVO FASE 6

    open fun registerSttReleaseCallback(callback: suspend () -> Unit) {
        sttReleaseCallback = callback
    }

    open fun registerTtsReleaseCallback(callback: suspend () -> Unit) {
        ttsReleaseCallback = callback
    }

    // NUEVO FASE 6
    open fun registerLlmReleaseCallback(callback: suspend () -> Unit) {
        llmReleaseCallback = callback
    }

    open fun notifyModelLoaded(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.LOADED
            EngineType.TTS -> _ttsState.value = ModelState.LOADED
            EngineType.LLM -> _llmState.value = ModelState.LOADED  // NUEVO
        }
        logger.logInfo("ModelMemoryManager", "${engine.name} loaded (mode: $currentMode)")
    }

    open fun notifyModelReleased(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.UNLOADED
            EngineType.TTS -> _ttsState.value = ModelState.UNLOADED
            EngineType.LLM -> _llmState.value = ModelState.UNLOADED  // NUEVO
        }
        logger.logInfo("ModelMemoryManager", "${engine.name} released")
    }

    open fun notifyModelLoading(engine: EngineType) {
        when (engine) {
            EngineType.STT -> _sttState.value = ModelState.LOADING
            EngineType.TTS -> _ttsState.value = ModelState.LOADING
            EngineType.LLM -> _llmState.value = ModelState.LOADING  // NUEVO
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

    val isAnyModelLoading: Boolean
        get() = _sttState.value == ModelState.LOADING ||
                _ttsState.value == ModelState.LOADING ||
                _llmState.value == ModelState.LOADING

    val allModelsReady: Boolean
        get() = _sttState.value == ModelState.LOADED &&
                _ttsState.value == ModelState.LOADED &&
                _llmState.value == ModelState.LOADED
}

// ACTUALIZADO FASE 6 — añade LLM
enum class EngineType { STT, TTS, LLM }
```

---

## AI — LLM EMBEBIDO

### `ai/llm/embedded/LlamaCppConfig.kt`

```kotlin
package com.july.offline.ai.llm.embedded

/**
 * Configuración del motor llama.cpp embebido.
 *
 * @param modelPath ruta absoluta al archivo .gguf en el dispositivo
 * @param contextSize ventana de contexto en tokens (4096 recomendado para 3B)
 * @param threads hilos de CPU para inferencia (recomendado: nCores - 1)
 * @param gpuLayers capas a offloadear a GPU (0 = solo CPU, recomendado en Android)
 * @param maxTokens máximo de tokens a generar por respuesta
 * @param temperature temperatura de muestreo [0.0, 2.0]
 * @param topP nucleus sampling (0.9 recomendado)
 * @param repeatPenalty penalización por repetición (1.1 recomendado)
 * @param systemPrompt prompt de sistema enviado en cada sesión
 * @param inferenceTimeoutMs timeout de inferencia en milisegundos
 */
data class LlamaCppConfig(
    val modelPath: String,
    val contextSize: Int = 4096,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val repeatPenalty: Float = 1.1f,
    val systemPrompt: String =
        "Eres July, un asistente de voz offline. " +
        "Responde de forma concisa y natural. " +
        "Máximo 3 oraciones por respuesta.",
    val inferenceTimeoutMs: Long = 120_000L   // 2 min para modelos grandes
) {
    init {
        require(contextSize in 512..32768) { "contextSize must be in [512, 32768]" }
        require(threads in 1..16) { "threads must be in [1, 16]" }
        require(maxTokens in 1..4096) { "maxTokens must be in [1, 4096]" }
        require(temperature in 0f..2f) { "temperature must be in [0.0, 2.0]" }
    }
}
```

---

### `ai/llm/embedded/LlamaCppJNI.kt`

```kotlin
package com.july.offline.ai.llm.embedded

/**
 * Bridge JNI hacia libllama.so + libggml.so (llama.cpp).
 *
 * Convención de nombres JNI:
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaInit
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaGenerate
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaFree
 *
 * ORDEN DE CARGA CRÍTICO:
 * libggml.so debe cargarse ANTES que libllama.so.
 * libggml contiene las operaciones de álgebra lineal que libllama referencia.
 */
object LlamaCppJNI {

    init {
        System.loadLibrary("ggml")    // primero — dependencia de llama
        System.loadLibrary("llama")   // segundo
    }

    /**
     * Inicializa el contexto llama.cpp cargando el modelo GGUF en memoria.
     *
     * @param modelPath ruta absoluta al archivo .gguf
     * @param contextSize ventana de contexto en tokens
     * @param threads número de hilos de CPU
     * @param gpuLayers capas a offloadear a GPU (0 = solo CPU)
     * @return handle nativo (Long) al llama_context, o 0L si falla
     *
     * ADVERTENCIA: Esta llamada puede tardar 5-15 segundos para modelos 7B.
     * Siempre llamar desde un hilo de IO, nunca desde Main.
     */
    external fun llamaInit(
        modelPath: String,
        contextSize: Int,
        threads: Int,
        gpuLayers: Int
    ): Long

    /**
     * Genera texto dado un prompt completo (incluyendo historial formateado).
     *
     * @param contextHandle handle devuelto por llamaInit()
     * @param prompt texto completo incluyendo system prompt e historial
     * @param maxTokens máximo de tokens a generar
     * @param temperature temperatura de muestreo
     * @param topP nucleus sampling
     * @param repeatPenalty penalización por repetición
     * @return texto generado, o cadena vacía si falla
     *
     * NOTA: Esta función es bloqueante. Para streaming ver llamaGenerateToken().
     */
    external fun llamaGenerate(
        contextHandle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        repeatPenalty: Float
    ): String

    /**
     * Devuelve el número de tokens en un texto según el tokenizador del modelo.
     * Útil para verificar que el prompt no excede contextSize.
     */
    external fun llamaTokenize(contextHandle: Long, text: String): Int

    /**
     * Libera el contexto llama.cpp y el modelo de memoria.
     * Siempre llamar cuando el modelo ya no se necesite.
     */
    external fun llamaFree(contextHandle: Long)

    /**
     * Devuelve información del modelo cargado (nombre, parámetros, arquitectura).
     * Solo válido si contextHandle != 0L.
     */
    external fun llamaModelInfo(contextHandle: Long): String
}

/*
 * ── REFERENCIA C++ (llama_jni.cpp) ────────────────────────────────────────────
 *
 * Solo necesario si compilas desde fuente. Con .so precompilados NO es necesario.
 *
 * #include <jni.h>
 * #include "llama.h"
 * #include <string>
 * #include <vector>
 *
 * struct LlamaCtx {
 *     llama_model* model;
 *     llama_context* ctx;
 * };
 *
 * extern "C" {
 *
 * JNIEXPORT jlong JNICALL
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaInit(
 *     JNIEnv* env, jobject,
 *     jstring modelPath, jint contextSize, jint threads, jint gpuLayers) {
 *
 *     llama_backend_init();
 *
 *     const char* path = env->GetStringUTFChars(modelPath, nullptr);
 *
 *     llama_model_params mparams = llama_model_default_params();
 *     mparams.n_gpu_layers = gpuLayers;
 *
 *     llama_model* model = llama_load_model_from_file(path, mparams);
 *     env->ReleaseStringUTFChars(modelPath, path);
 *
 *     if (!model) return 0L;
 *
 *     llama_context_params cparams = llama_context_default_params();
 *     cparams.n_ctx = contextSize;
 *     cparams.n_threads = threads;
 *     cparams.n_threads_batch = threads;
 *
 *     llama_context* ctx = llama_new_context_with_model(model, cparams);
 *     if (!ctx) {
 *         llama_free_model(model);
 *         return 0L;
 *     }
 *
 *     LlamaCtx* lctx = new LlamaCtx{model, ctx};
 *     return reinterpret_cast<jlong>(lctx);
 * }
 *
 * JNIEXPORT jstring JNICALL
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaGenerate(
 *     JNIEnv* env, jobject,
 *     jlong handle, jstring prompt,
 *     jint maxTokens, jfloat temperature, jfloat topP, jfloat repeatPenalty) {
 *
 *     LlamaCtx* lctx = reinterpret_cast<LlamaCtx*>(handle);
 *     if (!lctx) return env->NewStringUTF("");
 *
 *     const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
 *
 *     // Tokenizar el prompt
 *     std::vector<llama_token> tokens(llama_n_ctx(lctx->ctx));
 *     int nTokens = llama_tokenize(
 *         lctx->model, promptStr, strlen(promptStr),
 *         tokens.data(), tokens.size(), true, false);
 *     env->ReleaseStringUTFChars(prompt, promptStr);
 *
 *     if (nTokens < 0) return env->NewStringUTF("");
 *     tokens.resize(nTokens);
 *
 *     // Batch de evaluación
 *     llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size(), 0, 0);
 *     if (llama_decode(lctx->ctx, batch) != 0) return env->NewStringUTF("");
 *
 *     // Sampling params
 *     llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
 *     llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
 *     llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
 *     llama_sampler_chain_add(sampler, llama_sampler_init_penalties(
 *         llama_n_vocab(lctx->model), llama_token_eos(lctx->model),
 *         llama_token_nl(lctx->model), 64, repeatPenalty, 0.0f, 0.0f, false, false));
 *
 *     std::string result;
 *     for (int i = 0; i < maxTokens; ++i) {
 *         llama_token token = llama_sampler_sample(sampler, lctx->ctx, -1);
 *         if (llama_token_is_eog(lctx->model, token)) break;
 *
 *         char buf[256];
 *         int n = llama_token_to_piece(lctx->model, token, buf, sizeof(buf), 0, true);
 *         if (n > 0) result.append(buf, n);
 *
 *         llama_batch nb = llama_batch_get_one(&token, 1, nTokens + i, 0);
 *         if (llama_decode(lctx->ctx, nb) != 0) break;
 *     }
 *
 *     llama_sampler_free(sampler);
 *     llama_kv_cache_clear(lctx->ctx);
 *
 *     return env->NewStringUTF(result.c_str());
 * }
 *
 * JNIEXPORT jint JNICALL
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaTokenize(
 *     JNIEnv* env, jobject, jlong handle, jstring text) {
 *     LlamaCtx* lctx = reinterpret_cast<LlamaCtx*>(handle);
 *     if (!lctx) return -1;
 *     const char* txt = env->GetStringUTFChars(text, nullptr);
 *     std::vector<llama_token> tokens(4096);
 *     int n = llama_tokenize(lctx->model, txt, strlen(txt),
 *         tokens.data(), tokens.size(), true, false);
 *     env->ReleaseStringUTFChars(text, txt);
 *     return n;
 * }
 *
 * JNIEXPORT void JNICALL
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaFree(
 *     JNIEnv*, jobject, jlong handle) {
 *     LlamaCtx* lctx = reinterpret_cast<LlamaCtx*>(handle);
 *     if (lctx) {
 *         llama_free(lctx->ctx);
 *         llama_free_model(lctx->model);
 *         llama_backend_free();
 *         delete lctx;
 *     }
 * }
 *
 * JNIEXPORT jstring JNICALL
 * Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaModelInfo(
 *     JNIEnv* env, jobject, jlong handle) {
 *     LlamaCtx* lctx = reinterpret_cast<LlamaCtx*>(handle);
 *     if (!lctx) return env->NewStringUTF("{}");
 *     std::string info = "{\"name\":\"";
 *     info += llama_model_desc(lctx->model);
 *     info += "\",\"params\":";
 *     info += std::to_string(llama_model_n_params(lctx->model));
 *     info += "}";
 *     return env->NewStringUTF(info.c_str());
 * }
 *
 * } // extern "C"
 */
```

---

### `ai/llm/embedded/LlamaCppLLMAdapter.kt`

```kotlin
package com.july.offline.ai.llm.embedded

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.EngineType
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject

/**
 * Adaptador LLM que usa llama.cpp via JNI.
 * Implementa LanguageModelEngine — el contrato es idéntico al servidor local.
 *
 * Gestión del contexto JNI:
 * - Carga lazy en el primer generate() o isAvailable()
 * - Liberación gestionada por ModelMemoryManager en modo MEMORY
 * - El contexto llama.cpp mantiene el KV cache entre llamadas de la misma sesión
 *   (llama_kv_cache_clear() se llama tras cada respuesta completa para evitar
 *   acumulación entre sesiones distintas)
 *
 * Construcción del prompt:
 * Llama 3.2 usa el formato ChatML / Instruct:
 *   <|system|>\n{system}\n<|end|>\n
 *   <|user|>\n{user}\n<|end|>\n
 *   <|assistant|>\n
 *
 * Otros modelos pueden requerir formatos distintos. En FASE 7 esto
 * se puede abstraer con un PromptFormatter por modelo.
 */
class LlamaCppLLMAdapter @Inject constructor(
    private val config: LlamaCppConfig,
    private val modelMemoryManager: ModelMemoryManager,
    private val logger: DiagnosticsLogger
) : LanguageModelEngine {

    @Volatile private var contextHandle: Long = 0L

    init {
        modelMemoryManager.registerLlmReleaseCallback { releaseContext() }
    }

    private suspend fun ensureLoaded(): Boolean {
        if (contextHandle != 0L) return true

        return modelMemoryManager.llmMutex.withLock {
            if (contextHandle != 0L) return@withLock true

            if (!File(config.modelPath).exists()) {
                logger.logError("LlamaCpp", "Model not found: ${config.modelPath}")
                return@withLock false
            }

            modelMemoryManager.notifyModelLoading(EngineType.LLM)
            logger.logInfo(
                "LlamaCpp",
                "Loading model ${config.modelPath} " +
                "(ctx=${config.contextSize}, threads=${config.threads})..."
            )

            val startMs = System.currentTimeMillis()
            val handle = LlamaCppJNI.llamaInit(
                modelPath = config.modelPath,
                contextSize = config.contextSize,
                threads = config.threads,
                gpuLayers = config.gpuLayers
            )
            val loadMs = System.currentTimeMillis() - startMs

            return@withLock if (handle != 0L) {
                contextHandle = handle
                modelMemoryManager.notifyModelLoaded(EngineType.LLM)
                logger.logInfo("LlamaCpp", "Model loaded in ${loadMs}ms")
                true
            } else {
                logger.logError("LlamaCpp", "llamaInit() returned 0 — load failed")
                false
            }
        }
    }

    suspend fun releaseContext() {
        modelMemoryManager.llmMutex.withLock {
            if (contextHandle != 0L) {
                LlamaCppJNI.llamaFree(contextHandle)
                contextHandle = 0L
                modelMemoryManager.notifyModelReleased(EngineType.LLM)
                logger.logInfo("LlamaCpp", "Context released")
            }
        }
    }

    override suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {
        return try {
            withTimeout(config.inferenceTimeoutMs) {
                if (!ensureLoaded()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Llm(
                            message = "LlamaCpp model not available at ${config.modelPath}",
                            retryable = false
                        )
                    )
                }

                val fullPrompt = buildPrompt(prompt, history)

                // Verificar que el prompt no excede el contexto
                val tokenCount = LlamaCppJNI.llamaTokenize(contextHandle, fullPrompt)
                if (tokenCount > config.contextSize - config.maxTokens) {
                    logger.logWarning(
                        "LlamaCpp",
                        "Prompt too long ($tokenCount tokens), truncating history"
                    )
                    // Reintentar con historial recortado (últimos 4 mensajes)
                    val truncatedHistory = history.takeLast(4)
                    return@withTimeout generate(prompt, truncatedHistory)
                }

                logger.logInfo("LlamaCpp", "Generating ($tokenCount prompt tokens)...")
                val startMs = System.currentTimeMillis()

                val rawText = LlamaCppJNI.llamaGenerate(
                    contextHandle = contextHandle,
                    prompt = fullPrompt,
                    maxTokens = config.maxTokens,
                    temperature = config.temperature,
                    topP = config.topP,
                    repeatPenalty = config.repeatPenalty
                )

                val latencyMs = System.currentTimeMillis() - startMs
                logger.logEngineEvent("LLM_EMBEDDED", "generated", latencyMs)

                if (rawText.isBlank()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Llm("LlamaCpp returned empty response", retryable = false)
                    )
                }

                JulyResult.success(
                    LlmResponse(
                        text = rawText.trim(),
                        tokenCount = -1,   // llama.cpp no expone eval_count directamente en FASE 6
                        latencyMs = latencyMs,
                        modelName = "llama.cpp:${File(config.modelPath).name}"
                    )
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            JulyResult.failure(
                AppError.Llm(
                    message = "LlamaCpp timeout after ${config.inferenceTimeoutMs}ms",
                    cause = e,
                    retryable = false
                )
            )
        } catch (e: Exception) {
            logger.logError("LlamaCpp", "Generation failed", e)
            JulyResult.failure(
                AppError.Llm(
                    message = "LlamaCpp error: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        return ensureLoaded()
    }

    override suspend fun getModelInfo(): ModelInfo {
        if (contextHandle == 0L) return ModelInfo(name = "llama.cpp (not loaded)")
        return try {
            val infoJson = LlamaCppJNI.llamaModelInfo(contextHandle)
            // Parseo simple sin Gson para evitar dependencia extra
            val name = infoJson
                .substringAfter("\"name\":\"")
                .substringBefore("\"")
            ModelInfo(name = name)
        } catch (e: Exception) {
            ModelInfo(name = "llama.cpp")
        }
    }

    /**
     * Construye el prompt completo en formato ChatML (Llama 3.x Instruct).
     *
     * Formato:
     * <|system|>
     * {systemPrompt}
     * <|end|>
     * <|user|>
     * {message}
     * <|end|>
     * <|assistant|>
     * {message}
     * <|end|>
     * ... (historial)
     * <|user|>
     * {prompt actual}
     * <|end|>
     * <|assistant|>
     *
     * El prompt termina con <|assistant|> para que el modelo complete.
     */
    private fun buildPrompt(prompt: String, history: List<Message>): String {
        val sb = StringBuilder()

        // System prompt
        sb.append("<|system|>\n")
        sb.append(config.systemPrompt)
        sb.append("\n<|end|>\n")

        // Historial (máximo los últimos 10 mensajes para no saturar el contexto)
        history.takeLast(10).forEach { msg ->
            when (msg.role) {
                MessageRole.USER -> {
                    sb.append("<|user|>\n")
                    sb.append(msg.content)
                    sb.append("\n<|end|>\n")
                }
                MessageRole.ASSISTANT -> {
                    sb.append("<|assistant|>\n")
                    sb.append(msg.content)
                    sb.append("\n<|end|>\n")
                }
                MessageRole.SYSTEM -> {
                    // System messages adicionales se ignoran — ya está el principal
                }
            }
        }

        // Turno actual
        sb.append("<|user|>\n")
        sb.append(prompt)
        sb.append("\n<|end|>\n")
        sb.append("<|assistant|>\n")  // el modelo completa desde aquí

        return sb.toString()
    }
}
```

---

## AI — ROUTER

### `ai/llm/router/LlmMode.kt`

```kotlin
package com.july.offline.ai.llm.router

/**
 * Modo de operación del motor LLM.
 *
 * EMBEDDED: Solo llama.cpp embebido. Sin fallback. Falla si el modelo no está disponible.
 * SERVER:   Solo servidor local (Ollama/LM Studio). Comportamiento igual a FASE 3.
 * AUTO:     Embebido por defecto. Fallback al servidor si el embebido falla.
 *           Este es el modo por defecto en FASE 6.
 */
enum class LlmMode {
    EMBEDDED,
    SERVER,
    AUTO
}
```

---

### `ai/llm/router/LlmRouter.kt`

```kotlin
package com.july.offline.ai.llm.router

import com.july.offline.ai.llm.LocalServerLLMAdapter
import com.july.offline.ai.llm.embedded.LlamaCppLLMAdapter
import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.ModelInfo
import com.july.offline.domain.port.LanguageModelEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router LLM que implementa la estrategia embebido-primario / servidor-fallback.
 *
 * Lógica de routing:
 * - AUTO (default): intenta embebido → si falla con error no-network, intenta servidor
 * - EMBEDDED: solo embebido, sin fallback
 * - SERVER: solo servidor local, sin embebido
 *
 * Definición de "falla que activa fallback":
 * - AppError.Llm con retryable = false (modelo no cargado, OOM, timeout JNI)
 * - AppError.Network del embebido (no debería ocurrir pero se maneja)
 * NO activa fallback:
 * - AppError.Llm con retryable = true (se reintenta en el orquestador)
 * - Éxito pero respuesta vacía (se propaga como error normal)
 *
 * El modo se puede cambiar en runtime desde SettingsViewModel.
 */
@Singleton
class LlmRouter @Inject constructor(
    private val embeddedAdapter: LlamaCppLLMAdapter,
    private val serverAdapter: LocalServerLLMAdapter,
    private val logger: DiagnosticsLogger
) : LanguageModelEngine {

    /** Modo actual. Puede cambiarse en runtime desde Settings. */
    @Volatile var currentMode: LlmMode = LlmMode.AUTO

    override suspend fun generate(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {

        return when (currentMode) {
            LlmMode.EMBEDDED -> {
                logger.logInfo("LlmRouter", "Using embedded LLM (EMBEDDED mode)")
                embeddedAdapter.generate(prompt, history)
            }

            LlmMode.SERVER -> {
                logger.logInfo("LlmRouter", "Using server LLM (SERVER mode)")
                serverAdapter.generate(prompt, history)
            }

            LlmMode.AUTO -> generateWithFallback(prompt, history)
        }
    }

    private suspend fun generateWithFallback(
        prompt: String,
        history: List<Message>
    ): JulyResult<LlmResponse> {

        logger.logInfo("LlmRouter", "Using embedded LLM (AUTO mode — primary)")
        val embeddedResult = embeddedAdapter.generate(prompt, history)

        // Si el embebido tuvo éxito, devolver directamente
        if (embeddedResult.isSuccess) return embeddedResult

        val error = embeddedResult.errorOrNull()

        // Decidir si activar fallback
        val shouldFallback = when (error) {
            is AppError.Llm -> !error.retryable  // fallo permanente del embebido
            is AppError.Network -> true           // no debería pasar, pero fallback de seguridad
            else -> false
        }

        if (!shouldFallback) {
            logger.logWarning("LlmRouter", "Embedded failed with retryable error, not falling back")
            return embeddedResult
        }

        // Verificar si el servidor está disponible antes de intentar
        if (!serverAdapter.isAvailable()) {
            logger.logWarning(
                "LlmRouter",
                "Embedded failed and server is unreachable — both unavailable"
            )
            return JulyResult.failure(
                AppError.Llm(
                    message = "Both embedded LLM and local server are unavailable",
                    retryable = false
                )
            )
        }

        logger.logWarning(
            "LlmRouter",
            "Embedded failed (${error?.message}), trying server fallback"
        )
        val serverResult = serverAdapter.generate(prompt, history)

        if (serverResult.isSuccess) {
            logger.logInfo("LlmRouter", "Server fallback succeeded")
        } else {
            logger.logError("LlmRouter", "Both embedded and server failed")
        }

        return serverResult
    }

    override suspend fun isAvailable(): Boolean {
        return when (currentMode) {
            LlmMode.EMBEDDED -> embeddedAdapter.isAvailable()
            LlmMode.SERVER -> serverAdapter.isAvailable()
            LlmMode.AUTO -> embeddedAdapter.isAvailable() || serverAdapter.isAvailable()
        }
    }

    override suspend fun getModelInfo(): ModelInfo {
        return when (currentMode) {
            LlmMode.EMBEDDED -> embeddedAdapter.getModelInfo()
            LlmMode.SERVER -> serverAdapter.getModelInfo()
            LlmMode.AUTO -> if (embeddedAdapter.isAvailable())
                embeddedAdapter.getModelInfo()
            else
                serverAdapter.getModelInfo()
        }
    }
}
```

---

## DI — ACTUALIZACIÓN

### `di/LlmModule.kt` (reemplaza binding anterior)

```kotlin
package com.july.offline.di

import com.july.offline.ai.llm.LlmServerConfig
import com.july.offline.ai.llm.embedded.LlamaCppConfig
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.ai.llm.router.LlmRouter
import com.july.offline.data.datastore.SystemConfigDataStore
import com.july.offline.domain.port.LanguageModelEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * LlmModule actualizado para FASE 6.
 *
 * Cambio respecto a FASE 3-5:
 * - Ya no bindea LocalServerLLMAdapter → LanguageModelEngine directamente
 * - Bindea LlmRouter → LanguageModelEngine
 * - LlmRouter contiene ambos adapters y gestiona la estrategia de routing
 *
 * NOTA: EngineModule.kt debe eliminarse el binding de LanguageModelEngine
 * (o eliminarse completamente si solo contenía ese binding).
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

        private const val DEFAULT_FILES_DIR = "/data/data/com.july.offline/files"

        @Provides
        @Singleton
        fun provideLlamaCppConfig(
            systemConfigDataStore: SystemConfigDataStore
        ): LlamaCppConfig = runBlocking {
            val storedPath = systemConfigDataStore.llmModelPath.first()
            LlamaCppConfig(
                modelPath = storedPath.ifBlank {
                    "$DEFAULT_FILES_DIR/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
                },
                contextSize = 4096,
                threads = 4,
                gpuLayers = 0,
                maxTokens = 512
            )
        }

        /**
         * Configura el LlmRouter con el modo leído desde DataStore.
         * Llamado después de que Hilt construye LlmRouter.
         */
        @Provides
        @Singleton
        fun configureLlmRouter(
            router: LlmRouter,
            systemConfigDataStore: SystemConfigDataStore
        ): LlmRouter {
            val mode = runBlocking { systemConfigDataStore.llmMode.first() }
            router.currentMode = mode
            return router
        }
    }
}
```

**Nota:** En `di/EngineModule.kt` eliminar la línea:
```kotlin
// ELIMINAR en FASE 6:
// @Binds @Singleton
// abstract fun bindLanguageModelEngine(impl: LocalServerLLMAdapter): LanguageModelEngine
```

---

## DATA — ACTUALIZACIÓN

### `data/datastore/SystemConfigDataStore.kt` (añade llmMode y llmModelPath)

```kotlin
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
        val LLM_MODE = stringPreferencesKey("llm_mode")             // NUEVO FASE 6
        val LLM_MODEL_PATH = stringPreferencesKey("llm_model_path") // NUEVO FASE 6
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

    // NUEVO FASE 6
    val llmMode: Flow<LlmMode> = context.sysConfigDataStore.data.map { prefs ->
        when (prefs[Keys.LLM_MODE]) {
            LlmMode.EMBEDDED.name -> LlmMode.EMBEDDED
            LlmMode.SERVER.name -> LlmMode.SERVER
            else -> LlmMode.AUTO
        }
    }

    // NUEVO FASE 6 — ruta al modelo GGUF
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
```

---

## SETTINGS — ACTUALIZACIÓN

### `settings/AppSettings.kt`

```kotlin
package com.july.offline.settings

import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.core.memory.ModelMode

data class AppSettings(
    val language: String = "es",
    val ttsEnabled: Boolean = true,
    val showTranscript: Boolean = true,
    val modelMode: ModelMode = ModelMode.SPEED,
    val llmMode: LlmMode = LlmMode.AUTO    // NUEVO FASE 6
)
```

---

### `ui/settings/SettingsViewModel.kt` (añade setLlmMode)

```kotlin
package com.july.offline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.ai.llm.router.LlmRouter
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
    private val llmRouter: LlmRouter
) : ViewModel() {

    val settings: StateFlow<AppSettings> = combine(
        preferencesDataStore.language,
        preferencesDataStore.ttsEnabled,
        preferencesDataStore.showTranscript,
        systemConfigDataStore.modelMode,
        systemConfigDataStore.llmMode
    ) { language, ttsEnabled, showTranscript, modelMode, llmMode ->
        AppSettings(
            language = language,
            ttsEnabled = ttsEnabled,
            showTranscript = showTranscript,
            modelMode = modelMode,
            llmMode = llmMode
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

    fun setModelMode(mode: ModelMode) {
        viewModelScope.launch {
            systemConfigDataStore.setModelMode(mode)
            modelMemoryManager.currentMode = mode
        }
    }

    fun setLlmMode(mode: LlmMode) {
        viewModelScope.launch {
            systemConfigDataStore.setLlmMode(mode)
            llmRouter.currentMode = mode  // aplica en tiempo real sin reiniciar
        }
    }
}
```

---

### `ui/settings/SettingsScreen.kt` (selector modo LLM)

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
import com.july.offline.ai.llm.router.LlmMode
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
                    IconButton(onClick = onNavigateBack) { Text("←") }
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
            // TTS toggle
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

            // Transcript toggle
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

            // Modo LLM — NUEVO FASE 6
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Motor de IA", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when (settings.llmMode) {
                        LlmMode.AUTO ->
                            "Automático: usa el motor embebido (offline puro). " +
                            "Si falla, usa el servidor local como respaldo."
                        LlmMode.EMBEDDED ->
                            "Solo embebido: llama.cpp local (~2 GB RAM). " +
                            "Sin servidor. Falla si el modelo no está instalado."
                        LlmMode.SERVER ->
                            "Solo servidor: Ollama o LM Studio en 127.0.0.1. " +
                            "Requiere servidor activo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LlmMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.llmMode == mode,
                            onClick = { viewModel.setLlmMode(mode) },
                            label = {
                                Text(
                                    text = when (mode) {
                                        LlmMode.AUTO -> "Auto"
                                        LlmMode.EMBEDDED -> "Embebido"
                                        LlmMode.SERVER -> "Servidor"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Modo memoria
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Modo de memoria", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when (settings.modelMode) {
                        ModelMode.SPEED ->
                            "Velocidad: modelos en RAM siempre (~3 GB con LLM). " +
                            "Primera respuesta instantánea."
                        ModelMode.MEMORY ->
                            "Memoria: libera modelos tras 5 min en segundo plano. " +
                            "Recarga al volver: ~10 s (LLM) + ~3 s (STT/TTS)."
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

### `testutil/FakeLlamaCppJNI.kt`

```kotlin
package com.july.offline.testutil

/**
 * Fake para las llamadas JNI de LlamaCppJNI en tests.
 * Permite simular carga exitosa, fallo de carga y generación.
 *
 * Uso: en tests de LlamaCppLLMAdapter, mockear el objeto LlamaCppJNI
 * usando MockK object mocking:
 *
 *   mockkObject(LlamaCppJNI)
 *   every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns 42L
 *   every { LlamaCppJNI.llamaGenerate(any(), any(), any(), any(), any(), any()) } returns "respuesta"
 *   every { LlamaCppJNI.llamaFree(any()) } just runs
 */
object FakeLlamaCppJNI {
    const val VALID_HANDLE = 42L
    const val FAILED_HANDLE = 0L
    const val FAKE_RESPONSE = "Esta es una respuesta de prueba del LLM embebido."
    const val EMPTY_RESPONSE = ""
}
```

---

### `ai/llm/LlamaCppLLMAdapterTest.kt`

```kotlin
package com.july.offline.ai.llm

import com.july.offline.ai.llm.embedded.LlamaCppConfig
import com.july.offline.ai.llm.embedded.LlamaCppJNI
import com.july.offline.ai.llm.embedded.LlamaCppLLMAdapter
import com.july.offline.core.error.AppError
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.core.memory.ModelMode
import com.july.offline.core.memory.ModelState
import com.july.offline.domain.model.Message
import com.july.offline.domain.model.MessageRole
import com.july.offline.testutil.FakeLlamaCppJNI
import com.july.offline.testutil.TestCoroutineDispatchers
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LlamaCppLLMAdapterTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var adapter: LlamaCppLLMAdapter
    private lateinit var modelMemoryManager: ModelMemoryManager
    private val dispatchers = TestCoroutineDispatchers()
    private lateinit var fakeModelFile: File

    @BeforeEach
    fun setup() {
        mockkObject(LlamaCppJNI)

        fakeModelFile = File(tempDir.toFile(), "test-model.gguf").apply {
            writeBytes(ByteArray(100))  // archivo fake para que File.exists() sea true
        }

        modelMemoryManager = ModelMemoryManager(
            logger = mockk(relaxed = true),
            dispatchers = dispatchers
        )
        modelMemoryManager.currentMode = ModelMode.SPEED

        val config = LlamaCppConfig(
            modelPath = fakeModelFile.absolutePath,
            contextSize = 512,
            threads = 2,
            maxTokens = 64,
            inferenceTimeoutMs = 5_000L
        )

        adapter = LlamaCppLLMAdapter(
            config = config,
            modelMemoryManager = modelMemoryManager,
            logger = mockk(relaxed = true)
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(LlamaCppJNI)
    }

    @Test
    fun `generate exitoso devuelve LlmResponse con texto`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.VALID_HANDLE
        every { LlamaCppJNI.llamaTokenize(any(), any()) } returns 50
        every {
            LlamaCppJNI.llamaGenerate(any(), any(), any(), any(), any(), any())
        } returns FakeLlamaCppJNI.FAKE_RESPONSE

        val result = adapter.generate("hola", emptyList())

        assertTrue(result.isSuccess)
        assertEquals(FakeLlamaCppJNI.FAKE_RESPONSE, result.getOrNull()?.text)
    }

    @Test
    fun `generate con modelo no disponible devuelve AppError Llm`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.FAILED_HANDLE

        val result = adapter.generate("hola", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is AppError.Llm)
        assertFalse((result.errorOrNull() as AppError.Llm).retryable)
    }

    @Test
    fun `generate con respuesta vacia devuelve AppError Llm`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.VALID_HANDLE
        every { LlamaCppJNI.llamaTokenize(any(), any()) } returns 50
        every {
            LlamaCppJNI.llamaGenerate(any(), any(), any(), any(), any(), any())
        } returns FakeLlamaCppJNI.EMPTY_RESPONSE

        val result = adapter.generate("hola", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is AppError.Llm)
    }

    @Test
    fun `generate notifica ModelMemoryManager al cargar`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.VALID_HANDLE
        every { LlamaCppJNI.llamaTokenize(any(), any()) } returns 50
        every {
            LlamaCppJNI.llamaGenerate(any(), any(), any(), any(), any(), any())
        } returns FakeLlamaCppJNI.FAKE_RESPONSE

        adapter.generate("test", emptyList())

        assertEquals(ModelState.LOADED, modelMemoryManager.llmState.value)
    }

    @Test
    fun `generate construye prompt con historial en formato ChatML`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.VALID_HANDLE
        every { LlamaCppJNI.llamaTokenize(any(), any()) } returns 50

        var capturedPrompt = ""
        every {
            LlamaCppJNI.llamaGenerate(any(), capture(slot<String>().also {
                capturedPrompt = it.captured
            }), any(), any(), any(), any())
        } answers {
            capturedPrompt = secondArg()
            FakeLlamaCppJNI.FAKE_RESPONSE
        }

        val history = listOf(
            Message(id = "1", role = MessageRole.USER, content = "pregunta anterior"),
            Message(id = "2", role = MessageRole.ASSISTANT, content = "respuesta anterior")
        )

        adapter.generate("nueva pregunta", history)

        assertTrue(capturedPrompt.contains("<|system|>"))
        assertTrue(capturedPrompt.contains("pregunta anterior"))
        assertTrue(capturedPrompt.contains("respuesta anterior"))
        assertTrue(capturedPrompt.contains("nueva pregunta"))
        assertTrue(capturedPrompt.contains("<|assistant|>"))
    }

    @Test
    fun `releaseContext libera el handle JNI y notifica ModelMemoryManager`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.VALID_HANDLE
        every { LlamaCppJNI.llamaTokenize(any(), any()) } returns 50
        every {
            LlamaCppJNI.llamaGenerate(any(), any(), any(), any(), any(), any())
        } returns FakeLlamaCppJNI.FAKE_RESPONSE
        every { LlamaCppJNI.llamaFree(any()) } just runs

        // Cargar modelo
        adapter.generate("test", emptyList())
        assertEquals(ModelState.LOADED, modelMemoryManager.llmState.value)

        // Liberar
        adapter.releaseContext()

        verify { LlamaCppJNI.llamaFree(FakeLlamaCppJNI.VALID_HANDLE) }
        assertEquals(ModelState.UNLOADED, modelMemoryManager.llmState.value)
    }

    @Test
    fun `isAvailable devuelve false si archivo no existe`() = runTest {
        val config = LlamaCppConfig(modelPath = "/ruta/inexistente/model.gguf")
        val adapterNoModel = LlamaCppLLMAdapter(
            config = config,
            modelMemoryManager = modelMemoryManager,
            logger = mockk(relaxed = true)
        )

        assertFalse(adapterNoModel.isAvailable())
    }

    @Test
    fun `generate con timeout devuelve AppError Llm`() = runTest {
        every { LlamaCppJNI.llamaInit(any(), any(), any(), any()) } returns FakeLlamaCppJNI.VALID_HANDLE
        every { LlamaCppJNI.llamaTokenize(any(), any()) } returns 50
        every {
            LlamaCppJNI.llamaGenerate(any(), any(), any(), any(), any(), any())
        } answers {
            Thread.sleep(200)  // simular lentitud
            FakeLlamaCppJNI.FAKE_RESPONSE
        }

        // Config con timeout muy corto
        val shortConfig = LlamaCppConfig(
            modelPath = fakeModelFile.absolutePath,
            inferenceTimeoutMs = 1L
        )
        val adapterShortTimeout = LlamaCppLLMAdapter(
            config = shortConfig,
            modelMemoryManager = modelMemoryManager,
            logger = mockk(relaxed = true)
        )

        val result = adapterShortTimeout.generate("test", emptyList())

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is AppError.Llm)
        assertTrue((error as AppError.Llm).message.contains("timeout"))
    }
}
```

---

### `ai/llm/LlmRouterTest.kt`

```kotlin
package com.july.offline.ai.llm

import com.july.offline.ai.llm.embedded.LlamaCppLLMAdapter
import com.july.offline.ai.llm.router.LlmMode
import com.july.offline.ai.llm.router.LlmRouter
import com.july.offline.core.error.AppError
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.LlmResponse
import com.july.offline.testutil.FakeLanguageModelEngine
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LlmRouterTest {

    private lateinit var embeddedAdapter: LlamaCppLLMAdapter
    private lateinit var serverAdapter: LocalServerLLMAdapter
    private lateinit var router: LlmRouter

    @BeforeEach
    fun setup() {
        embeddedAdapter = mockk()
        serverAdapter = mockk()
        router = LlmRouter(
            embeddedAdapter = embeddedAdapter,
            serverAdapter = serverAdapter,
            logger = mockk(relaxed = true)
        )
    }

    @Test
    fun `modo AUTO usa embebido cuando esta disponible`() = runTest {
        router.currentMode = LlmMode.AUTO
        val expectedResponse = LlmResponse(text = "respuesta embebida")

        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.success(expectedResponse)

        val result = router.generate("hola", emptyList())

        assertTrue(result.isSuccess)
        assertEquals("respuesta embebida", result.getOrNull()?.text)
        coVerify(exactly = 0) { serverAdapter.generate(any(), any()) }
    }

    @Test
    fun `modo AUTO hace fallback al servidor cuando embebido falla con error permanente`() = runTest {
        router.currentMode = LlmMode.AUTO
        val serverResponse = LlmResponse(text = "respuesta servidor")

        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.failure(
            AppError.Llm("model not loaded", retryable = false)
        )
        coEvery { serverAdapter.isAvailable() } returns true
        coEvery { serverAdapter.generate(any(), any()) } returns JulyResult.success(serverResponse)

        val result = router.generate("hola", emptyList())

        assertTrue(result.isSuccess)
        assertEquals("respuesta servidor", result.getOrNull()?.text)
        coVerify { serverAdapter.generate(any(), any()) }
    }

    @Test
    fun `modo AUTO no hace fallback con error retryable`() = runTest {
        router.currentMode = LlmMode.AUTO

        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.failure(
            AppError.Llm("timeout", retryable = true)
        )

        val result = router.generate("hola", emptyList())

        assertTrue(result.isFailure)
        // Server no debe haberse llamado
        coVerify(exactly = 0) { serverAdapter.generate(any(), any()) }
    }

    @Test
    fun `modo AUTO devuelve error si ambos fallan`() = runTest {
        router.currentMode = LlmMode.AUTO

        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.failure(
            AppError.Llm("embedded failed", retryable = false)
        )
        coEvery { serverAdapter.isAvailable() } returns true
        coEvery { serverAdapter.generate(any(), any()) } returns JulyResult.failure(
            AppError.Network("server unreachable")
        )

        val result = router.generate("hola", emptyList())

        assertTrue(result.isFailure)
    }

    @Test
    fun `modo AUTO devuelve error si embebido falla y servidor no disponible`() = runTest {
        router.currentMode = LlmMode.AUTO

        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.failure(
            AppError.Llm("embedded failed", retryable = false)
        )
        coEvery { serverAdapter.isAvailable() } returns false

        val result = router.generate("hola", emptyList())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { serverAdapter.generate(any(), any()) }
    }

    @Test
    fun `modo EMBEDDED nunca llama al servidor`() = runTest {
        router.currentMode = LlmMode.EMBEDDED
        val response = LlmResponse(text = "respuesta")

        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.success(response)

        router.generate("test", emptyList())

        coVerify(exactly = 0) { serverAdapter.generate(any(), any()) }
    }

    @Test
    fun `modo SERVER nunca llama al embebido`() = runTest {
        router.currentMode = LlmMode.SERVER
        val response = LlmResponse(text = "respuesta")

        coEvery { serverAdapter.generate(any(), any()) } returns JulyResult.success(response)

        router.generate("test", emptyList())

        coVerify(exactly = 0) { embeddedAdapter.generate(any(), any()) }
    }

    @Test
    fun `isAvailable en modo AUTO es true si cualquiera esta disponible`() = runTest {
        router.currentMode = LlmMode.AUTO

        coEvery { embeddedAdapter.isAvailable() } returns false
        coEvery { serverAdapter.isAvailable() } returns true

        assertTrue(router.isAvailable())
    }

    @Test
    fun `isAvailable en modo AUTO es false si ambos no estan disponibles`() = runTest {
        router.currentMode = LlmMode.AUTO

        coEvery { embeddedAdapter.isAvailable() } returns false
        coEvery { serverAdapter.isAvailable() } returns false

        assertFalse(router.isAvailable())
    }

    @Test
    fun `cambio de modo en runtime aplica inmediatamente`() = runTest {
        router.currentMode = LlmMode.SERVER
        val response = LlmResponse(text = "server response")
        coEvery { serverAdapter.generate(any(), any()) } returns JulyResult.success(response)

        router.generate("test1", emptyList())
        coVerify { serverAdapter.generate(any(), any()) }

        // Cambiar a EMBEDDED
        router.currentMode = LlmMode.EMBEDDED
        val embResponse = LlmResponse(text = "embedded response")
        coEvery { embeddedAdapter.generate(any(), any()) } returns JulyResult.success(embResponse)

        val result = router.generate("test2", emptyList())

        assertEquals("embedded response", result.getOrNull()?.text)
        coVerify(exactly = 1) { serverAdapter.generate(any(), any()) }  // solo la primera llamada
    }
}
```

---

## RESUMEN DE CAMBIOS FASE 5 → FASE 6

| Componente | Cambio |
|---|---|
| `LlamaCppConfig` | NUEVO — configuración del motor llama.cpp |
| `LlamaCppJNI` | NUEVO — bridge JNI con código C++ de referencia |
| `LlamaCppLLMAdapter` | NUEVO — adapter completo con ensureLoaded + releaseContext |
| `LlmMode` | NUEVO — enum EMBEDDED / SERVER / AUTO |
| `LlmRouter` | NUEVO — orquesta estrategia primario/fallback |
| `ModelMemoryManager` | Añade soporte EngineType.LLM + llmMutex + llmReleaseCallback |
| `EngineType` | Añade LLM al enum |
| `SystemConfigDataStore` | Añade llmMode + llmModelPath |
| `LlmModule` | Reemplaza binding directo por LlmRouter |
| `EngineModule` | Eliminar binding LanguageModelEngine (pasa a LlmModule) |
| `AppSettings` | Añade llmMode |
| `SettingsViewModel` | Añade setLlmMode() |
| `SettingsScreen` | Selector de 3 chips: Auto / Embebido / Servidor |
| Scripts | download_llm_model.sh + setup_fase6.sh |
| Tests | LlamaCppLLMAdapterTest + LlmRouterTest |

---

## NOTAS CRÍTICAS

### Descarga de binarios llama.cpp para Android

Los binarios precompilados están en los releases de llama.cpp:
- https://github.com/ggerganov/llama.cpp/releases

Buscar archivos con nombre `llama-{version}-bin-android-*` o compilar con:
```bash
# Compilación cross-compile desde Linux/macOS
cmake -B build-android \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_TESTS=OFF
cmake --build build-android --config Release
```

### Consumo de RAM total en FASE 6

| Componente | RAM |
|---|---|
| Llama-3.2-3B Q4_K_M | ~2.0 GB |
| Whisper-small | ~0.5 GB |
| Piper-sharvard | ~0.3 GB |
| Proceso Android | ~0.2 GB |
| **Total foreground** | **~3.0 GB** |

Dispositivos mínimos recomendados: 6 GB RAM físicos.
En dispositivos de 4 GB: activar `ModelMode.MEMORY` y usar Llama-3.2-1B (~0.8 GB).

### Por qué `LlmRouter` en lugar de un switch en el orquestador

El orquestador no debe conocer la estrategia de selección de motor.
`LlmRouter` encapsula esa lógica completamente — si en FASE 7 se añade
un tercer motor (por ejemplo, un servidor remoto cifrado), el orquestador
no cambia. Solo se añade un caso en `LlmRouter`. El contrato
`LanguageModelEngine` permanece estable desde FASE 1.
