# July Offline
## Asistente de voz Android completamente offline

> STT · LLM · TTS · Wake-word — todo en el dispositivo, sin red externa.

---

## Índice

1. [Descripción](#descripción)
2. [Requisitos de hardware](#requisitos-de-hardware)
3. [Stack tecnológico](#stack-tecnológico)
4. [Estructura del proyecto](#estructura-del-proyecto)
5. [Setup inicial](#setup-inicial)
6. [Claves API necesarias](#claves-api-necesarias)
7. [Descarga de modelos](#descarga-de-modelos)
8. [Compilación](#compilación)
9. [Configuración de motores](#configuración-de-motores)
10. [Arquitectura en resumen](#arquitectura-en-resumen)
11. [Fases del proyecto](#fases-del-proyecto)
12. [Tests](#tests)
13. [ProGuard](#proguard)
14. [Problemas comunes](#problemas-comunes)

---

## Descripción

July es un asistente de voz Android que funciona completamente sin conexión a internet.
Toda la inferencia ocurre en el dispositivo:

- **STT**: Whisper.cpp (reconocimiento de voz en español)
- **LLM**: llama.cpp embebido con fallback a servidor local (Ollama/LM Studio)
- **TTS**: Piper (síntesis de voz en español)
- **Wake-word**: Porcupine ("Oye July") con modelo personalizable

El proyecto está organizado en 8 fases de implementación, desde la arquitectura
base hasta los tests instrumentados y la configuración de producción.

---

## Requisitos de hardware

| Componente | Mínimo | Recomendado |
|---|---|---|
| RAM | 4 GB | 6 GB+ |
| Almacenamiento libre | 4 GB | 6 GB |
| Android | API 26 (Android 8.0) | API 34 (Android 14) |
| ABI | arm64-v8a | arm64-v8a |
| CPU | Octa-core | Snapdragon 8 Gen 2+ |

**Nota sobre memoria:**
- Modo SPEED: ~3 GB RAM en uso (todos los modelos cargados)
- Modo MEMORY: ~400 MB en background (modelos liberados tras 5 min)

---

## Stack tecnológico

```
Kotlin 2.0.21
Jetpack Compose BOM 2024.11.00
Hilt 2.52
Room 2.6.1
DataStore 1.1.1
Retrofit 2.11.0 / OkHttp 4.12.0
Coroutines 1.9.0
JUnit 5 + MockK 1.13.12 + Turbine 1.1.0
Porcupine SDK 3.0.2
minSdk 26 / targetSdk 35
```

---

## Estructura del proyecto

```
july-offline/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/july/offline/
│   │   │   │   ├── core/          # AppError, JulyResult, Logger, Memory, OOM
│   │   │   │   ├── domain/        # Orquestador, Coordinadores, Interfaces, Estado
│   │   │   │   ├── ai/            # Whisper, Piper, llama.cpp, LlmRouter
│   │   │   │   ├── audio/         # Grabación, Reproducción, VAD
│   │   │   │   ├── wakeword/      # Porcupine adapter
│   │   │   │   ├── lifecycle/     # AppLifecycleObserver, ModelReleaseTimer
│   │   │   │   ├── data/          # Room, DataStore, Repositorios, Red
│   │   │   │   ├── di/            # Módulos Hilt
│   │   │   │   ├── ui/            # Compose screens y componentes
│   │   │   │   ├── navigation/    # NavGraph
│   │   │   │   └── settings/      # AppSettings
│   │   │   ├── assets/            # oye_july_es.ppn (modelo Porcupine)
│   │   │   ├── jniLibs/
│   │   │   │   ├── arm64-v8a/     # libwhisper.so, libpiper.so, libonnxruntime.so, libllama.so, libggml.so
│   │   │   │   └── x86_64/        # libwhisper.so, libllama.so, libggml.so
│   │   │   └── res/
│   │   │       ├── font/          # jetbrains_mono, inter
│   │   │       └── values/        # themes.xml, colors.xml
│   │   ├── test/                  # Tests unitarios JUnit 5 + MockK
│   │   └── androidTest/           # Tests instrumentados Hilt + Room
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── scripts/
│   ├── download_binaries.sh       # Descarga .so precompilados
│   ├── download_models.sh         # Descarga modelos Whisper y Piper
│   ├── download_llm_model.sh      # Descarga modelo GGUF llama.cpp
│   ├── setup_fase3.sh             # Instala modelos STT/TTS en dispositivo
│   ├── setup_fase6.sh             # Instala modelo LLM en dispositivo
│   └── models/                    # Modelos descargados (no incluir en git)
└── docs/
    ├── SETUP_FASE3.md
    └── SETUP_FASE6.md
```

---

## Setup inicial

### 1. Crear proyecto en Android Studio

```
File > New > New Project > Empty Activity
Name: July Offline
Package: com.july.offline
Language: Kotlin
Min SDK: API 26
```

### 2. Crear estructura de directorios

```bash
bash setup_project_structure.sh
```

### 3. Copiar código de cada fase

Copiar el contenido de cada archivo `.kt` desde los documentos de fase
a sus rutas correspondientes. Orden obligatorio:

```
FASE 2 → FASE 3 → fix_dispatchers → FASE 4 → FASE 5 → FASE 6 → FASE 7 → FASE 8
```

### 4. Descargar binarios JNI

```bash
bash scripts/download_binaries.sh
```

Descarga `libwhisper.so`, `libpiper.so`, `libonnxruntime.so`,
`libllama.so`, `libggml.so` para arm64-v8a y x86_64.

### 5. Descargar modelos

```bash
bash scripts/download_models.sh      # Whisper-small (466 MB) + Piper-sharvard (300 MB)
bash scripts/download_llm_model.sh   # Llama-3.2-3B Q4_K_M (~2 GB)
```

### 6. Instalar modelos en el dispositivo

```bash
bash scripts/setup_fase3.sh   # STT + TTS
bash scripts/setup_fase6.sh   # LLM
```

---

## Claves API necesarias

### Picovoice (Porcupine wake-word) — OBLIGATORIA

1. Crear cuenta gratuita en https://picovoice.ai/console/
2. Copiar el `AccessKey` del dashboard
3. Establecer variable de entorno:
   ```bash
   export PICOVOICE_KEY="tu_clave_aqui"
   ```
4. La clave se inyecta automáticamente en `BuildConfig.PICOVOICE_ACCESS_KEY`
   via `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "PICOVOICE_ACCESS_KEY",
       "\"${System.getenv("PICOVOICE_KEY") ?: ""}\"")
   ```

### Modelo personalizado "Oye July" — OPCIONAL (mejora la precisión)

1. Ir a https://console.picovoice.ai/ppn
2. Seleccionar idioma: `Spanish (ES)`
3. Escribir la frase: `Oye July`
4. Descargar el archivo `.ppn` generado
5. Colocar en `app/src/main/assets/oye_july_es.ppn`
6. `AssetCopier` lo copia automáticamente a `filesDir` en el primer arranque

Sin el modelo custom, Porcupine usa `hey siri` como fallback built-in.

---

## Descarga de modelos

| Modelo | Tamaño | Fuente |
|---|---|---|
| whisper-small.bin | 466 MB | Hugging Face: ggerganov/whisper.cpp |
| es_ES-sharvard-medium.onnx | 300 MB | Hugging Face: rhasspy/piper-voices |
| Llama-3.2-3B-Instruct-Q4_K_M.gguf | ~2.0 GB | Hugging Face: bartowski/Llama-3.2-3B-Instruct-GGUF |

Todos los scripts de descarga incluyen verificación de espacio disponible
y soporte de reanudación (`curl --retry 3`).

---

## Compilación

```bash
# Debug (para desarrollo)
./gradlew assembleDebug

# Release (con ProGuard)
./gradlew assembleRelease

# Tests unitarios
./gradlew test

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Tests instrumentados con dispositivo gestionado
./gradlew pixel6api34DebugAndroidTest
```

---

## Configuración de motores

Todas las configuraciones se persisten en `SystemConfigDataStore`
y son ajustables desde `SettingsScreen` en runtime.

### Motor LLM

| Modo | Descripción |
|---|---|
| AUTO (default) | llama.cpp embebido → fallback servidor si falla |
| EMBEDDED | Solo llama.cpp, sin red |
| SERVER | Solo servidor local (Ollama/LM Studio en 127.0.0.1:11434) |

### Memoria

| Modo | RAM foreground | RAM background >5min |
|---|---|---|
| SPEED (default) | ~3 GB | ~3 GB |
| MEMORY | ~3 GB | ~400 MB |

### Puerto del servidor LLM (modo SERVER/AUTO)

Por defecto: `http://127.0.0.1:11434` (Ollama).
Para LM Studio: cambiar puerto a `1234`.

---

## Arquitectura en resumen

```
UI (Compose)
    ↓ eventos
ConversationViewModel
    ↓ delega
ConversationOrchestrator  ← fuente única de verdad: ConversationStateHolder
    ↓ coordina
AudioCoordinator  →  AudioRecorderAdapter (PCM)
                  →  VADProcessor (fin de voz)
                  →  AudioPlayerAdapter (reproducción TTS)

SessionCoordinator  →  SessionRepository  →  Room DB

WakeWordCoordinator  →  PorcupineWakeWordAdapter  →  Porcupine SDK

SpeechToTextEngine (interface)
    ↑ implementa
WhisperSTTAdapter  →  WhisperJNI  →  libwhisper.so

LanguageModelEngine (interface)
    ↑ implementa
LlmRouter
    ├─ LlamaCppLLMAdapter  →  LlamaCppJNI  →  libllama.so
    └─ LocalServerLLMAdapter  →  Retrofit  →  HTTP 127.0.0.1

TextToSpeechEngine (interface)
    ↑ implementa
PiperTTSAdapter  →  PiperJNI  →  libpiper.so

ModelMemoryManager  ←─ gestiona contextos JNI de los 3 motores
AppLifecycleObserver  ←─ pausa Porcupine en background via ProcessLifecycleOwner
ModelReleaseTimer  ←─ libera modelos tras 5 min en background (modo MEMORY)
OomHandler  ←─ captura OutOfMemoryError en JNI y libera contextos
```

---

## Fases del proyecto

| Archivo | Contenido |
|---|---|
| `july_offline_fase1.md` | Arquitectura completa (este resumen + diagramas en chat) |
| `july_offline_fase2.md` | Código base: domain, core, data, di, ui, esqueletos ai/audio |
| `july_offline_fase3.md` | JNI real Whisper + Piper, permisos, historial UI, scripts setup |
| `july_offline_fase4.md` | Wake-word Porcupine, tests JUnit5 + MockK + Turbine |
| `july_offline_fase5.md` | Lifecycle ProcessLifecycleOwner, gestión memoria JNI |
| `july_offline_fase6.md` | LLM embebido llama.cpp + LlmRouter con fallback |
| `july_offline_fase7.md` | Design System: tema oscuro verde terminal, tipografía, animaciones |
| `july_offline_fase8.md` | Tests instrumentados + ProGuard + AssetCopier + OOM handler |
| `july_offline_fix_dispatchers.md` | Fix CoroutineDispatchers open properties (aplicar tras FASE 4) |
| `setup_project_structure.sh` | Script bash que crea todos los directorios y archivos vacíos |

---

## Tests

### Tests unitarios (sin Android)

```bash
./gradlew test
```

Cubren: `ConversationOrchestrator`, `WakeWordCoordinator`, `AudioCoordinator`,
`WhisperSTTAdapter`, `LlamaCppLLMAdapter`, `LlmRouter`, `LocalServerLLMAdapter`,
`SessionRepository`, `AppLifecycleObserver`, `ModelMemoryManager`, `ModelReleaseTimer`.

Stack: JUnit 5 + MockK + Turbine + TestCoroutineDispatchers.

### Tests instrumentados (requiere dispositivo o emulador)

```bash
./gradlew connectedAndroidTest
```

Cubren: Room (CRUD + migraciones v1→v2→v3), DataStore (preferencias + sistema),
`SessionRepositoryImpl` real, `PermissionHandler` en Compose.

Stack: Hilt Testing + Room en memoria + Espresso + Compose Test.

---

## ProGuard

`app/proguard-rules.pro` contiene reglas para:

- **JNI**: Whisper, Piper, llama.cpp — preserva nombres de métodos `external`
- **Porcupine**: evita ofuscación del SDK de Picovoice
- **Hilt/Dagger**: preserva clases generadas
- **Room**: preserva entidades y DAOs
- **Retrofit + Gson**: preserva modelos de request/response
- **Sealed classes y enums**: necesario para `when()` exhaustivo en runtime

---

## Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| `UnsatisfiedLinkError: libwhisper.so` | .so no está en jniLibs | Ejecutar `download_binaries.sh` y verificar ABI |
| `whisperInit() returned 0` | Modelo no encontrado en la ruta | Ejecutar `setup_fase3.sh` |
| `llamaInit() returned 0` | Modelo GGUF no en filesDir | Ejecutar `setup_fase6.sh` |
| TTS silencioso en emulador | Piper sin release x86_64 | Usar emulador arm64 o dispositivo físico |
| `UnsatisfiedLinkError` en release | ProGuard ofuscó nombres JNI | Verificar `proguard-rules.pro` sección JNI |
| Room crash al actualizar | Migración no aplicada | Desinstalar app y reinstalar, o limpiar datos |
| Wake-word no detecta "Oye July" | Sin modelo .ppn custom | Entrenar en console.picovoice.ai/ppn |
| OOM durante inferencia | RAM insuficiente | Activar modo MEMORY en Settings, usar modelo 1B |
| Porcupine pausado en background | Comportamiento correcto | Se reanuda automáticamente al volver a foreground |
