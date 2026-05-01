package com.july.offline.security.ui

import com.july.offline.security.report.SecurityReport

sealed class SecurityUiState {
    object Idle : SecurityUiState()
    data class Scanning(val message: String, val progress: Int) : SecurityUiState()
    data class Complete(val report: SecurityReport) : SecurityUiState()
    data class Error(val message: String) : SecurityUiState()
}
