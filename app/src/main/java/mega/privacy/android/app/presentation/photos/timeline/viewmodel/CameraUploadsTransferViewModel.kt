package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.photos.model.CameraUploadsTransferType
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.active.MonitorCameraUploadsInProgressTransfersUseCase
import javax.inject.Inject

@HiltViewModel
class CameraUploadsTransferViewModel @Inject constructor(
    private val monitorCameraUploadsInProgressTransfersUseCase: MonitorCameraUploadsInProgressTransfersUseCase,
) : ViewModel() {

    val cameraUploadsTransfers: StateFlow<List<CameraUploadsTransferType>>
        field: MutableStateFlow<List<CameraUploadsTransferType>> = MutableStateFlow(emptyList<CameraUploadsTransferType>())

    init {
        viewModelScope.launch {
            monitorCameraUploadsInProgressTransfersUseCase()
                .map { activeTransfersMap ->
                    val transfers = activeTransfersMap.values.sortedBy { it.priority }
                    val inProgress = transfers.filter { it.state != TransferState.STATE_QUEUED }
                    val inQueue = transfers.filter { it.state == TransferState.STATE_QUEUED }

                    buildList<CameraUploadsTransferType> {
                        if (inProgress.isNotEmpty()) {
                            add(CameraUploadsTransferType.InProgress(inProgress))
                        }
                        if (inQueue.isNotEmpty()) {
                            add(CameraUploadsTransferType.InQueue(inQueue))
                        }
                    }
                }.collectLatest { cuUploads ->
                    cameraUploadsTransfers.update { cuUploads }
                }
        }
    }
}