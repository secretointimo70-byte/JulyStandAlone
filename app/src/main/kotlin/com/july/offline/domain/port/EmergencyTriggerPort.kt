package com.july.offline.domain.port

import kotlinx.coroutines.flow.Flow

interface EmergencyTriggerPort {
    fun voiceTriggerFlow(): Flow<Unit>
}
