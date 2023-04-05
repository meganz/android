package mega.privacy.android.app.presentation.transfers

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.IsCompletedTransfersEmpty
import mega.privacy.android.domain.usecase.MonitorTransfersSize
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfer.BroadcastPausedTransfers
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingDownloadsNonBackground    [GetNumPendingDownloadsNonBackground]
 * @property getNumPendingUploads                   [GetNumPendingUploads]
 * @property getNumPendingTransfers                 [GetNumPendingTransfers]
 * @property isCompletedTransfersEmpty              [IsCompletedTransfersEmpty]
 * @property areAllTransfersPaused                  [AreAllTransfersPaused]
 * @property broadcastPausedTransfers               [BroadcastPausedTransfers]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingDownloadsNonBackground: GetNumPendingDownloadsNonBackground,
    private val getNumPendingUploads: GetNumPendingUploads,
    private val getNumPendingTransfers: GetNumPendingTransfers,
    private val isCompletedTransfersEmpty: IsCompletedTransfersEmpty,
    private val areAllTransfersPaused: AreAllTransfersPaused,
    private val broadcastPausedTransfers: BroadcastPausedTransfers,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorTransfersSize: MonitorTransfersSize,
) : ViewModel() {
    private val _state = MutableStateFlow(TransferManagementUiState())
    private val shouldShowCompletedTab = SingleLiveEvent<Boolean>()

    /**
     * is network connected
     */
    val online = monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Lazily, false)

    /**
     * Transfers info
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorTransfersSize()
                .flowOn(ioDispatcher)
                .sample(500L)
                .collect { transfersInfo ->
                    getPendingDownloadAndUpload(transfersInfo)
                }
        }
    }

    /**
     * Notifies about updates on if should show or not the Completed tab.
     */
    fun onGetShouldCompletedTab(): LiveData<Boolean> = shouldShowCompletedTab

    /**
     * Checks transfers info.
     */
    fun checkTransfersInfo(transferType: TransferType) {
        val transfersInfo = state.value.transfersInfo
        getPendingDownloadAndUpload(
            TransfersSizeInfo(
                transferType = transferType,
                totalSizeTransferred = transfersInfo.totalSizeTransferred,
                totalSizePendingTransfer = transfersInfo.totalSizePendingTransfer,
            )
        )
    }

    /**
     * get pending download and upload
     */
    private fun getPendingDownloadAndUpload(transfersSizeInfo: TransfersSizeInfo) {
        viewModelScope.launch {
            val numPendingDownloadsNonBackground = getNumPendingDownloadsNonBackground()
            val numPendingUploads = getNumPendingUploads()

            _state.update {
                it.copy(
                    transfersInfo = TransfersInfo(
                        transferType = transfersSizeInfo.transferType,
                        numPendingDownloadsNonBackground = numPendingDownloadsNonBackground,
                        numPendingUploads = numPendingUploads,
                        areTransfersPaused = areAllTransfersPaused(),
                        totalSizeTransferred = transfersSizeInfo.totalSizeTransferred,
                        totalSizePendingTransfer = transfersSizeInfo.totalSizePendingTransfer
                    )
                )
            }
        }
    }

    /**
     * Checks if should show the Completed tab or not.
     */
    fun checkIfShouldShowCompletedTab() {
        viewModelScope.launch {
            shouldShowCompletedTab.value =
                !isCompletedTransfersEmpty() && getNumPendingTransfers() <= 0
        }
    }

    /**
     * Checks if transfers are paused.
     */
    fun checkTransfersState() = viewModelScope.launch {
        areAllTransfersPaused().let { paused ->
            _state.update { it.copy(transfersInfo = it.transfersInfo.copy(areTransfersPaused = paused)) }
            if (paused) {
                broadcastPausedTransfers()
            }
        }
    }

    /**
     * Are transfers paused
     */
    val areTransfersPaused: Boolean
        get() = state.value.transfersInfo.areTransfersPaused
}