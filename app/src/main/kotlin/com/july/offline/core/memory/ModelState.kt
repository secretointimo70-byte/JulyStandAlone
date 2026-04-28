package com.july.offline.core.memory

/**
 * Estado del ciclo de vida de un modelo JNI en memoria.
 */
enum class ModelState {
    /** Modelo no cargado. El contexto JNI es 0L. */
    UNLOADED,
    /** Modelo cargándose (initContext() en progreso). */
    LOADING,
    /** Modelo en memoria y listo para inferencia. */
    LOADED,
    /** Modelo liberándose (whisperFree / piperFree en progreso). */
    RELEASING
}
