package mega.privacy.android.app.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.providers.FileProviderActivity]
 */
@HiltViewModel
class FileProviderViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileProviderUiState())

    /**
     * Ui state flow
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Stop camera uploads
     */
    fun stopCameraUploads() = viewModelScope.launch {
        runCatching { stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable) }
            .onFailure { Timber.d(it) }
    }

    /**
     * Is connected
     */
    fun isConnected() = isConnectedToInternetUseCase()

    /**
     * Start downloading the selected nodes
     */
    fun startDownload(nodeIds: List<NodeId>) {
        viewModelScope.launch {
            val nodes = nodeIds.mapNotNull { getNodeByIdUseCase(it) }
            val triggerEvent = TransferTriggerEvent.StartDownloadNode(nodes, true)
            _uiState.update {
                it.copy(startDownloadEvent = triggered(triggerEvent))
            }
        }
    }

    /**
     * Consume transfer trigger event
     */
    fun consumeTransferTriggerEvent() {
        _uiState.update {
            it.copy(startDownloadEvent = consumed())
        }
    }
}