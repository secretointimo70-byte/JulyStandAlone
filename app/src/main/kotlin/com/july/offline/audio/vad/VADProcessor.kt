package com.july.offline.audio.vad

import javax.inject.Inject
import kotlin.math.sqrt

class VADProcessor @Inject constructor(
    private val config: VADConfig
) {

    private var silenceStartMs: Long? = null

    var lastEnergyLevel: Double = 0.0
        private set

    val maxRecordingMs: Long get() = config.maxRecordingMs

    fun isVoiceActive(): Boolean = lastEnergyLevel >= config.energyThreshold

    fun isSilence(chunk: ByteArray): Boolean {
        val energy = calculateRmsEnergy(chunk)
        lastEnergyLevel = energy
        val now = System.currentTimeMillis()

        return if (energy < config.energyThreshold) {
            val silenceStart = silenceStartMs ?: run {
                silenceStartMs = now
                now
            }
            (now - silenceStart) >= config.silenceThresholdMs
        } else {
            silenceStartMs = null
            false
        }
    }

    fun hasExceededMaxDuration(recordingStartMs: Long): Boolean =
        System.currentTimeMillis() - recordingStartMs >= config.maxRecordingMs

    fun reset() {
        silenceStartMs = null
    }

    private fun calculateRmsEnergy(pcm: ByteArray): Double {
        if (pcm.size < 2) return 0.0
        var sum = 0.0
        var sampleCount = 0
        var i = 0
        while (i + 1 < pcm.size) {
            // PCM 16-bit little-endian signed — sign-extend via toShort() to get -32768..32767
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toDouble()
            sum += sample * sample
            sampleCount++
            i += 2
        }
        return if (sampleCount > 0) sqrt(sum / sampleCount) else 0.0
    }
}
