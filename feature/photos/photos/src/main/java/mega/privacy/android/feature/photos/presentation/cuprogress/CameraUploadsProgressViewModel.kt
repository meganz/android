package mega.privacy.android.feature.photos.presentation.cuprogress

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.CameraUploadsTransferType
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorCameraUploadsInProgressTransfersUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class CameraUploadsProgressViewModel @Inject constructor(
    monitorCameraUploadsInProgressTransfersUseCase: MonitorCameraUploadsInProgressTransfersUseCase,
    monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
) : ViewModel() {

    private val transferItemStateFlowMap =
        ConcurrentHashMap<Int, MutableStateFlow<CameraUploadsTransferItemUiState>>()

    private val cuInProgressTransferFlow =
        monitorCameraUploadsInProgressTransfersUseCase().onStart {
            emit(emptyMap())
        }

    private val cuStatusInfoFlow = monitorCameraUploadsStatusInfoUseCase().onStart {
        emit(CameraUploadsStatusInfo.Unknown)
    }

    internal val uiState: StateFlow<CameraUploadsProgressUiState> by lazy {
        cameraUploadsProgressUiState().asUiStateFlow(
            scope = viewModelScope,
            initialValue = CameraUploadsProgressUiState()
        )
    }

    private fun cameraUploadsProgressUiState(): Flow<CameraUploadsProgressUiState> = combine(
        flow = cuInProgressTransferFlow,
        flow2 = cuStatusInfoFlow,
        transform = ::Pair
    ).map { (cuInProgressTransfers, cuStatusInfo) ->
        buildCameraUploadsProgressUiState(
            inProgressTransfers = cuInProgressTransfers,
            statusInfo = cuStatusInfo
        )
    }

    private fun buildCameraUploadsProgressUiState(
        inProgressTransfers: Map<Long, InProgressTransfer>,
        statusInfo: CameraUploadsStatusInfo,
    ): CameraUploadsProgressUiState {
        val transfers = inProgressTransfers.toCUTransferTypes()
        val isLoading: Boolean
        var pendingCount = 0
        when (statusInfo) {
            is CameraUploadsStatusInfo.CheckFilesForUpload -> {
                isLoading = transfers.isEmpty()
            }

            is CameraUploadsStatusInfo.UploadProgress -> {
                isLoading = transfers.isEmpty()
                pendingCount = statusInfo.totalToUpload - statusInfo.totalUploaded
            }

            else -> isLoading = false
        }
        return CameraUploadsProgressUiState(
            isLoading = isLoading,
            transfers = transfers,
            pendingCount = pendingCount
        )
    }

    private fun Map<Long, InProgressTransfer>.toCUTransferTypes(): List<CameraUploadsTransferType> {
        val transfers = values.sortedBy { it.priority }
        val inProgress = transfers.filter { it.state != TransferState.STATE_QUEUED }
        val inQueue = transfers.filter { it.state == TransferState.STATE_QUEUED }
        return buildList {
            if (inProgress.isNotEmpty()) {
                add(CameraUploadsTransferType.InProgress(inProgress))
            }
            if (inQueue.isNotEmpty()) {
                add(CameraUploadsTransferType.InQueue(inQueue))
            }
        }
    }

    internal fun getTransferItemUiState(id: Int): StateFlow<CameraUploadsTransferItemUiState> =
        getTransferMutableItem(id = id)

    internal fun addTransfer(transfer: InProgressTransfer) {
        if (transfer is InProgressTransfer.Download) {
            addNodeTransfer(
                id = transfer.tag,
                fileName = transfer.fileName,
                nodeId = transfer.nodeId
            )
        } else {
            addFileTransfer(
                id = transfer.tag,
                fileName = transfer.fileName,
                localPath = (transfer as InProgressTransfer.Upload).localPath
            )
        }
    }

    private fun addNodeTransfer(id: Int, fileName: String, nodeId: NodeId) {
        viewModelScope.launch {
            val fileTypeResId = getFileTypeIcon(fileName)

            getTransferMutableItem(id = id).update { state ->
                state.copy(fileTypeResId = fileTypeResId)
            }

            runCatching { getThumbnailUseCase(nodeId = nodeId.longValue, allowThrow = true) }
                .onSuccess { thumbnail ->
                    getTransferMutableItem(id = id).update { state ->
                        state.copy(previewUri = thumbnail?.toUri())
                    }
                }.onFailure { Timber.w(it) }
        }
    }

    private fun addFileTransfer(id: Int, fileName: String, localPath: String) {
        viewModelScope.launch {
            val fileTypeResId = getFileTypeIcon(fileName)
            getTransferMutableItem(id = id).update { state ->
                state.copy(
                    fileTypeResId = fileTypeResId,
                    previewUri = localPath.toUri()
                )
            }
        }
    }

    private fun getFileTypeIcon(fileName: String) =
        fileTypeIconMapper(fileName.substringAfterLast('.', ""))

    private fun getTransferMutableItem(id: Int): MutableStateFlow<CameraUploadsTransferItemUiState> =
        transferItemStateFlowMap.getOrPut(id) {
            MutableStateFlow(CameraUploadsTransferItemUiState())
        }
}
