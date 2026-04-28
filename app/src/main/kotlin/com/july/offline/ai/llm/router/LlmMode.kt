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
