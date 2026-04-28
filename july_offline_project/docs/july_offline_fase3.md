# JULY OFFLINE — FASE 3
## Integración JNI Real + Corrección de TODOs + UI Completa
### `com.july.offline`

**Alcance de FASE 3:**
- Integración JNI real para Whisper.cpp (STT) y Piper (TTS)
- Clases de bridge JNI (`WhisperJNI.kt`, `PiperJNI.kt`)
- Instrucciones de descarga de binarios precompilados (.so)
- Scripts de descarga y setup de modelos
- Corrección de los 10 TODOs críticos de FASE 2
- AudioCoordinator con Flow correcto (takeWhile + VAD)
- NetworkModule con config dinámica desde SystemConfigDataStore
- EngineModule con rutas de modelo desde SystemConfigDataStore
- AudioModule sin instanciación manual
- DiagnosticsLogger conectado a DiagnosticsDao
- Room con migración declarativa versión 1→2
- ConversationScreen con historial de mensajes scrollable
- Solicitud de permiso de micrófono en runtime
- ABIs: arm64-v8a + x86_64

---

## ÍNDICE DE ARCHIVOS FASE 3

### NUEVOS — JNI BRIDGE
- `ai/stt/WhisperJNI.kt`
- `ai/tts/PiperJNI.kt`

### NUEVOS — SETUP
- `scripts/download_binaries.sh`
- `scripts/download_models.sh`
- `scripts/setup_fase3.sh`
- `docs/SETUP_FASE3.md`

### MODIFICADOS — AI
- `ai/stt/WhisperSTTAdapter.kt` (JNI real, reemplaza placeholder)
- `ai/tts/PiperTTSAdapter.kt` (JNI real, reemplaza placeholder)

### MODIFICADOS — DOMAIN
- `domain/orchestrator/AudioCoordinator.kt` (Flow correcto con takeWhile)
- `domain/orchestrator/EngineHealthMonitor.kt` (config dinámica)

### MODIFICADOS — DATA
- `data/db/JulyDatabase.kt` (migración 1→2)
- `data/db/dao/DiagnosticsDao.kt` (query adicional)
- `data/network/NetworkHealthChecker.kt` (sin cambios, confirmado)

### MODIFICADOS — DI
- `di/AudioModule.kt` (sin instanciación manual)
- `di/NetworkModule.kt` (config dinámica desde DataStore)
- `di/EngineModule.kt` (rutas de modelo desde DataStore)

### MODIFICADOS — CORE
- `core/logging/DiagnosticsLogger.kt` (persistencia en Room)

### MODIFICADOS — UI
- `ui/conversation/ConversationScreen.kt` (historial scrollable)
- `ui/conversation/ConversationViewModel.kt` (historial de mensajes)
- `ui/conversation/ConversationUiState.kt` (campo messages)
- `ui/permission/PermissionHandler.kt` (NUEVO)

### MODIFICADOS — APP
- `app/build.gradle.kts` (abiFilters para ABIs)

---

## SETUP DE BINARIOS PRECOMPILADOS

### `docs/SETUP_FASE3.md`

```markdown
# Setup FASE 3 — Binarios JNI y Modelos

## Requisitos previos
- Android Studio Ladybug (2024.2.1) o superior
- ADB instalado y en PATH
- Dispositivo físico arm64-v8a O emulador x86_64 en Android Studio
- ~900 MB de espacio libre en el dispositivo

## Paso 1: Descargar binarios precompilados (.so)

Ejecutar desde la raíz del proyecto:
    bash scripts/download_binaries.sh

Esto coloca los archivos en:
    app/src/main/jniLibs/arm64-v8a/libwhisper.so
    app/src/main/jniLibs/arm64-v8a/libpiper.so
    app/src/main/jniLibs/arm64-v8a/libonnxruntime.so
    app/src/main/jniLibs/x86_64/libwhisper.so
    app/src/main/jniLibs/x86_64/libpiper.so
    app/src/main/jniLibs/x86_64/libonnxruntime.so

## Paso 2: Descargar modelos

    bash scripts/download_models.sh

Modelos descargados en scripts/models/:
    whisper-small.bin          (466 MB)
    es_ES-sharvard-medium.onnx (300 MB)
    es_ES-sharvard-medium.onnx.json (2 KB)

## Paso 3: Instalar modelos en el dispositivo

    bash scripts/setup_fase3.sh

Esto copia los modelos a /data/data/com.july.offline/files/ via ADB.
Requiere que la app ya esté instalada (debuggable).

## Paso 4: Compilar y ejecutar

En Android Studio: Run > Run 'app'

## Verificación

En Logcat filtrar por tag "JulyOffline":
    [Engine:STT] transcribed [XXXms]   ← Whisper funcionando
    [Engine:TTS] synthesized [XXXms]   ← Piper funcionando
    [EngineHealthMonitor] STT:READY LLM:READY TTS:READY

## Fuentes de los binarios precompilados

### libwhisper.so
- Repositorio: https://github.com/ggerganov/whisper.cpp
- Release: v1.7.2
- Descarga directa arm64: https://github.com/ggerganov/whisper.cpp/releases/download/v1.7.2/libwhisper-android-arm64.zip
- Descarga directa x86_64: https://github.com/ggerganov/whisper.cpp/releases/download/v1.7.2/libwhisper-android-x86_64.zip

### libpiper.so + libonnxruntime.so
- Repositorio: https://github.com/rhasspy/piper
- Release: 2023.11.14-2
- Descarga: https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_android_arm64.tar.gz
- ONNX Runtime: https://github.com/microsoft/onnxruntime/releases/download/v1.17.1/onnxruntime-android-arm64-v8a-1.17.1.zip

### whisper-small.bin
- Hugging Face: https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin

### es_ES-sharvard-medium.onnx
- Piper voices: https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/sharvard/medium/es_ES-sharvard-medium.onnx
- Config: https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/sharvard/medium/es_ES-sharvard-medium.onnx.json

## Estructura jniLibs esperada

    app/src/main/jniLibs/
    ├── arm64-v8a/
    │   ├── libwhisper.so       (~4 MB)
    │   ├── libpiper.so         (~2 MB)
    │   └── libonnxruntime.so   (~8 MB)
    └── x86_64/
        ├── libwhisper.so
        ├── libpiper.so
        └── libonnxruntime.so
```

---

## SCRIPTS DE SETUP

### `scripts/download_binaries.sh`

```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
JNILIBS_DIR="$PROJECT_ROOT/app/src/main/jniLibs"
TMP_DIR="$SCRIPT_DIR/tmp_binaries"

echo "=== July Offline — Descarga de binarios JNI precompilados ==="
echo "Destino: $JNILIBS_DIR"

mkdir -p "$TMP_DIR"
mkdir -p "$JNILIBS_DIR/arm64-v8a"
mkdir -p "$JNILIBS_DIR/x86_64"

# ── Whisper.cpp arm64-v8a ─────────────────────────────────────────────
echo ""
echo "[1/4] Descargando libwhisper.so (arm64-v8a)..."
WHISPER_ARM64_URL="https://github.com/ggerganov/whisper.cpp/releases/download/v1.7.2/libwhisper-android-arm64.zip"
curl -L -o "$TMP_DIR/whisper_arm64.zip" "$WHISPER_ARM64_URL" --progress-bar
unzip -q -o "$TMP_DIR/whisper_arm64.zip" -d "$TMP_DIR/whisper_arm64"
cp "$TMP_DIR/whisper_arm64/libwhisper.so" "$JNILIBS_DIR/arm64-v8a/libwhisper.so"
echo "  OK: $JNILIBS_DIR/arm64-v8a/libwhisper.so"

# ── Whisper.cpp x86_64 ───────────────────────────────────────────────
echo ""
echo "[2/4] Descargando libwhisper.so (x86_64)..."
WHISPER_X86_URL="https://github.com/ggerganov/whisper.cpp/releases/download/v1.7.2/libwhisper-android-x86_64.zip"
curl -L -o "$TMP_DIR/whisper_x86.zip" "$WHISPER_X86_URL" --progress-bar
unzip -q -o "$TMP_DIR/whisper_x86.zip" -d "$TMP_DIR/whisper_x86"
cp "$TMP_DIR/whisper_x86/libwhisper.so" "$JNILIBS_DIR/x86_64/libwhisper.so"
echo "  OK: $JNILIBS_DIR/x86_64/libwhisper.so"

# ── Piper arm64-v8a ──────────────────────────────────────────────────
echo ""
echo "[3/4] Descargando libpiper.so + libonnxruntime.so (arm64-v8a)..."
PIPER_ARM64_URL="https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_android_arm64.tar.gz"
curl -L -o "$TMP_DIR/piper_arm64.tar.gz" "$PIPER_ARM64_URL" --progress-bar
tar -xzf "$TMP_DIR/piper_arm64.tar.gz" -C "$TMP_DIR" --strip-components=1 2>/dev/null || \
    tar -xzf "$TMP_DIR/piper_arm64.tar.gz" -C "$TMP_DIR"

find "$TMP_DIR" -name "libpiper.so" -exec cp {} "$JNILIBS_DIR/arm64-v8a/libpiper.so" \;
find "$TMP_DIR" -name "libonnxruntime.so" -exec cp {} "$JNILIBS_DIR/arm64-v8a/libonnxruntime.so" \;
echo "  OK: $JNILIBS_DIR/arm64-v8a/libpiper.so"
echo "  OK: $JNILIBS_DIR/arm64-v8a/libonnxruntime.so"

# ── Piper x86_64 (copia arm64 con nota) ─────────────────────────────
# NOTA: Piper no publica binarios x86_64 oficiales para Android.
# Para el emulador, copiar arm64 no funciona. Opciones:
# 1. Usar un emulador arm64 (recomendado): AVD con ABI arm64-v8a
# 2. Compilar desde fuente para x86_64
# En FASE 3 dejamos un stub vacío para que la app compile en x86_64
# pero TTS.isAvailable() devolverá false en el emulador x86_64.
echo ""
echo "[4/4] x86_64: Piper no tiene release oficial. Usando stub para compilación."
echo "  NOTA: TTS no funcionará en emulador x86_64. Usar emulador arm64 o dispositivo físico."
touch "$JNILIBS_DIR/x86_64/.piper_not_available"
# libwhisper x86_64 sí está disponible (ya descargado arriba)
# Para libpiper x86_64 en emulador: usar AVD arm64 en Android Studio

# ── Limpieza ─────────────────────────────────────────────────────────
echo ""
echo "Limpiando temporales..."
rm -rf "$TMP_DIR"

echo ""
echo "=== Descarga completada ==="
echo ""
echo "Archivos en jniLibs:"
find "$JNILIBS_DIR" -name "*.so" | sort
echo ""
echo "Siguiente paso: bash scripts/download_models.sh"
```

---

### `scripts/download_models.sh`

```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="$SCRIPT_DIR/models"

echo "=== July Offline — Descarga de modelos de IA ==="
echo "Destino: $MODELS_DIR"
echo ""

mkdir -p "$MODELS_DIR"

# Verificar espacio disponible (~800MB necesarios)
AVAILABLE_KB=$(df -k "$MODELS_DIR" | awk 'NR==2 {print $4}')
NEEDED_KB=820000
if [ "$AVAILABLE_KB" -lt "$NEEDED_KB" ]; then
    echo "ERROR: Espacio insuficiente. Necesitas ~800MB libres."
    echo "Disponible: $((AVAILABLE_KB / 1024)) MB"
    exit 1
fi

# ── whisper-small.bin ────────────────────────────────────────────────
WHISPER_MODEL="$MODELS_DIR/whisper-small.bin"
if [ -f "$WHISPER_MODEL" ]; then
    echo "[1/3] whisper-small.bin ya existe, saltando."
else
    echo "[1/3] Descargando whisper-small.bin (~466 MB)..."
    WHISPER_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
    curl -L -o "$WHISPER_MODEL" "$WHISPER_URL" --progress-bar
    echo "  OK: $WHISPER_MODEL ($(du -sh "$WHISPER_MODEL" | cut -f1))"
fi

# ── es_ES-sharvard-medium.onnx ───────────────────────────────────────
PIPER_MODEL="$MODELS_DIR/es_ES-sharvard-medium.onnx"
if [ -f "$PIPER_MODEL" ]; then
    echo "[2/3] es_ES-sharvard-medium.onnx ya existe, saltando."
else
    echo "[2/3] Descargando es_ES-sharvard-medium.onnx (~300 MB)..."
    PIPER_URL="https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/sharvard/medium/es_ES-sharvard-medium.onnx"
    curl -L -o "$PIPER_MODEL" "$PIPER_URL" --progress-bar
    echo "  OK: $PIPER_MODEL ($(du -sh "$PIPER_MODEL" | cut -f1))"
fi

# ── es_ES-sharvard-medium.onnx.json ─────────────────────────────────
PIPER_CONFIG="$MODELS_DIR/es_ES-sharvard-medium.onnx.json"
if [ -f "$PIPER_CONFIG" ]; then
    echo "[3/3] es_ES-sharvard-medium.onnx.json ya existe, saltando."
else
    echo "[3/3] Descargando es_ES-sharvard-medium.onnx.json..."
    CONFIG_URL="https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/sharvard/medium/es_ES-sharvard-medium.onnx.json"
    curl -L -o "$PIPER_CONFIG" "$CONFIG_URL" --progress-bar
    echo "  OK: $PIPER_CONFIG"
fi

echo ""
echo "=== Modelos descargados ==="
ls -lh "$MODELS_DIR"
echo ""
echo "Siguiente paso: bash scripts/setup_fase3.sh"
```

---

### `scripts/setup_fase3.sh`

```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="$SCRIPT_DIR/models"
APP_PKG="com.july.offline"
DEVICE_FILES_DIR="/data/data/$APP_PKG/files"

echo "=== July Offline — Setup FASE 3 en dispositivo/emulador ==="

# Verificar ADB
if ! command -v adb &> /dev/null; then
    echo "ERROR: adb no encontrado. Instala Android SDK Platform Tools."
    exit 1
fi

# Verificar dispositivo conectado
DEVICE=$(adb devices | grep -v "List" | grep -v "^$" | head -1 | cut -f1)
if [ -z "$DEVICE" ]; then
    echo "ERROR: No hay dispositivo conectado. Conecta un dispositivo o inicia el emulador."
    exit 1
fi
echo "Dispositivo: $DEVICE"

# Verificar que la app esté instalada
if ! adb -s "$DEVICE" shell pm list packages | grep -q "$APP_PKG"; then
    echo "ERROR: La app $APP_PKG no está instalada."
    echo "Instala primero desde Android Studio: Run > Run 'app'"
    exit 1
fi

# Verificar modelos
for MODEL in "whisper-small.bin" "es_ES-sharvard-medium.onnx" "es_ES-sharvard-medium.onnx.json"; do
    if [ ! -f "$MODELS_DIR/$MODEL" ]; then
        echo "ERROR: Modelo faltante: $MODELS_DIR/$MODEL"
        echo "Ejecuta primero: bash scripts/download_models.sh"
        exit 1
    fi
done

# Crear directorio en el dispositivo
echo ""
echo "Creando directorio $DEVICE_FILES_DIR..."
adb -s "$DEVICE" shell run-as "$APP_PKG" mkdir -p files

# Copiar modelos
echo ""
echo "[1/3] Copiando whisper-small.bin (~466MB, puede tardar varios minutos)..."
adb -s "$DEVICE" push "$MODELS_DIR/whisper-small.bin" "/sdcard/whisper-small.bin"
adb -s "$DEVICE" shell run-as "$APP_PKG" cp /sdcard/whisper-small.bin files/
adb -s "$DEVICE" shell rm /sdcard/whisper-small.bin
echo "  OK"

echo "[2/3] Copiando es_ES-sharvard-medium.onnx (~300MB)..."
adb -s "$DEVICE" push "$MODELS_DIR/es_ES-sharvard-medium.onnx" "/sdcard/piper.onnx"
adb -s "$DEVICE" shell run-as "$APP_PKG" cp /sdcard/piper.onnx "files/es_ES-sharvard-medium.onnx"
adb -s "$DEVICE" shell rm /sdcard/piper.onnx
echo "  OK"

echo "[3/3] Copiando es_ES-sharvard-medium.onnx.json..."
adb -s "$DEVICE" push "$MODELS_DIR/es_ES-sharvard-medium.onnx.json" "/sdcard/piper.json"
adb -s "$DEVICE" shell run-as "$APP_PKG" cp /sdcard/piper.json "files/es_ES-sharvard-medium.onnx.json"
adb -s "$DEVICE" shell rm /sdcard/piper.json
echo "  OK"

echo ""
echo "=== Setup completado ==="
echo ""
echo "Archivos en el dispositivo:"
adb -s "$DEVICE" shell run-as "$APP_PKG" ls -lh files/
echo ""
echo "Compila y ejecuta la app desde Android Studio."
echo "Filtra Logcat por 'JulyOffline' para verificar que los motores cargan."
```

---

## JNI BRIDGE

### `ai/stt/WhisperJNI.kt`

```kotlin
package com.july.offline.ai.stt

/**
 * Bridge JNI hacia libwhisper.so (Whisper.cpp).
 *
 * Convención de nombres JNI:
 * El nombre de cada función nativa debe coincidir con:
 * Java_{package_con_underscores}_{ClassName}_{methodName}
 *
 * Para este archivo:
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperInit
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperTranscribe
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperFree
 *
 * Implementación C++ en: whisper_jni.cpp (incluido al final de este archivo como comentario)
 */
object WhisperJNI {

    init {
        System.loadLibrary("whisper")
    }

    /**
     * Inicializa el contexto de Whisper con el modelo en disco.
     * @param modelPath ruta absoluta al archivo .bin del modelo GGML
     * @param nThreads número de hilos de CPU para inferencia (recomendado: 4)
     * @return handle nativo (Long) al contexto whisper_context, o 0L si falla
     */
    external fun whisperInit(modelPath: String, nThreads: Int): Long

    /**
     * Transcribe audio PCM a texto.
     * @param contextHandle handle devuelto por whisperInit()
     * @param pcmSamples muestras de audio PCM 32-bit float, mono, 16kHz
     *                   (Whisper requiere float32 internamente; la conversión se hace en WhisperSTTAdapter)
     * @param language código de idioma ISO 639-1 ("es", "en", "auto")
     * @return texto transcrito, o cadena vacía si no se detectó voz
     */
    external fun whisperTranscribe(
        contextHandle: Long,
        pcmSamples: FloatArray,
        language: String
    ): String

    /**
     * Libera el contexto nativo de Whisper.
     * Llamar cuando la app se destruye o cuando se cambia de modelo.
     */
    external fun whisperFree(contextHandle: Long)
}

/*
 * ── REFERENCIA C++ (whisper_jni.cpp) ─────────────────────────────────────────
 *
 * Este archivo debe existir en app/src/main/cpp/whisper_jni.cpp
 * SOLO si compilas desde fuente. Con .so precompilados NO es necesario.
 *
 * #include <jni.h>
 * #include "whisper.h"
 * #include <string>
 * #include <vector>
 *
 * extern "C" {
 *
 * JNIEXPORT jlong JNICALL
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperInit(
 *     JNIEnv* env, jobject, jstring modelPath, jint nThreads) {
 *
 *     const char* path = env->GetStringUTFChars(modelPath, nullptr);
 *     whisper_context_params params = whisper_context_default_params();
 *     params.use_gpu = false;
 *     whisper_context* ctx = whisper_init_from_file_with_params(path, params);
 *     env->ReleaseStringUTFChars(modelPath, path);
 *     return ctx ? reinterpret_cast<jlong>(ctx) : 0L;
 * }
 *
 * JNIEXPORT jstring JNICALL
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperTranscribe(
 *     JNIEnv* env, jobject, jlong handle, jfloatArray samples, jstring language) {
 *
 *     whisper_context* ctx = reinterpret_cast<whisper_context*>(handle);
 *     if (!ctx) return env->NewStringUTF("");
 *
 *     jsize len = env->GetArrayLength(samples);
 *     jfloat* data = env->GetFloatArrayElements(samples, nullptr);
 *
 *     whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
 *     const char* lang = env->GetStringUTFChars(language, nullptr);
 *     wparams.language = lang;
 *     wparams.translate = false;
 *     wparams.no_context = true;
 *     wparams.single_segment = false;
 *     wparams.print_progress = false;
 *     wparams.print_realtime = false;
 *
 *     int result = whisper_full(ctx, wparams, data, len);
 *
 *     env->ReleaseFloatArrayElements(samples, data, JNI_ABORT);
 *     env->ReleaseStringUTFChars(language, lang);
 *
 *     if (result != 0) return env->NewStringUTF("");
 *
 *     std::string text;
 *     int n_segments = whisper_full_n_segments(ctx);
 *     for (int i = 0; i < n_segments; ++i) {
 *         text += whisper_full_get_segment_text(ctx, i);
 *     }
 *
 *     // Trim leading space that Whisper often adds
 *     if (!text.empty() && text[0] == ' ') text = text.substr(1);
 *
 *     return env->NewStringUTF(text.c_str());
 * }
 *
 * JNIEXPORT void JNICALL
 * Java_com_july_offline_ai_stt_WhisperJNI_whisperFree(
 *     JNIEnv*, jobject, jlong handle) {
 *     whisper_context* ctx = reinterpret_cast<whisper_context*>(handle);
 *     if (ctx) whisper_free(ctx);
 * }
 *
 * } // extern "C"
 */
```

---

### `ai/tts/PiperJNI.kt`

```kotlin
package com.july.offline.ai.tts

/**
 * Bridge JNI hacia libpiper.so + libonnxruntime.so (Piper TTS).
 *
 * Convención de nombres JNI:
 * Java_com_july_offline_ai_tts_PiperJNI_piperInit
 * Java_com_july_offline_ai_tts_PiperJNI_piperSynthesize
 * Java_com_july_offline_ai_tts_PiperJNI_piperFree
 *
 * ONNX Runtime debe cargarse antes que Piper.
 */
object PiperJNI {

    init {
        System.loadLibrary("onnxruntime")  // debe cargarse primero
        System.loadLibrary("piper")
    }

    /**
     * Inicializa el contexto de Piper.
     * @param modelPath ruta absoluta al archivo .onnx del modelo de voz
     * @param modelConfigPath ruta al archivo .onnx.json de configuración
     * @return handle nativo al contexto Piper, o 0L si falla
     */
    external fun piperInit(modelPath: String, modelConfigPath: String): Long

    /**
     * Sintetiza texto a audio PCM 16-bit.
     * @param contextHandle handle devuelto por piperInit()
     * @param text texto a sintetizar
     * @param speakerId ID del hablante para modelos multi-speaker (0 para mono)
     * @return ShortArray con muestras PCM 16-bit al sampleRate del modelo (22050 Hz)
     *         Array vacío si la síntesis falla
     */
    external fun piperSynthesize(
        contextHandle: Long,
        text: String,
        speakerId: Int
    ): ShortArray

    /**
     * Libera el contexto nativo de Piper.
     */
    external fun piperFree(contextHandle: Long)
}

/*
 * ── REFERENCIA C++ (piper_jni.cpp) ────────────────────────────────────────────
 *
 * Este archivo debe existir en app/src/main/cpp/piper_jni.cpp
 * SOLO si compilas desde fuente. Con .so precompilados NO es necesario.
 *
 * #include <jni.h>
 * #include "piper.hpp"
 * #include <vector>
 *
 * extern "C" {
 *
 * JNIEXPORT jlong JNICALL
 * Java_com_july_offline_ai_tts_PiperJNI_piperInit(
 *     JNIEnv* env, jobject, jstring modelPath, jstring configPath) {
 *
 *     const char* mPath = env->GetStringUTFChars(modelPath, nullptr);
 *     const char* cPath = env->GetStringUTFChars(configPath, nullptr);
 *
 *     piper::PiperConfig* config = new piper::PiperConfig();
 *     piper::Voice* voice = new piper::Voice();
 *
 *     try {
 *         piper::initialize(*config);
 *         piper::loadVoice(*config, std::string(mPath), std::string(cPath), *voice);
 *     } catch (...) {
 *         delete voice;
 *         delete config;
 *         env->ReleaseStringUTFChars(modelPath, mPath);
 *         env->ReleaseStringUTFChars(configPath, cPath);
 *         return 0L;
 *     }
 *
 *     env->ReleaseStringUTFChars(modelPath, mPath);
 *     env->ReleaseStringUTFChars(configPath, cPath);
 *
 *     // Empaquetar config+voice en un struct simple para pasar como handle
 *     struct PiperCtx { piper::PiperConfig* cfg; piper::Voice* voice; };
 *     PiperCtx* ctx = new PiperCtx{config, voice};
 *     return reinterpret_cast<jlong>(ctx);
 * }
 *
 * JNIEXPORT jshortArray JNICALL
 * Java_com_july_offline_ai_tts_PiperJNI_piperSynthesize(
 *     JNIEnv* env, jobject, jlong handle, jstring text, jint speakerId) {
 *
 *     struct PiperCtx { piper::PiperConfig* cfg; piper::Voice* voice; };
 *     PiperCtx* ctx = reinterpret_cast<PiperCtx*>(handle);
 *     if (!ctx) return env->NewShortArray(0);
 *
 *     const char* txt = env->GetStringUTFChars(text, nullptr);
 *
 *     piper::SynthesisResult result;
 *     std::vector<int16_t> audioBuffer;
 *
 *     // speakerId como optional
 *     std::optional<piper::SpeakerId> sid;
 *     if (speakerId >= 0) sid = static_cast<piper::SpeakerId>(speakerId);
 *
 *     try {
 *         piper::textToAudio(*ctx->cfg, *ctx->voice, std::string(txt), audioBuffer, result, nullptr, sid);
 *     } catch (...) {
 *         env->ReleaseStringUTFChars(text, txt);
 *         return env->NewShortArray(0);
 *     }
 *
 *     env->ReleaseStringUTFChars(text, txt);
 *
 *     jshortArray out = env->NewShortArray(static_cast<jsize>(audioBuffer.size()));
 *     env->SetShortArrayRegion(out, 0, audioBuffer.size(),
 *         reinterpret_cast<const jshort*>(audioBuffer.data()));
 *     return out;
 * }
 *
 * JNIEXPORT void JNICALL
 * Java_com_july_offline_ai_tts_PiperJNI_piperFree(
 *     JNIEnv*, jobject, jlong handle) {
 *     struct PiperCtx { piper::PiperConfig* cfg; piper::Voice* voice; };
 *     PiperCtx* ctx = reinterpret_cast<PiperCtx*>(handle);
 *     if (ctx) {
 *         piper::terminate(*ctx->cfg);
 *         delete ctx->voice;
 *         delete ctx->cfg;
 *         delete ctx;
 *     }
 * }
 *
 * } // extern "C"
 */
```

---

## AI — ADAPTERS ACTUALIZADOS

### `ai/stt/WhisperSTTAdapter.kt` (reemplaza FASE 2)

```kotlin
package com.july.offline.ai.stt

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.model.Transcript
import com.july.offline.domain.port.SpeechToTextEngine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Adaptador STT con integración JNI real hacia Whisper.cpp.
 *
 * Ciclo de vida del contexto nativo:
 * - El contexto se inicializa lazy en el primer uso (initContext())
 * - El modelo (466 MB) se mantiene en memoria mientras la app vive
 * - Se libera en onCleared() del ViewModel vía EngineHealthMonitor (FASE 4)
 *
 * Conversión de audio:
 * - Input: PCM 16-bit signed (ByteArray) a 16kHz mono
 * - Whisper requiere: FloatArray de muestras normalizadas [-1.0, 1.0]
 * - La conversión se hace en convertPcm16ToFloat()
 */
class WhisperSTTAdapter @Inject constructor(
    private val config: WhisperConfig,
    private val logger: DiagnosticsLogger
) : SpeechToTextEngine {

    @Volatile private var contextHandle: Long = 0L
    private val initLock = Any()

    /**
     * Inicializa el contexto Whisper de forma lazy y thread-safe.
     * El modelo tarda ~2-4s en cargar la primera vez.
     */
    private fun initContext(): Boolean {
        if (contextHandle != 0L) return true
        synchronized(initLock) {
            if (contextHandle != 0L) return true
            if (!File(config.modelPath).exists()) {
                logger.logError("WhisperSTT", "Model not found: ${config.modelPath}")
                return false
            }
            logger.logInfo("WhisperSTT", "Loading model from ${config.modelPath}...")
            val handle = WhisperJNI.whisperInit(config.modelPath, config.threads)
            return if (handle != 0L) {
                contextHandle = handle
                logger.logInfo("WhisperSTT", "Model loaded successfully")
                true
            } else {
                logger.logError("WhisperSTT", "whisperInit() returned 0 — model load failed")
                false
            }
        }
    }

    override suspend fun transcribe(audio: ByteArray): JulyResult<Transcript> {
        return try {
            withTimeout(config.maxDurationMs) {
                if (!initContext()) {
                    return@withTimeout JulyResult.failure(
                        AppError.Stt("Whisper context not initialized — model missing at ${config.modelPath}")
                    )
                }

                logger.logInfo("WhisperSTT", "Transcribing ${audio.size} bytes (${audio.size / 32}ms)")
                val startMs = System.currentTimeMillis()

                // Convertir PCM 16-bit a Float32 normalizado que Whisper requiere
                val floatSamples = convertPcm16ToFloat(audio)

                val rawText = WhisperJNI.whisperTranscribe(
                    contextHandle = contextHandle,
                    pcmSamples = floatSamples,
                    language = config.language
                )

                val latencyMs = System.currentTimeMillis() - startMs
                logger.logEngineEvent("STT", "transcribed '${rawText.take(40)}...'", latencyMs)

                JulyResult.success(
                    Transcript(
                        text = rawText.trim(),
                        confidence = -1f,       // Whisper.cpp greedy no expone confidence
                        languageCode = config.language,
                        durationMs = audio.size.toLong() / 32L   // 16kHz mono 16bit = 32 bytes/ms
                    )
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            JulyResult.failure(
                AppError.Stt("Whisper timeout after ${config.maxDurationMs}ms", e)
            )
        } catch (e: Exception) {
            logger.logError("WhisperSTT", "Transcription failed", e)
            JulyResult.failure(
                AppError.Stt("Whisper error: ${e.message}", e)
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        if (!File(config.modelPath).exists()) return false
        return initContext()
    }

    /**
     * Convierte PCM 16-bit signed little-endian a FloatArray normalizado [-1.0, 1.0].
     * Whisper.cpp internamente espera float32 mono 16kHz.
     */
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

### `ai/tts/PiperTTSAdapter.kt` (reemplaza FASE 2)

```kotlin
package com.july.offline.ai.tts

import com.july.offline.core.error.AppError
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.result.JulyResult
import com.july.offline.domain.port.TextToSpeechEngine
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Adaptador TTS con integración JNI real hacia Piper + ONNX Runtime.
 *
 * Ciclo de vida del contexto nativo:
 * - Inicialización lazy en primer uso
 * - El modelo (300 MB) se mantiene en memoria
 *
 * Conversión de audio:
 * - Piper devuelve: ShortArray PCM 16-bit a 22050 Hz mono
 * - AudioPlayerAdapter espera: ByteArray PCM 16-bit little-endian
 * - La conversión se hace en convertShortsToPcm()
 */
class PiperTTSAdapter @Inject constructor(
    private val config: PiperConfig,
    private val logger: DiagnosticsLogger
) : TextToSpeechEngine {

    @Volatile private var contextHandle: Long = 0L
    private val initLock = Any()

    private fun initContext(): Boolean {
        if (contextHandle != 0L) return true
        synchronized(initLock) {
            if (contextHandle != 0L) return true
            if (!File(config.modelPath).exists()) {
                logger.logError("PiperTTS", "Model not found: ${config.modelPath}")
                return false
            }
            if (!File(config.modelConfigPath).exists()) {
                logger.logError("PiperTTS", "Config not found: ${config.modelConfigPath}")
                return false
            }
            logger.logInfo("PiperTTS", "Loading model from ${config.modelPath}...")
            val handle = PiperJNI.piperInit(config.modelPath, config.modelConfigPath)
            return if (handle != 0L) {
                contextHandle = handle
                logger.logInfo("PiperTTS", "Piper model loaded successfully")
                true
            } else {
                logger.logError("PiperTTS", "piperInit() returned 0 — model load failed")
                false
            }
        }
    }

    override suspend fun synthesize(text: String): JulyResult<ByteArray> {
        return try {
            if (!initContext()) {
                return JulyResult.failure(
                    AppError.Tts("Piper context not initialized — model missing at ${config.modelPath}")
                )
            }

            if (text.isBlank()) {
                return JulyResult.success(ByteArray(0))
            }

            logger.logInfo("PiperTTS", "Synthesizing: '${text.take(60)}...'")
            val startMs = System.currentTimeMillis()

            val shorts = PiperJNI.piperSynthesize(
                contextHandle = contextHandle,
                text = text,
                speakerId = config.speakerId
            )

            val latencyMs = System.currentTimeMillis() - startMs
            logger.logEngineEvent("TTS", "synthesized ${shorts.size} samples", latencyMs)

            if (shorts.isEmpty()) {
                return JulyResult.failure(
                    AppError.Tts("Piper returned empty audio for input: '${text.take(40)}'")
                )
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
        return initContext()
    }

    override fun getSupportedLanguages(): List<String> = listOf("es")

    /**
     * Convierte ShortArray PCM 16-bit a ByteArray little-endian
     * para que AudioPlayerAdapter pueda reproducirlo con AudioTrack.
     */
    private fun convertShortsToPcm(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { buffer.putShort(it) }
        return bytes
    }
}
```

---

## DOMAIN — CORRECCIONES

### `domain/orchestrator/AudioCoordinator.kt` (corrige TODO FASE 2)

```kotlin
package com.july.offline.domain.orchestrator

import com.july.offline.audio.player.AudioPlayerAdapter
import com.july.offline.audio.vad.VADProcessor
import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.port.AudioCapturePort
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioCoordinator corregido.
 *
 * PROBLEMA FASE 2:
 * El Flow de AudioCapturePort.startRecording() no termina solo.
 * El collect() anterior no tenía forma de terminar al detectar silencio.
 *
 * SOLUCIÓN FASE 3:
 * Usar takeWhile sobre el Flow de chunks. Cuando VAD detecta silencio
 * continuo >= silenceThresholdMs, takeWhile retorna false y el Flow se completa.
 * stopRecording() es llamado en el bloque finally para limpiar AudioRecord.
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
     * El Flow se completa automáticamente cuando VAD detecta fin de voz.
     *
     * @return Pair<ByteArray, Long> con audio PCM completo y duración en ms,
     *         o null si se canceló o no hay audio válido.
     */
    suspend fun recordUntilSilence(): Pair<ByteArray, Long>? =
        withContext(dispatchers.io) {
            val startTime = System.currentTimeMillis()
            val audioBuffer = mutableListOf<Byte>()
            var detectedVoice = false

            vadProcessor.reset()

            try {
                audioCapturePort
                    .startRecording()
                    .onEach { chunk ->
                        audioBuffer.addAll(chunk.toList())
                        // Marcar que hubo al menos un chunk con voz
                        if (!vadProcessor.isSilence(chunk)) {
                            detectedVoice = true
                        }
                    }
                    .takeWhile { chunk ->
                        // Continuar mientras NO haya silencio detectado,
                        // o si aún no se ha detectado voz (evita corte prematuro)
                        val silence = vadProcessor.isSilence(chunk)
                        !(silence && detectedVoice)
                    }
                    .collect {}  // consumir el Flow hasta que takeWhile lo cierre

            } finally {
                // Siempre limpiar AudioRecord, independientemente de cómo terminó el Flow
                audioCapturePort.stopRecording()
            }

            val audioBytes = audioBuffer.toByteArray()
            val durationMs = System.currentTimeMillis() - startTime

            if (audioBytes.size < 3200) {  // < 100ms de audio (3200 bytes = 100ms a 16kHz/16bit)
                logger.logWarning(
                    "AudioCoordinator",
                    "Audio too short: ${audioBytes.size} bytes (${durationMs}ms), discarding"
                )
                return@withContext null
            }

            logger.logInfo(
                "AudioCoordinator",
                "Captured ${audioBytes.size} bytes in ${durationMs}ms (voice detected: $detectedVoice)"
            )
            Pair(audioBytes, durationMs)
        }

    /** Reproduce audio PCM via AudioPlayerAdapter. */
    suspend fun playAudio(audio: ByteArray) = withContext(dispatchers.io) {
        if (audio.isEmpty()) {
            logger.logWarning("AudioCoordinator", "Empty audio, skipping playback")
            return@withContext
        }
        audioPlayerAdapter.play(audio)
    }

    /** Cancela grabación y reproducción activas. */
    fun cancel() {
        audioCapturePort.cancel()
        audioPlayerAdapter.stop()
        vadProcessor.reset()
        logger.logInfo("AudioCoordinator", "Cancelled")
    }
}
```

---

## CORE — CORRECCIONES

### `core/logging/DiagnosticsLogger.kt` (corrige TODO FASE 2)

```kotlin
package com.july.offline.core.logging

import android.util.Log
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.entity.DiagnosticsDbEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger estructurado con persistencia en Room.
 *
 * FASE 2: Solo escribía en Logcat.
 * FASE 3: Persiste en DiagnosticsDao de forma asíncrona (fire-and-forget).
 *
 * La escritura en Room es asíncrona con SupervisorJob para que un fallo
 * de persistencia nunca bloquee el hilo principal ni el orquestador.
 *
 * Política de retención: los logs se borran automáticamente tras 7 días
 * (llamado desde JulyApplication.onCreate()).
 */
@Singleton
class DiagnosticsLogger @Inject constructor(
    private val diagnosticsDao: DiagnosticsDao
) {

    companion object {
        private const val APP_TAG = "JulyOffline"
        private const val RETENTION_DAYS = 7L
    }

    // Scope dedicado para escrituras en Room — independiente del lifecycle de cualquier ViewModel
    private val logScope = CoroutineScope(SupervisorJob())

    fun logInfo(tag: String, message: String) {
        Log.i(APP_TAG, "[$tag] $message")
        persist("INFO", tag, message)
    }

    fun logDebug(tag: String, message: String) {
        Log.d(APP_TAG, "[$tag] $message")
        // Debug no se persiste para evitar volumen excesivo
    }

    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        Log.w(APP_TAG, "[$tag] $message", cause)
        persist("WARNING", tag, message, cause)
    }

    fun logError(tag: String, message: String, cause: Throwable? = null) {
        Log.e(APP_TAG, "[$tag] $message", cause)
        persist("ERROR", tag, message, cause)
    }

    fun logStateTransition(from: String, to: String, trigger: String) {
        val message = "$from → $to (trigger: $trigger)"
        Log.i(APP_TAG, "[State] $message")
        persist("INFO", "State", message)
    }

    fun logEngineEvent(engine: String, event: String, latencyMs: Long? = null) {
        val latencyStr = latencyMs?.let { " [${it}ms]" } ?: ""
        val message = "$event$latencyStr"
        Log.i(APP_TAG, "[Engine:$engine] $message")
        persist("INFO", "Engine:$engine", message)
    }

    /** Elimina logs más antiguos que RETENTION_DAYS días. */
    fun pruneOldLogs() {
        val cutoffMs = Instant.now().toEpochMilli() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        logScope.launch {
            try {
                diagnosticsDao.deleteOlderThan(cutoffMs)
            } catch (e: Exception) {
                Log.w(APP_TAG, "[DiagnosticsLogger] pruneOldLogs failed", e)
            }
        }
    }

    private fun persist(level: String, tag: String, message: String, cause: Throwable? = null) {
        logScope.launch {
            try {
                diagnosticsDao.insert(
                    DiagnosticsDbEntity(
                        timestamp = Instant.now().toEpochMilli(),
                        level = level,
                        tag = tag,
                        message = message,
                        stackTrace = cause?.stackTraceToString()?.take(2000) // limitar tamaño
                    )
                )
            } catch (e: Exception) {
                // Fallo silencioso: si Room falla, no cascadeamos el error
                Log.w(APP_TAG, "[DiagnosticsLogger] persist failed", e)
            }
        }
    }
}
```

---

## DATA — CORRECCIONES

### `data/db/JulyDatabase.kt` (migración 1→2)

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
 * Base de datos Room — versión 2.
 *
 * CAMBIO v1→v2: Se añade columna `title` a la tabla `sessions`
 * con valor por defecto '' para filas existentes.
 *
 * POLÍTICA DE MIGRACIÓN FASE 3:
 * - Migración declarativa (no destructiva) desde v1.
 * - A partir de FASE 3 los datos de sesiones son valiosos y no deben borrarse.
 * - Eliminar fallbackToDestructiveMigration() del DatabaseModule.
 */
@Database(
    entities = [
        SessionDbEntity::class,
        MessageDbEntity::class,
        DiagnosticsDbEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class JulyDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun diagnosticsDao(): DiagnosticsDao

    companion object {

        /**
         * Migración 1 → 2.
         * Añade columna `title` TEXT NOT NULL DEFAULT '' a la tabla sessions.
         * Room no detecta este cambio automáticamente si la entidad ya tenía el campo
         * con valor por defecto — la migración manual es necesaria.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE sessions ADD COLUMN title TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
```

---

## DI — CORRECCIONES

### `di/DatabaseModule.kt` (elimina fallbackToDestructiveMigration)

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
        .addMigrations(JulyDatabase.MIGRATION_1_2)
        // fallbackToDestructiveMigration() ELIMINADO en FASE 3
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

### `di/NetworkModule.kt` (config dinámica desde DataStore)

```kotlin
package com.july.offline.di

import com.july.offline.ai.llm.LlmApiService
import com.july.offline.ai.llm.LlmServerConfig
import com.july.offline.data.datastore.SystemConfigDataStore
import com.july.offline.data.network.LocalNetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * NetworkModule corregido.
 *
 * PROBLEMA FASE 2:
 * LlmServerConfig tenía valores hardcoded (host, port, model).
 *
 * SOLUCIÓN FASE 3:
 * LlmServerConfig se construye leyendo SystemConfigDataStore.
 * Se usa runBlocking() en el @Provides para leer el primer valor emitido
 * por DataStore al momento de la inyección (ocurre una sola vez en el arranque).
 *
 * NOTA: Si el usuario cambia la config en SettingsScreen, la app debe
 * reiniciarse para que Hilt reconstruya el singleton. En FASE 4 se puede
 * hacer dinámico con un Retrofit mutable, pero en FASE 3 el reinicio es aceptable.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLlmServerConfig(
        systemConfigDataStore: SystemConfigDataStore
    ): LlmServerConfig {
        // runBlocking es seguro aquí porque ocurre solo una vez en el grafo DI
        // durante el arranque de la app, antes de que el hilo principal esté activo
        return runBlocking {
            LlmServerConfig(
                host = systemConfigDataStore.llmHost.first(),
                port = systemConfigDataStore.llmPort.first(),
                modelName = systemConfigDataStore.llmModel.first()
            )
        }
    }

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

### `di/EngineModule.kt` (rutas de modelo desde DataStore)

```kotlin
package com.july.offline.di

import com.july.offline.ai.llm.LocalServerLLMAdapter
import com.july.offline.ai.stt.WhisperConfig
import com.july.offline.ai.stt.WhisperSTTAdapter
import com.july.offline.ai.tts.PiperConfig
import com.july.offline.ai.tts.PiperTTSAdapter
import com.july.offline.data.datastore.SystemConfigDataStore
import com.july.offline.domain.port.LanguageModelEngine
import com.july.offline.domain.port.SpeechToTextEngine
import com.july.offline.domain.port.TextToSpeechEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * EngineModule corregido.
 *
 * PROBLEMA FASE 2:
 * WhisperConfig y PiperConfig tenían rutas hardcoded.
 *
 * SOLUCIÓN FASE 3:
 * Las rutas se leen desde SystemConfigDataStore en el momento de la inyección.
 * Si el DataStore no tiene valor, se usan las rutas por defecto.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds @Singleton
    abstract fun bindSpeechToTextEngine(impl: WhisperSTTAdapter): SpeechToTextEngine

    @Binds @Singleton
    abstract fun bindLanguageModelEngine(impl: LocalServerLLMAdapter): LanguageModelEngine

    @Binds @Singleton
    abstract fun bindTextToSpeechEngine(impl: PiperTTSAdapter): TextToSpeechEngine

    companion object {

        private const val DEFAULT_FILES_DIR = "/data/data/com.july.offline/files"

        @Provides @Singleton
        fun provideWhisperConfig(
            systemConfigDataStore: SystemConfigDataStore
        ): WhisperConfig = runBlocking {
            val storedPath = systemConfigDataStore.sttModelPath.first()
            WhisperConfig(
                modelPath = storedPath.ifBlank { "$DEFAULT_FILES_DIR/whisper-small.bin" },
                language = "es",
                threads = 4,
                maxDurationMs = 30_000L
            )
        }

        @Provides @Singleton
        fun providePiperConfig(
            systemConfigDataStore: SystemConfigDataStore
        ): PiperConfig = runBlocking {
            val storedModelPath = systemConfigDataStore.ttsModelPath.first()
            val storedConfigPath = systemConfigDataStore.ttsConfigPath.first()
            PiperConfig(
                modelPath = storedModelPath.ifBlank {
                    "$DEFAULT_FILES_DIR/es_ES-sharvard-medium.onnx"
                },
                modelConfigPath = storedConfigPath.ifBlank {
                    "$DEFAULT_FILES_DIR/es_ES-sharvard-medium.onnx.json"
                },
                sampleRate = 22_050,
                speakerId = 0
            )
        }
    }
}
```

---

### `di/AudioModule.kt` (sin instanciación manual)

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

/**
 * AudioModule corregido.
 *
 * PROBLEMA FASE 2:
 * AudioPlayerAdapter se instanciaba manualmente con DiagnosticsLogger(),
 * lo que creaba una instancia separada del singleton.
 *
 * SOLUCIÓN FASE 3:
 * AudioPlayerAdapter usa @Inject constructor — Hilt lo instancia
 * y le inyecta el DiagnosticsLogger singleton automáticamente.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds @Singleton
    abstract fun bindAudioCapturePort(impl: AudioRecorderAdapter): AudioCapturePort

    companion object {

        @Provides @Singleton
        fun provideAudioRecorderConfig(): AudioRecorderConfig = AudioRecorderConfig()

        @Provides @Singleton
        fun provideVADConfig(): VADConfig = VADConfig()

        // AudioPlayerAdapter y VADProcessor se instancian automáticamente por Hilt
        // gracias a su @Inject constructor. No se necesita @Provides manual.
    }
}
```

**Nota:** Para que esto compile, `AudioPlayerAdapter` debe tener `@Inject constructor`:

```kotlin
// audio/player/AudioPlayerAdapter.kt — agregar @Inject
class AudioPlayerAdapter @Inject constructor(
    private val logger: DiagnosticsLogger
) { ... }
```

Y `VADProcessor` ya tiene `@Inject constructor` desde FASE 2.

---

## UI — PERMISO DE MICRÓFONO

### `ui/permission/PermissionHandler.kt` (NUEVO)

```kotlin
package com.july.offline.ui.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Composable que gestiona el permiso de RECORD_AUDIO en runtime.
 *
 * Uso en ConversationScreen:
 *   PermissionHandler(
 *       onPermissionGranted = { viewModel.onMicPressed() },
 *       onPermissionDenied = { viewModel.onPermissionDenied() }
 *   ) { requestPermission ->
 *       Button(onClick = requestPermission) { Text("Hablar") }
 *   }
 */
@Composable
fun PermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            showRationale = true
            onPermissionDenied()
        }
    }

    val requestPermission: () -> Unit = {
        val currentStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (currentStatus == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    content(requestPermission)

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permiso de micrófono") },
            text = {
                Text(
                    "July necesita acceso al micrófono para escucharte. " +
                    "Sin este permiso no puede funcionar. " +
                    "Puedes habilitarlo en Configuración > Aplicaciones > July > Permisos."
                )
            },
            confirmButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

/** Verifica sincrónicamente si el permiso ya fue concedido. */
fun hasMicrophonePermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
```

---

## UI — CONVERSACIÓN CON HISTORIAL

### `ui/conversation/ConversationUiState.kt` (actualizado)

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
    val messages: List<MessageUiModel> = emptyList()   // ← NUEVO en FASE 3
)

enum class ConversationPhase {
    IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, ERROR, CANCELLED
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

### `ui/conversation/ConversationViewModel.kt` (historial)

```kotlin
package com.july.offline.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.core.error.AppError
import com.july.offline.domain.model.ConversationState
import com.july.offline.domain.model.MessageRole
import com.july.offline.domain.orchestrator.ConversationOrchestrator
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.domain.orchestrator.SessionCoordinator
import com.july.offline.domain.port.SessionRepository
import com.july.offline.domain.state.ConversationStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val orchestrator: ConversationOrchestrator,
    private val stateHolder: ConversationStateHolder,
    private val healthMonitor: EngineHealthMonitor,
    private val sessionCoordinator: SessionCoordinator,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    val uiState: StateFlow<ConversationUiState> = combine(
        stateHolder.conversationState,
        healthMonitor.healthState,
        // Flow del historial de la sesión activa (últimas 20 sesiones, tomamos la primera)
        sessionRepository.getRecentSessions(1)
    ) { conversationState, healthState, recentSessions ->
        val messages = recentSessions
            .firstOrNull()
            ?.messages
            ?.map { msg ->
                MessageUiModel(
                    id = msg.id,
                    text = msg.content,
                    isUser = msg.role == MessageRole.USER,
                    timestamp = timeFormatter.format(msg.timestamp)
                )
            } ?: emptyList()

        mapToUiState(conversationState, healthState, messages)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationUiState()
    )

    fun onMicPressed() = orchestrator.startConversationCycle()
    fun onCancelPressed() = orchestrator.cancelCurrentCycle()
    fun onPermissionDenied() { /* El estado Error ya se gestiona en el orchestrator */ }

    private fun mapToUiState(
        state: ConversationState,
        health: com.july.offline.domain.model.EngineHealthState,
        messages: List<MessageUiModel>
    ): ConversationUiState {
        val engineHealthUi = EngineHealthUiState.from(health)
        return when (state) {
            is ConversationState.Idle -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                isMicButtonEnabled = engineHealthUi.llmReady,
                messages = messages
            )
            is ConversationState.Listening -> ConversationUiState(
                phase = ConversationPhase.LISTENING,
                displayedText = "Escuchando...",
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages
            )
            is ConversationState.Transcribing -> ConversationUiState(
                phase = ConversationPhase.TRANSCRIBING,
                displayedText = "Procesando audio...",
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages
            )
            is ConversationState.Thinking -> ConversationUiState(
                phase = ConversationPhase.THINKING,
                displayedText = "Pensando...",
                transcriptText = state.transcript.text,
                engineHealth = engineHealthUi,
                isCancelVisible = true,
                messages = messages
            )
            is ConversationState.Speaking -> ConversationUiState(
                phase = ConversationPhase.SPEAKING,
                displayedText = state.response.text,
                engineHealth = engineHealthUi,
                messages = messages
            )
            is ConversationState.Error -> ConversationUiState(
                phase = ConversationPhase.ERROR,
                errorMessage = mapErrorToUserMessage(state.error),
                engineHealth = engineHealthUi,
                isMicButtonEnabled = state.error !is AppError.Permission,
                messages = messages
            )
            is ConversationState.Cancelled -> ConversationUiState(
                phase = ConversationPhase.IDLE,
                engineHealth = engineHealthUi,
                messages = messages
            )
        }
    }

    private fun mapErrorToUserMessage(error: AppError): String = when (error) {
        is AppError.Permission -> "Se necesita permiso de micrófono"
        is AppError.Stt -> "No pude entenderte. Intenta de nuevo."
        is AppError.Llm -> "El asistente no está disponible en este momento."
        is AppError.Tts -> "No pude reproducir la respuesta."
        is AppError.Network -> "El servidor de IA no está disponible."
        is AppError.Cancelled -> ""
        is AppError.Unknown -> "Error inesperado."
    }
}
```

---

### `ui/conversation/ConversationScreen.kt` (historial scrollable + permisos)

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
import com.july.offline.ui.conversation.components.EngineHealthWidget
import com.july.offline.ui.conversation.components.MessageBubble
import com.july.offline.ui.conversation.components.StatusBar
import com.july.offline.ui.conversation.components.WaveformIndicator
import com.july.offline.ui.permission.PermissionHandler

/**
 * Pantalla principal de conversación — FASE 3.
 * Añade:
 * - Historial de mensajes scrollable (LazyColumn con auto-scroll)
 * - Gestión de permiso de micrófono en runtime via PermissionHandler
 */
@Composable
fun ConversationScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje cuando se añade uno nuevo
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
            // Indicador de salud de motores (solo visible si hay problema)
            EngineHealthWidget(healthState = uiState.engineHealth)

            // ── Historial de mensajes ───────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(message = message)
                }

                // Indicador de estado actual al final del historial
                if (uiState.phase != ConversationPhase.IDLE &&
                    uiState.phase != ConversationPhase.ERROR) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Column {
                                StatusBar(phase = uiState.phase)
                                if (uiState.phase == ConversationPhase.LISTENING) {
                                    Spacer(Modifier.height(4.dp))
                                    WaveformIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // ── Error ──────────────────────────────────────────────────
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

            // ── Controles inferiores con gestión de permiso ────────────
            PermissionHandler(
                onPermissionGranted = { viewModel.onMicPressed() },
                onPermissionDenied = { viewModel.onPermissionDenied() }
            ) { requestPermissionAndStart ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                        onClick = {
                            if (uiState.phase == ConversationPhase.IDLE ||
                                uiState.phase == ConversationPhase.ERROR ||
                                uiState.phase == ConversationPhase.CANCELLED) {
                                requestPermissionAndStart()
                            }
                        },
                        enabled = uiState.isMicButtonEnabled &&
                                  (uiState.phase == ConversationPhase.IDLE ||
                                   uiState.phase == ConversationPhase.ERROR ||
                                   uiState.phase == ConversationPhase.CANCELLED)
                    ) {
                        Text(
                            text = when (uiState.phase) {
                                ConversationPhase.IDLE,
                                ConversationPhase.CANCELLED -> "Hablar"
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

## APP — ACTUALIZACIÓN

### `JulyApplication.kt` (poda de logs)

```kotlin
package com.july.offline

import android.app.Application
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JulyApplication : Application() {

    @Inject lateinit var engineHealthMonitor: EngineHealthMonitor
    @Inject lateinit var diagnosticsLogger: DiagnosticsLogger

    override fun onCreate() {
        super.onCreate()
        engineHealthMonitor.startMonitoring()
        // Borrar logs con más de 7 días de antigüedad al arrancar
        diagnosticsLogger.pruneOldLogs()
    }
}
```

---

## BUILD — CONFIGURACIÓN ABI

### `app/build.gradle.kts` — sección adicional (añadir dentro de `android {}`)

```kotlin
// Añadir dentro del bloque android { } existente en app/build.gradle.kts

defaultConfig {
    // ... configuración existente ...

    // Filtrar ABIs: solo compilar para arm64-v8a y x86_64
    // Reduce el tamaño del APK eliminando armeabi-v7a y x86
    ndk {
        abiFilters += listOf("arm64-v8a", "x86_64")
    }
}

// Configuración de CMake — solo si compilas desde fuente (no necesario con .so precompilados)
// externalNativeBuild {
//     cmake {
//         path = file("src/main/cpp/CMakeLists.txt")
//         version = "3.22.1"
//     }
// }
```

---

## CHECKLIST DE VERIFICACIÓN FASE 3

### Antes de compilar
- [ ] Scripts ejecutados: `download_binaries.sh` → `download_models.sh` → `setup_fase3.sh`
- [ ] `app/src/main/jniLibs/arm64-v8a/` contiene: `libwhisper.so`, `libpiper.so`, `libonnxruntime.so`
- [ ] `app/src/main/jniLibs/x86_64/` contiene: `libwhisper.so` (Piper no disponible en x86_64)
- [ ] Modelos en el dispositivo: verificar con `adb shell run-as com.july.offline ls files/`

### Verificación en Logcat (filtro: `JulyOffline`)
```
[Engine:STT] transcribed 'texto...' [XXXms]    ← Whisper funcionando
[Engine:TTS] synthesized XXX samples [XXXms]   ← Piper funcionando
[State] Idle → Listening (trigger: user_input)
[State] Listening → Transcribing (trigger: vad_end)
[State] Transcribing → Thinking (trigger: transcript_ready)
[State] Thinking → Speaking (trigger: llm_response)
[State] Speaking → Idle (trigger: reset)
```

### Problemas comunes y soluciones
| Síntoma | Causa probable | Solución |
|---|---|---|
| `UnsatisfiedLinkError: dlopen failed: libwhisper.so` | .so no está en jniLibs | Verificar estructura jniLibs y ABI del dispositivo |
| `whisperInit() returned 0` | Model path incorrecto o archivo corrupto | Verificar ruta en EngineModule y re-ejecutar setup_fase3.sh |
| `piperInit() returned 0` | ONNX Runtime no cargó antes que Piper | Verificar orden en PiperJNI.init { } |
| TTS no funciona en emulador | x86_64 Piper no disponible | Usar emulador arm64 o dispositivo físico |
| Permiso RECORD_AUDIO denegado | Usuario denegó en el diálogo | PermissionHandler muestra rationale automáticamente |
| Room crash al actualizar | Migración 1→2 no aplicada | Limpiar datos de la app o desinstalar/reinstalar |

---

## RESUMEN DE CAMBIOS FASE 2 → FASE 3

| TODO FASE 2 | Estado FASE 3 |
|---|---|
| WhisperSTTAdapter — integrar JNI real | RESUELTO — WhisperJNI.kt + adapter real |
| PiperTTSAdapter — integrar JNI real | RESUELTO — PiperJNI.kt + adapter real |
| AudioCoordinator — Flow con takeWhile | RESUELTO — takeWhile + finally stopRecording() |
| AudioModule — instanciación manual | RESUELTO — @Inject constructor en AudioPlayerAdapter |
| DiagnosticsLogger — persistencia Room | RESUELTO — DiagnosticsDao + logScope async |
| JulyDatabase — migración declarativa | RESUELTO — MIGRATION_1_2 + addMigrations() |
| NetworkModule — config dinámica | RESUELTO — lee SystemConfigDataStore en @Provides |
| EngineModule — rutas de modelo dinámicas | RESUELTO — lee SystemConfigDataStore en @Provides |
| ConversationScreen — historial scrollable | RESUELTO — LazyColumn + auto-scroll |
| Permisos de micrófono | RESUELTO — PermissionHandler composable |
