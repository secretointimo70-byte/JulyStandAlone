package com.july.offline.survival

import com.july.offline.domain.port.EmergencyTriggerPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class NoOpEmergencyTriggerPort @Inject constructor() : EmergencyTriggerPort {
    override fun voiceTriggerFlow(): Flow<Unit> = emptyFlow()
}
