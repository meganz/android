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
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.TransfersStatus
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfer.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.MonitorTransfersSize
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfer.BroadcastPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.IsCompletedTransfersEmptyUseCase
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingDownloadsNonBackgroundUseCase    [GetNumPendingDownloadsNonBackgroundUseCase]
 * @property getNumPendingUploads                   [GetNumPendingUploads]
 * @property getNumPendingTransfers                 [GetNumPendingTransfers]
 * @property isCompletedTransfersEmptyUseCase       [IsCompletedTransfersEmptyUseCase]
 * @property areAllTransfersPaused                  [AreAllTransfersPaused]
 * @property broadcastPausedTransfersUseCase               [BroadcastPausedTransfersUseCase]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingDownloadsNonBackgroundUseCase: GetNumPendingDownloadsNonBackgroundUseCase,
    private val getNumPendingUploads: GetNumPendingUploads,
    private val getNumPendingTransfers: GetNumPendingTransfers,
    private val isCompletedTransfersEmptyUseCase: IsCompletedTransfersEmptyUseCase,
    private val areAllTransfersPaused: AreAllTransfersPaused,
    private val broadcastPausedTransfersUseCase: BroadcastPausedTransfersUseCase,
    private val transfersInfoMapper: TransfersInfoMapper,
    private val transfersManagement: TransfersManagement,
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
        checkTransfersState(false)
    }

    /**
     * Notifies about updates on if should show or not the Completed tab.
     */
    fun onGetShouldCompletedTab(): LiveData<Boolean> = shouldShowCompletedTab

    /**
     * Checks transfers info.
     */
    fun checkTransfersInfo(transferType: TransferType, unHideWidget: Boolean) {
        val transfersInfo = _state.value.transfersInfo
        getPendingDownloadAndUpload(
            TransfersSizeInfo(
                transferType = transferType,
                totalSizeTransferred = transfersInfo.totalSizeTransferred,
                totalSizePendingTransfer = transfersInfo.totalSizePendingTransfer,
            ), unHideWidget
        )
    }

    /**
     * get pending download and upload
     */
    private fun getPendingDownloadAndUpload(
        transfersSizeInfo: TransfersSizeInfo,
        unHideWidget: Boolean = false,
    ) {
        viewModelScope.launch {
            val numPendingDownloadsNonBackground = getNumPendingDownloadsNonBackgroundUseCase()
            val numPendingUploads = getNumPendingUploads()
            val areTransfersPaused = areAllTransfersPaused()
            _state.update {
                it.copy(
                    hideTransfersWidget = if (unHideWidget) false else it.hideTransfersWidget,
                    transfersInfo = transfersInfoMapper(
                        transferType = transfersSizeInfo.transferType,
                        numPendingDownloadsNonBackground = numPendingDownloadsNonBackground,
                        numPendingUploads = numPendingUploads,
                        isTransferError = transfersManagement.shouldShowNetworkWarning || transfersManagement.getAreFailedTransfers(),
                        isTransferOverQuota = false,
                        isStorageOverQuota = false,
                        areTransfersPaused = areTransfersPaused,
                        totalSizeTransferred = transfersSizeInfo.totalSizeTransferred,
                        totalSizePendingTransfer = transfersSizeInfo.totalSizePendingTransfer,
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
                !isCompletedTransfersEmptyUseCase() && getNumPendingTransfers() <= 0
        }
    }

    /**
     * Checks if transfers are paused.
     */
    fun checkTransfersState(shouldBroadcast: Boolean = true) = viewModelScope.launch {
        areAllTransfersPaused().let { paused ->
            if (paused) {
                _state.update { it.copy(transfersInfo = it.transfersInfo.copy(status = TransfersStatus.Paused)) }
                if (shouldBroadcast) {
                    broadcastPausedTransfersUseCase()
                }
            } else {
                checkTransfersInfo(TransferType.NONE, false)
            }
        }
    }

    /**
     * updates UI state to hide the transfers widget
     */
    fun hideTransfersWidget() {
        _state.update {
            it.copy(hideTransfersWidget = true)
        }
    }

    /**
     * Are transfers paused
     */
    val areTransfersPaused: Boolean
        get() = _state.value.transfersInfo.status == TransfersStatus.Paused
}
