#include <jni.h>
#include "whisper.h"
#include <string>
#include <regex>
#include <android/log.h>

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Wraps context + thread count since whisper_full_params needs n_threads at transcribe time
struct WhisperState {
    whisper_context *ctx;
    int              nThreads;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_july_offline_ai_stt_WhisperJNI_whisperInit(
    JNIEnv *env, jobject,
    jstring modelPath, jint nThreads)
{
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!ctx) { LOGE("whisper_init_from_file_with_params failed"); return 0L; }

    auto *state = new WhisperState{ctx, (int)nThreads};
    LOGI("context initialized, nThreads=%d", nThreads);
    return (jlong)(intptr_t)state;
}

JNIEXPORT jstring JNICALL
Java_com_july_offline_ai_stt_WhisperJNI_whisperTranscribe(
    JNIEnv *env, jobject,
    jlong handle, jfloatArray pcmSamples, jstring language)
{
    auto *state = (WhisperState *)(intptr_t)handle;
    if (!state || !state->ctx) return env->NewStringUTF("");

    const char *lang    = env->GetStringUTFChars(language, nullptr);
    jsize        n      = env->GetArrayLength(pcmSamples);
    jfloat      *samples = env->GetFloatArrayElements(pcmSamples, nullptr);

    whisper_full_params wp = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);

    // Threading — was commented out before, causing single-thread performance
    wp.n_threads = state->nThreads;

    wp.language             = lang;
    wp.translate            = false;
    wp.no_context           = true;      // cada comando es independiente
    wp.no_timestamps        = true;      // no necesitamos timestamps
    wp.single_segment       = true;      // comandos de voz cortos → un solo segmento
    wp.print_special        = false;
    wp.print_progress       = false;
    wp.print_realtime       = false;
    wp.print_timestamps     = false;

    // Supresión de alucinaciones — estos tres son los más críticos
    wp.suppress_blank       = true;      // evita segmentos vacíos/silencio
    wp.suppress_nst         = true;      // suprime [Music] [Noise] [BLANK_AUDIO] etc.
    wp.no_speech_thold      = 0.9f;      // descarta solo cuando hay certeza alta de no-voz

    // Temperatura 0 = decodificación determinista y más conservadora
    wp.temperature          = 0.0f;
    wp.temperature_inc      = 0.2f;      // fallback si entropía alta

    // Sin initial_prompt: evita que Whisper lo repita cuando hay silencio/ruido
    wp.initial_prompt       = nullptr;

    wp.greedy.best_of       = 1;

    int ret = whisper_full(state->ctx, wp, samples, (int)n);

    env->ReleaseFloatArrayElements(pcmSamples, samples, JNI_ABORT);
    env->ReleaseStringUTFChars(language, lang);

    if (ret != 0) { LOGE("whisper_full failed: %d", ret); return env->NewStringUTF(""); }

    std::string result;
    int segs = whisper_full_n_segments(state->ctx);
    for (int i = 0; i < segs; i++) {
        const char *text = whisper_full_get_segment_text(state->ctx, i);
        if (text) result += text;
    }
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_july_offline_ai_stt_WhisperJNI_whisperFree(
    JNIEnv *, jobject, jlong handle)
{
    auto *state = (WhisperState *)(intptr_t)handle;
    if (state) {
        if (state->ctx) whisper_free(state->ctx);
        delete state;
        LOGI("context freed");
    }
}

} // extern "C"
