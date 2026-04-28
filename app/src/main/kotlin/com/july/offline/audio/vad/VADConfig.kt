package com.july.offline.audio.vad

data class VADConfig(
    val silenceThresholdMs: Long = 700L,    // pausa natural al terminar de hablar
    val energyThreshold: Double = 200.0,    // correcto con PCM signed: silencio ~50-150, voz >1000
    val minAudioDurationMs: Long = 500L,    // descartar ruidos muy cortos
    val maxRecordingMs: Long = 8_000L       // techo duro: evita escucha infinita si el VAD no dispara
)
