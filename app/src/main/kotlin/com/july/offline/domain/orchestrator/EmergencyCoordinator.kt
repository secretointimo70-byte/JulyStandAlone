package com.july.offline.domain.orchestrator

import com.july.offline.core.coroutines.CoroutineDispatchers
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.domain.model.EmergencyTrigger
import com.july.offline.domain.model.SurvivalCategory
import com.july.offline.domain.port.EmergencyTriggerPort
import com.july.offline.domain.state.EmergencyStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyCoordinator @Inject constructor(
    private val emergencyStateHolder: EmergencyStateHolder,
    private val triggerPort: EmergencyTriggerPort,
    private val logger: DiagnosticsLogger,
    private val dispatchers: CoroutineDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var activeConversationJob: Job? = null

    var onCancelConversation: (() -> Unit)? = null
    var onResumeConversation: (() -> Unit)? = null

    fun startListening() {
        triggerPort.voiceTriggerFlow()
            .onEach { activate(EmergencyTrigger.VOICE) }
            .launchIn(scope)
    }

    fun registerActiveJob(job: Job) {
        activeConversationJob = job
    }

    fun isEmergencyActive() = emergencyStateHolder.isActive

    fun activateByButton() = activate(EmergencyTrigger.BUTTON)

    private fun activate(trigger: EmergencyTrigger) {
        if (emergencyStateHolder.isActive) return
        activeConversationJob?.cancel()
        activeConversationJob = null
        onCancelConversation?.invoke()
        emergencyStateHolder.activate(trigger)
        logger.logInfo("EmergencyCoordinator", "Activated by ${trigger.name}")
    }

    fun selectCategory(category: SurvivalCategory) = emergencyStateHolder.selectCategory(category)
    fun clearCategory() = emergencyStateHolder.clearCategory()
    fun advanceStep() = emergencyStateHolder.advanceStep()
    fun previousStep() = emergencyStateHolder.previousStep()

    fun deactivate() {
        emergencyStateHolder.startResolving()
        activeConversationJob = null
        onResumeConversation?.invoke()
        emergencyStateHolder.deactivate()
        logger.logInfo("EmergencyCoordinator", "Deactivated")
    }
}
