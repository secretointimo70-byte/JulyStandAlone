package com.july.offline.download

data class DownloadableModel(
    val id: String,
    val displayName: String,
    val filename: String,
    val url: String,
    val sizeBytes: Long,
    val description: String
) {
    val sizeLabel: String get() = when {
        sizeBytes >= 1_000_000_000L -> "${"%.1f".format(sizeBytes / 1_000_000_000.0)} GB"
        sizeBytes >= 1_000_000L -> "${sizeBytes / 1_000_000} MB"
        else -> "${sizeBytes / 1_000} KB"
    }
}

object DownloadableModels {
    val WHISPER = DownloadableModel(
        id = "whisper_small",
        displayName = "Whisper Small",
        filename = "whisper-small.bin",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
        sizeBytes = 244_000_000L,
        description = "Reconocimiento de voz (STT)"
    )
    val LLAMA_1B = DownloadableModel(
        id = "llama_1b",
        displayName = "Llama 3.2 · 1B",
        filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        sizeBytes = 750_000_000L,
        description = "Modelo de lenguaje pequeño — recomendado"
    )
    val LLAMA_3B = DownloadableModel(
        id = "llama_3b",
        displayName = "Llama 3.2 · 3B",
        filename = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        url = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        sizeBytes = 2_000_000_000L,
        description = "Modelo de lenguaje completo — requiere 4 GB RAM"
    )

    val all = listOf(WHISPER, LLAMA_1B, LLAMA_3B)
}
