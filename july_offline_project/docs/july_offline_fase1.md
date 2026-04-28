# JULY OFFLINE — FASE 1
## Arquitectura y Estructura Base
### `com.july.offline`

Esta fase fue entregada como documento arquitectónico interactivo en el chat.
El contenido completo está resumido a continuación.

---

## RESUMEN EJECUTIVO

July Offline es un asistente de voz Android completamente offline-first.
Toda la inferencia ocurre en el dispositivo: STT (Whisper.cpp), LLM (llama.cpp),
TTS (Piper). Sin dependencias de red externa en producción.

---

## STACK TECNOLÓGICO

- Kotlin 2.0.21 + Jetpack Compose BOM 2024.11
- Hilt 2.52 (DI)
- Room 2.6.1 (persistencia)
- DataStore 1.1.1 (configuración)
- Retrofit 2.11.0 + OkHttp 4.12.0 (red local LLM servidor)
- Coroutines 1.9.0 + StateFlow
- JUnit 5 + MockK + Turbine (tests)
- minSdk 26 / targetSdk 35
- ABIs: arm64-v8a + x86_64

---

## ARQUITECTURA: CAPAS

```
ui/           → Compose screens, ViewModels, UiState
navigation/   → NavGraph, Destinations
settings/     → AppSettings data class
di/           → Hilt modules
domain/       → Orquestador, Coordinadores, Interfaces, Estado
  model/      → ConversationState, SessionEntity, Message, Transcript, LlmResponse
  port/       → SpeechToTextEngine, LanguageModelEngine, TextToSpeechEngine,
                AudioCapturePort, SessionRepository, WakeWordEngine
  state/      → ConversationStateHolder (fuente única de verdad)
  orchestrator/ → ConversationOrchestrator, AudioCoordinator,
                  SessionCoordinator, EngineHealthMonitor, WakeWordCoordinator
ai/           → Adapters JNI: Whisper (STT), Piper (TTS), llama.cpp (LLM)
              → LlmRouter (embebido + fallback servidor)
audio/        → AudioRecorderAdapter, AudioPlayerAdapter, VADProcessor
wakeword/     → PorcupineWakeWordAdapter
lifecycle/    → AppLifecycleObserver, ModelReleaseTimer
data/         → Room DB, DAOs, DataStore, SessionRepositoryImpl, NetworkClient
core/         → AppError, JulyResult, DiagnosticsLogger, CoroutineDispatchers,
                ModelMemoryManager, OomHandler, AssetCopier
```

---

## FLUJO DE CONVERSACIÓN

```
IDLE → WAKE_WORD_LISTENING → LISTENING → TRANSCRIBING → THINKING → SPEAKING → IDLE
         (Porcupine)          (VAD)       (Whisper)    (llama.cpp)  (Piper)
```

---

## REGLAS DE DEPENDENCIAS

- `domain` no depende de `data`, `ai`, `audio` ni `ui`
- `ui` no accede directamente a `data` ni `ai`
- `di` es el único punto que conoce todas las capas
- Las interfaces viven en `domain/port/` — las implementaciones en `ai/`, `audio/`, `data/`

---

## MOTORES

| Motor | Tecnología | Modelo | Tamaño |
|---|---|---|---|
| STT | Whisper.cpp JNI | whisper-small.bin | 466 MB |
| LLM | llama.cpp JNI | Llama-3.2-3B Q4_K_M | ~2.0 GB |
| LLM fallback | HTTP local | Ollama/LM Studio | — |
| TTS | Piper JNI | es_ES-sharvard-medium | 300 MB |
| Wake-word | Porcupine SDK | oye_july_es.ppn | ~1 MB |

---

## GESTIÓN DE MEMORIA

Dos modos configurables por el usuario:

- **SPEED**: modelos en RAM siempre (~3 GB total). Primera respuesta instantánea.
- **MEMORY**: modelos liberados tras 5 min en background (~400 MB libres).
  Recarga al volver: ~10s (LLM) + ~3s (STT/TTS).

---

## ESTADOS DE CONVERSACIÓN

```kotlin
sealed class ConversationState {
    object Idle
    object WakeWordListening
    data class Listening(val sessionId: String)
    data class Transcribing(val sessionId: String, val audioLengthMs: Long)
    data class Thinking(val sessionId: String, val transcript: Transcript)
    data class Speaking(val sessionId: String, val response: LlmResponse)
    data class Error(val error: AppError, val previousState: ConversationState)
    object Cancelled
}
```

---

## DESIGN SYSTEM (FASE 7)

- Tema oscuro por defecto, acento verde terminal `#39D353`
- JetBrains Mono para elementos de sistema
- Inter para contenido conversacional
- Bordes de 0.5dp, sin sombras, geometría precisa
- Dynamic Color (Material You) desactivado intencionalmente

---

## FASES DEL PROYECTO

| Fase | Contenido |
|---|---|
| FASE 1 | Arquitectura (este documento) |
| FASE 2 | Código base completo: domain, core, data, di, ui, esqueletos ai/audio |
| FASE 3 | JNI real Whisper + Piper, permisos, historial UI, scripts setup |
| FASE 4 | Wake-word Porcupine, tests JUnit5 + MockK + Turbine |
| FASE 5 | Lifecycle ProcessLifecycleOwner, gestión memoria JNI |
| FASE 6 | LLM embebido llama.cpp + LlmRouter con fallback |
| FASE 7 | Design System completo: tema oscuro verde, tipografía, animaciones |
| FASE 8 | Tests instrumentados Android + ProGuard + AssetCopier + OOM handler |
| Fix | CoroutineDispatchers open properties |
| Script | setup_project_structure.sh |
