package com.july.offline

import android.app.Application
import com.july.offline.core.logging.DiagnosticsLogger
import com.july.offline.core.memory.ModelMemoryManager
import com.july.offline.data.db.SurvivalContentSeeder
import com.july.offline.domain.orchestrator.EmergencyCoordinator
import com.july.offline.domain.orchestrator.EngineHealthMonitor
import com.july.offline.lifecycle.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JulyApplication : Application() {

    @Inject lateinit var engineHealthMonitor: EngineHealthMonitor
    @Inject lateinit var diagnosticsLogger: DiagnosticsLogger
    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    @Inject lateinit var modelMemoryManager: ModelMemoryManager
    @Inject lateinit var emergencyCoordinator: EmergencyCoordinator
    @Inject lateinit var survivalContentSeeder: SurvivalContentSeeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        engineHealthMonitor.startMonitoring()
        appLifecycleObserver.register()
        diagnosticsLogger.pruneOldLogs()
        emergencyCoordinator.startListening()
        appScope.launch { survivalContentSeeder.seedIfEmpty() }
    }
}
