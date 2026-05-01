package com.july.offline.security.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.security.SecurityModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityModule: SecurityModule
) : ViewModel() {

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    fun runFullAudit() = launch("auditoría completa") {
        securityModule.runFullAudit { msg, progress ->
            _uiState.value = SecurityUiState.Scanning(msg, progress)
        }
    }

    fun runAppAudit() = launch("auditoría de app") { securityModule.runAppAudit() }

    fun runDeviceAudit() = launch("auditoría de dispositivo") { securityModule.runDeviceAudit() }

    fun runNetworkAudit() = launch("escaneo de red") {
        securityModule.runNetworkAudit { msg, done, total ->
            _uiState.value = SecurityUiState.Scanning(msg, done * 100 / total.coerceAtLeast(1))
        }
    }

    fun reset() { _uiState.value = SecurityUiState.Idle }

    private fun launch(label: String, block: suspend () -> com.july.offline.security.report.SecurityReport) {
        viewModelScope.launch {
            _uiState.value = SecurityUiState.Scanning("Iniciando $label...", 0)
            try {
                val report = block()
                _uiState.value = SecurityUiState.Complete(report)
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error("Error: ${e.message}")
            }
        }
    }
}
