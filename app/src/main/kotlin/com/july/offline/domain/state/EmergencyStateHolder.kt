package com.july.offline.domain.state

import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.EmergencyState
import com.july.offline.domain.model.EmergencyTrigger
import com.july.offline.domain.model.SurvivalCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyStateHolder @Inject constructor(
    private val logger: DiagnosticsLogger
) {
    private val _emergencyState = MutableStateFlow<EmergencyState>(EmergencyState.Inactive)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState.asStateFlow()

    val isActive: Boolean get() = _emergencyState.value is EmergencyState.Active

    internal fun activate(trigger: EmergencyTrigger) {
        _emergencyState.value = EmergencyState.Active(trigger = trigger)
        logger.logStateTransition("Inactive", "Active", "emergency_${trigger.name.lowercase()}")
    }

    internal fun selectCategory(category: SurvivalCategory) {
        val current = _emergencyState.value as? EmergencyState.Active ?: return
        _emergencyState.value = current.copy(activeCategory = category, activeStepIndex = 0)
    }

    internal fun advanceStep() {
        val current = _emergencyState.value as? EmergencyState.Active ?: return
        _emergencyState.value = current.copy(activeStepIndex = current.activeStepIndex + 1)
    }

    internal fun previousStep() {
        val current = _emergencyState.value as? EmergencyState.Active ?: return
        if (current.activeStepIndex > 0) {
            _emergencyState.value = current.copy(activeStepIndex = current.activeStepIndex - 1)
        }
    }

    internal fun clearCategory() {
        val current = _emergencyState.value as? EmergencyState.Active ?: return
        _emergencyState.value = current.copy(activeCategory = null, activeStepIndex = 0)
    }

    internal fun startResolving() {
        _emergencyState.value = EmergencyState.Resolving
        logger.logStateTransition("Active", "Resolving", "user_exit")
    }

    internal fun deactivate() {
        _emergencyState.value = EmergencyState.Inactive
        logger.logStateTransition("Resolving", "Inactive", "restored")
    }
}
