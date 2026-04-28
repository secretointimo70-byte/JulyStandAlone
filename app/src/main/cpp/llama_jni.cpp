#include <jni.h>
#include "llama.h"
#include <string>
#include <vector>
#include <cstring>
#include <android/log.h>

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaState {
    llama_model   *model;
    llama_context *ctx;
    uint32_t       n_ctx;
    int32_t        n_threads;
};

static llama_context *make_context(LlamaState *s) {
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx           = s->n_ctx;
    cp.n_threads       = s->n_threads;
    cp.n_threads_batch = s->n_threads;
    return llama_new_context_with_model(s->model, cp);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaInit(
    JNIEnv *env, jobject,
    jstring modelPath, jint contextSize, jint threads, jint gpuLayers)
{
    llama_backend_init();

    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = (int)gpuLayers;

    llama_model *model = llama_model_load_from_file(path, mp);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) { LOGE("llama_model_load_from_file failed"); return 0L; }

    auto *s = new LlamaState{};
    s->model     = model;
    s->n_ctx     = (uint32_t)contextSize;
    s->n_threads = (int32_t)threads;
    s->ctx       = make_context(s);

    if (!s->ctx) {
        LOGE("llama_new_context_with_model failed");
        llama_model_free(model);
        delete s;
        return 0L;
    }
    LOGI("initialized (ctx=%d threads=%d)", contextSize, threads);
    return (jlong)(intptr_t)s;
}

JNIEXPORT jstring JNICALL
Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaGenerate(
    JNIEnv *env, jobject,
    jlong handle, jstring promptStr, jint maxTokens,
    jfloat temperature, jfloat topP, jfloat repeatPenalty)
{
    auto *s = (LlamaState *)(intptr_t)handle;
    if (!s) return env->NewStringUTF("");

    // Reset KV cache by recreating the context (model stays loaded)
    if (s->ctx) llama_free(s->ctx);
    s->ctx = make_context(s);
    if (!s->ctx) { LOGE("context recreate failed"); return env->NewStringUTF(""); }

    const llama_vocab *vocab = llama_model_get_vocab(s->model);
    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);

    // Tokenize prompt
    std::vector<llama_token> tokens((int)s->n_ctx);
    int n_tokens = llama_tokenize(
        vocab, prompt, (int32_t)strlen(prompt),
        tokens.data(), (int32_t)tokens.size(),
        true, true);
    env->ReleaseStringUTFChars(promptStr, prompt);

    if (n_tokens <= 0) { LOGE("tokenize failed: %d", n_tokens); return env->NewStringUTF(""); }
    tokens.resize(n_tokens);

    // Decode prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t)n_tokens);
    if (llama_decode(s->ctx, batch) != 0) {
        LOGE("llama_decode (prompt) failed");
        return env->NewStringUTF("");
    }

    // Sampler chain
    llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    llama_sampler *smpl = llama_sampler_chain_init(sp);
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(64, repeatPenalty, 0.0f, 0.0f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    // Generate tokens
    std::string result;
    char piece[256];

    for (int i = 0; i < (int)maxTokens; i++) {
        llama_token id = llama_sampler_sample(smpl, s->ctx, -1);

        if (llama_vocab_is_eog(vocab, id)) break;

        int n = llama_token_to_piece(vocab, id, piece, sizeof(piece) - 1, 0, true);
        if (n > 0) { piece[n] = '\0'; result += piece; }

        llama_batch next = llama_batch_get_one(&id, 1);
        if (llama_decode(s->ctx, next) != 0) {
            LOGE("llama_decode (gen) failed at token %d", i);
            break;
        }
    }

    llama_sampler_free(smpl);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jint JNICALL
Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaTokenize(
    JNIEnv *env, jobject, jlong handle, jstring textStr)
{
    auto *s = (LlamaState *)(intptr_t)handle;
    if (!s) return 0;

    const char *text = env->GetStringUTFChars(textStr, nullptr);
    std::vector<llama_token> tokens((int)s->n_ctx);
    int n = llama_tokenize(
        llama_model_get_vocab(s->model),
        text, (int32_t)strlen(text),
        tokens.data(), (int32_t)tokens.size(),
        false, false);
    env->ReleaseStringUTFChars(textStr, text);
    return (jint)(n < 0 ? 0 : n);
}

JNIEXPORT void JNICALL
Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaFree(
    JNIEnv *, jobject, jlong handle)
{
    auto *s = (LlamaState *)(intptr_t)handle;
    if (s) {
        if (s->ctx)   llama_free(s->ctx);
        if (s->model) llama_model_free(s->model);
        delete s;
        LOGI("state freed");
    }
}

JNIEXPORT jstring JNICALL
Java_com_july_offline_ai_llm_embedded_LlamaCppJNI_llamaModelInfo(
    JNIEnv *env, jobject, jlong handle)
{
    auto *s = (LlamaState *)(intptr_t)handle;
    if (!s) return env->NewStringUTF("{}");

    char buf[256] = {};
    llama_model_desc(s->model, buf, sizeof(buf) - 1);

    std::string json = "{\"name\":\"";
    json += buf;
    json += "\"}";
    return env->NewStringUTF(json.c_str());
}

} // extern "C"
