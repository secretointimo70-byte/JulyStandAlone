#!/bin/bash
# =============================================================================
# July Offline — Script de estructura de proyecto
# Crea todos los directorios y archivos vacíos listos para copiar el código
# de los documentos de FASE 2 a FASE 5.
#
# Uso:
#   1. Crear proyecto en Android Studio:
#      File > New > New Project > Empty Activity
#      Name: July Offline
#      Package: com.july.offline
#      Language: Kotlin
#      Min SDK: API 26
#
#   2. Desde la raíz del proyecto recién creado, ejecutar:
#      bash setup_project_structure.sh
#
#   3. Copiar el contenido de cada archivo desde los documentos de fase.
# =============================================================================

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$ROOT_DIR/app/src/main/kotlin/com/july/offline"
TEST="$ROOT_DIR/app/src/test/kotlin/com/july/offline"

echo "================================================="
echo " July Offline — Creando estructura del proyecto"
echo " Raíz: $ROOT_DIR"
echo "================================================="
echo ""

# =============================================================================
# FUNCIÓN AUXILIAR
# =============================================================================
mkfile() {
    mkdir -p "$(dirname "$1")"
    touch "$1"
}

# =============================================================================
# GRADLE / CONFIG
# =============================================================================
echo "[1/10] Configuración Gradle..."
mkfile "$ROOT_DIR/gradle/libs.versions.toml"
mkfile "$ROOT_DIR/build.gradle.kts"
mkfile "$ROOT_DIR/app/build.gradle.kts"
mkfile "$ROOT_DIR/app/proguard-rules.pro"

# =============================================================================
# JNILIBS
# =============================================================================
echo "[2/10] Directorios JNI..."
mkdir -p "$ROOT_DIR/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$ROOT_DIR/app/src/main/jniLibs/x86_64"
touch "$ROOT_DIR/app/src/main/jniLibs/arm64-v8a/.gitkeep"
touch "$ROOT_DIR/app/src/main/jniLibs/x86_64/.gitkeep"

# Assets (modelo Porcupine)
mkdir -p "$ROOT_DIR/app/src/main/assets"
touch "$ROOT_DIR/app/src/main/assets/.gitkeep"

# =============================================================================
# SCRIPTS DE SETUP
# =============================================================================
echo "[3/10] Scripts de setup..."
mkdir -p "$ROOT_DIR/scripts/models"
mkfile "$ROOT_DIR/scripts/download_binaries.sh"
mkfile "$ROOT_DIR/scripts/download_models.sh"
mkfile "$ROOT_DIR/scripts/setup_fase3.sh"
touch "$ROOT_DIR/scripts/models/.gitkeep"
chmod +x "$ROOT_DIR/scripts/download_binaries.sh"
chmod +x "$ROOT_DIR/scripts/download_models.sh"
chmod +x "$ROOT_DIR/scripts/setup_fase3.sh"

mkdir -p "$ROOT_DIR/docs"
mkfile "$ROOT_DIR/docs/SETUP_FASE3.md"

# =============================================================================
# CORE
# =============================================================================
echo "[4/10] Capa core..."
mkfile "$SRC/core/error/AppError.kt"
mkfile "$SRC/core/error/ErrorHandler.kt"
mkfile "$SRC/core/result/JulyResult.kt"
mkfile "$SRC/core/logging/DiagnosticsLogger.kt"
mkfile "$SRC/core/coroutines/CoroutineDispatchers.kt"
mkfile "$SRC/core/extensions/FlowExtensions.kt"
mkfile "$SRC/core/memory/ModelMode.kt"
mkfile "$SRC/core/memory/ModelState.kt"
mkfile "$SRC/core/memory/ModelMemoryManager.kt"

# =============================================================================
# DOMAIN — MODELS
# =============================================================================
echo "[5/10] Capa domain..."
mkfile "$SRC/domain/model/ConversationState.kt"
mkfile "$SRC/domain/model/RuntimeState.kt"
mkfile "$SRC/domain/model/EngineHealthState.kt"
mkfile "$SRC/domain/model/SessionEntity.kt"
mkfile "$SRC/domain/model/Message.kt"
mkfile "$SRC/domain/model/Transcript.kt"
mkfile "$SRC/domain/model/LlmResponse.kt"
mkfile "$SRC/domain/model/ModelInfo.kt"

# DOMAIN — PORTS
mkfile "$SRC/domain/port/SpeechToTextEngine.kt"
mkfile "$SRC/domain/port/LanguageModelEngine.kt"
mkfile "$SRC/domain/port/TextToSpeechEngine.kt"
mkfile "$SRC/domain/port/AudioCapturePort.kt"
mkfile "$SRC/domain/port/SessionRepository.kt"
mkfile "$SRC/domain/port/WakeWordEngine.kt"

# DOMAIN — STATE
mkfile "$SRC/domain/state/ConversationStateHolder.kt"

# DOMAIN — ORCHESTRATOR
mkfile "$SRC/domain/orchestrator/ConversationOrchestrator.kt"
mkfile "$SRC/domain/orchestrator/AudioCoordinator.kt"
mkfile "$SRC/domain/orchestrator/SessionCoordinator.kt"
mkfile "$SRC/domain/orchestrator/EngineHealthMonitor.kt"
mkfile "$SRC/domain/orchestrator/WakeWordCoordinator.kt"

# =============================================================================
# AI
# =============================================================================
echo "[6/10] Capa ai..."
mkfile "$SRC/ai/stt/WhisperConfig.kt"
mkfile "$SRC/ai/stt/WhisperJNI.kt"
mkfile "$SRC/ai/stt/WhisperSTTAdapter.kt"
mkfile "$SRC/ai/llm/LlmServerConfig.kt"
mkfile "$SRC/ai/llm/LlmApiService.kt"
mkfile "$SRC/ai/llm/LocalServerLLMAdapter.kt"
# FASE 6 — llama.cpp embebido (archivos preparados vacíos)
mkfile "$SRC/ai/llm/embedded/LlamaCppConfig.kt"
mkfile "$SRC/ai/llm/embedded/LlamaCppJNI.kt"
mkfile "$SRC/ai/llm/embedded/LlamaCppLLMAdapter.kt"
mkfile "$SRC/ai/tts/PiperConfig.kt"
mkfile "$SRC/ai/tts/PiperJNI.kt"
mkfile "$SRC/ai/tts/PiperTTSAdapter.kt"

# =============================================================================
# AUDIO
# =============================================================================
echo "[7/10] Capa audio..."
mkfile "$SRC/audio/recorder/AudioRecorderConfig.kt"
mkfile "$SRC/audio/recorder/AudioRecorderAdapter.kt"
mkfile "$SRC/audio/player/AudioPlayerAdapter.kt"
mkfile "$SRC/audio/vad/VADConfig.kt"
mkfile "$SRC/audio/vad/VADProcessor.kt"

# =============================================================================
# WAKEWORD
# =============================================================================
mkfile "$SRC/wakeword/PorcupineConfig.kt"
mkfile "$SRC/wakeword/PorcupineWakeWordAdapter.kt"

# =============================================================================
# LIFECYCLE
# =============================================================================
mkfile "$SRC/lifecycle/AppLifecycleObserver.kt"
mkfile "$SRC/lifecycle/ModelReleaseTimer.kt"

# =============================================================================
# DATA
# =============================================================================
echo "[8/10] Capa data..."
mkfile "$SRC/data/db/JulyDatabase.kt"
mkfile "$SRC/data/db/entity/SessionDbEntity.kt"
mkfile "$SRC/data/db/entity/MessageDbEntity.kt"
mkfile "$SRC/data/db/entity/DiagnosticsDbEntity.kt"
mkfile "$SRC/data/db/dao/SessionDao.kt"
mkfile "$SRC/data/db/dao/MessageDao.kt"
mkfile "$SRC/data/db/dao/DiagnosticsDao.kt"
mkfile "$SRC/data/datastore/AppPreferencesDataStore.kt"
mkfile "$SRC/data/datastore/SystemConfigDataStore.kt"
mkfile "$SRC/data/network/LocalNetworkClient.kt"
mkfile "$SRC/data/network/NetworkHealthChecker.kt"
mkfile "$SRC/data/repository/SessionRepositoryImpl.kt"

# =============================================================================
# DI
# =============================================================================
echo "[9/10] Capa di + navigation + settings + ui..."
mkfile "$SRC/di/EngineModule.kt"
mkfile "$SRC/di/DatabaseModule.kt"
mkfile "$SRC/di/NetworkModule.kt"
mkfile "$SRC/di/DataStoreModule.kt"
mkfile "$SRC/di/AudioModule.kt"
mkfile "$SRC/di/CoroutineModule.kt"
mkfile "$SRC/di/WakeWordModule.kt"
mkfile "$SRC/di/LifecycleModule.kt"
mkfile "$SRC/di/MemoryModule.kt"
# FASE 6
mkfile "$SRC/di/LlmModule.kt"

# =============================================================================
# SETTINGS
# =============================================================================
mkfile "$SRC/settings/AppSettings.kt"

# =============================================================================
# NAVIGATION
# =============================================================================
mkfile "$SRC/navigation/JulyDestination.kt"
mkfile "$SRC/navigation/JulyNavGraph.kt"

# =============================================================================
# UI
# =============================================================================
mkfile "$SRC/ui/conversation/ConversationUiState.kt"
mkfile "$SRC/ui/conversation/ConversationViewModel.kt"
mkfile "$SRC/ui/conversation/ConversationScreen.kt"
mkfile "$SRC/ui/conversation/components/WaveformIndicator.kt"
mkfile "$SRC/ui/conversation/components/StatusBar.kt"
mkfile "$SRC/ui/conversation/components/MessageBubble.kt"
mkfile "$SRC/ui/conversation/components/EngineHealthWidget.kt"
mkfile "$SRC/ui/conversation/components/WakeWordIndicator.kt"
mkfile "$SRC/ui/settings/SettingsViewModel.kt"
mkfile "$SRC/ui/settings/SettingsScreen.kt"
mkfile "$SRC/ui/permission/PermissionHandler.kt"

# =============================================================================
# APP
# =============================================================================
mkfile "$SRC/JulyApplication.kt"
mkfile "$SRC/MainActivity.kt"
mkfile "$ROOT_DIR/app/src/main/AndroidManifest.xml"

# =============================================================================
# TESTS
# =============================================================================
echo "[10/10] Estructura de tests..."
mkfile "$TEST/testutil/FakeSpeechToTextEngine.kt"
mkfile "$TEST/testutil/FakeLanguageModelEngine.kt"
mkfile "$TEST/testutil/FakeTextToSpeechEngine.kt"
mkfile "$TEST/testutil/FakeAudioCapturePort.kt"
mkfile "$TEST/testutil/FakeSessionRepository.kt"
mkfile "$TEST/testutil/FakeWakeWordEngine.kt"
mkfile "$TEST/testutil/FakeModelMemoryManager.kt"
mkfile "$TEST/testutil/TestCoroutineDispatchers.kt"
mkfile "$TEST/orchestrator/ConversationOrchestratorTest.kt"
mkfile "$TEST/orchestrator/WakeWordCoordinatorTest.kt"
mkfile "$TEST/orchestrator/AudioCoordinatorTest.kt"
mkfile "$TEST/ai/stt/WhisperSTTAdapterTest.kt"
mkfile "$TEST/ai/llm/LocalServerLLMAdapterTest.kt"
# FASE 6
mkfile "$TEST/ai/llm/LlamaCppLLMAdapterTest.kt"
mkfile "$TEST/data/repository/SessionRepositoryImplTest.kt"
mkfile "$TEST/lifecycle/AppLifecycleObserverTest.kt"
mkfile "$TEST/memory/ModelMemoryManagerTest.kt"
mkfile "$TEST/memory/ModelReleaseTimerTest.kt"

# =============================================================================
# RESUMEN
# =============================================================================
echo ""
echo "================================================="
echo " Estructura creada exitosamente"
echo "================================================="
echo ""
echo "Archivos creados:"
find "$SRC" -name "*.kt" | wc -l | xargs echo "  Kotlin (main):"
find "$TEST" -name "*.kt" | wc -l | xargs echo "  Kotlin (test):"
echo ""
echo "Próximos pasos:"
echo "  1. Copiar libs.versions.toml desde FASE 2 → gradle/libs.versions.toml"
echo "  2. Copiar build.gradle.kts desde FASE 2 → app/build.gradle.kts"
echo "  3. Copiar cada archivo .kt desde los documentos de fase"
echo "     Orden: FASE 2 → FASE 3 → fix_dispatchers → FASE 4 → FASE 5"
echo "  4. Ejecutar scripts/download_binaries.sh"
echo "  5. Ejecutar scripts/download_models.sh"
echo "  6. Ejecutar scripts/setup_fase3.sh"
echo "  7. Compilar desde Android Studio"
echo ""
echo "Archivos que necesitan tu clave API:"
echo "  - di/WakeWordModule.kt → PICOVOICE_ACCESS_KEY"
echo "  - app/build.gradle.kts → buildConfigField PICOVOICE_ACCESS_KEY"
echo ""
