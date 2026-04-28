package com.july.offline.domain.model

sealed class EmergencyState {

    object Inactive : EmergencyState()

    data class Active(
        val trigger: EmergencyTrigger,
        val activeCategory: SurvivalCategory? = null,
        val activeStepIndex: Int = 0
    ) : EmergencyState()

    object Resolving : EmergencyState()
}

enum class EmergencyTrigger { VOICE, BUTTON }
