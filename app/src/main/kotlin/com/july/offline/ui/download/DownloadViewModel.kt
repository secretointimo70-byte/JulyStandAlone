package com.july.offline.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.july.offline.download.DownloadStatus
import com.july.offline.download.DownloadableModel
import com.july.offline.download.DownloadableModels
import com.july.offline.download.ModelDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelDownloadUiState(
    val model: DownloadableModel,
    val status: DownloadStatus,
    val activeDownloadId: Long? = null
)

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadManager: ModelDownloadManager
) : ViewModel() {

    private val _states = MutableStateFlow(buildInitialStates())
    val states: StateFlow<List<ModelDownloadUiState>> = _states.asStateFlow()

    private val activeDownloads = mutableMapOf<String, Long>()

    init {
        startPolling()
    }

    private fun buildInitialStates() = DownloadableModels.all.map { model ->
        ModelDownloadUiState(
            model = model,
            status = if (downloadManager.isInstalled(model.filename))
                DownloadStatus.Installed else DownloadStatus.NotInstalled
        )
    }

    fun startDownload(model: DownloadableModel) {
        val id = downloadManager.startDownload(model)
        activeDownloads[model.id] = id
        updateState(model.id) { it.copy(status = DownloadStatus.Pending, activeDownloadId = id) }
    }

    fun cancelDownload(model: DownloadableModel) {
        val id = activeDownloads[model.id] ?: return
        downloadManager.cancel(id)
        activeDownloads.remove(model.id)
        updateState(model.id) { it.copy(status = DownloadStatus.NotInstalled, activeDownloadId = null) }
    }

    fun refresh() {
        _states.value = _states.value.map { state ->
            val installed = downloadManager.isInstalled(state.model.filename)
            val activeId = activeDownloads[state.model.id]
            val status = when {
                installed -> DownloadStatus.Installed
                activeId != null -> downloadManager.queryStatus(activeId)
                else -> DownloadStatus.NotInstalled
            }
            state.copy(status = status, activeDownloadId = activeId)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                // Clean up completed/failed downloads from active map
                activeDownloads.entries.removeIf { (_, id) ->
                    val s = downloadManager.queryStatus(id)
                    s is DownloadStatus.Installed || s is DownloadStatus.Failed || s is DownloadStatus.NotInstalled
                }
                delay(1_500L)
            }
        }
    }

    private fun updateState(modelId: String, update: (ModelDownloadUiState) -> ModelDownloadUiState) {
        _states.value = _states.value.map { if (it.model.id == modelId) update(it) else it }
    }
}
