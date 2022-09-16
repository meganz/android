package mega.privacy.android.app.presentation.transfers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.IsCompletedTransfersEmpty
import mega.privacy.android.domain.usecase.MonitorTransfersSize
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingDownloadsNonBackground    [GetNumPendingDownloadsNonBackground]
 * @property getNumPendingUploads                   [GetNumPendingUploads]
 * @property getNumPendingTransfers                 [GetNumPendingTransfers]
 * @property isCompletedTransfersEmpty              [IsCompletedTransfersEmpty]
 * @property areAllTransfersPaused                  [AreAllTransfersPaused]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingDownloadsNonBackground: GetNumPendingDownloadsNonBackground,
    private val getNumPendingUploads: GetNumPendingUploads,
    private val getNumPendingTransfers: GetNumPendingTransfers,
    private val isCompletedTransfersEmpty: IsCompletedTransfersEmpty,
    private val areAllTransfersPaused: AreAllTransfersPaused,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorTransfersSize: MonitorTransfersSize,
) : ViewModel() {

    private val transfersInfo: MutableLiveData<TransfersInfo> = MutableLiveData()
    private val shouldShowCompletedTab = SingleLiveEvent<Boolean>()
    private val areTransfersPaused = SingleLiveEvent<Boolean>()
    private val transfersSizeInfoState = MutableStateFlow(TransfersSizeInfo())

    init {
        viewModelScope.launch {
            monitorTransfersSize()
                .flowOn(ioDispatcher)
                .sample(500L)
                .collect { transfersInfo ->
                    transfersSizeInfoState.value = transfersInfo
                    getPendingDownloadAndUpload(transfersInfo)
                }
        }
    }

    /**
     * Notifies about updates on Transfers info.
     */
    fun onTransfersInfoUpdate(): LiveData<TransfersInfo> = transfersInfo

    /**
     * Notifies about updates on if should show or not the Completed tab.
     */
    fun onGetShouldCompletedTab(): LiveData<Boolean> = shouldShowCompletedTab

    /**
     * Notifies about the transfers state
     */
    fun onGetTransfersState(): LiveData<Boolean> = areTransfersPaused

    /**
     * Checks transfers info.
     */
    fun checkTransfersInfo(transferType: TransferType) {
        getPendingDownloadAndUpload(transfersSizeInfoState.value.copy(transferType = transferType))
    }

    /**
     * get pending download and upload
     */
    private fun getPendingDownloadAndUpload(transfersSizeInfo: TransfersSizeInfo) {
        viewModelScope.launch {
            val numPendingDownloadsNonBackground = getNumPendingDownloadsNonBackground()
            val numPendingUploads = getNumPendingUploads()

            transfersInfo.value =
                TransfersInfo(
                    transferType = transfersSizeInfo.transferType,
                    numPendingDownloadsNonBackground = numPendingDownloadsNonBackground,
                    numPendingUploads = numPendingUploads,
                    areTransfersPaused = areAllTransfersPaused(),
                    totalSizeTransferred = transfersSizeInfo.totalSizeTransferred,
                    totalSizePendingTransfer = transfersSizeInfo.totalSizePendingTransfer
                )
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
    fun checkTransfersState() {
        viewModelScope.launch { areTransfersPaused.value = areAllTransfersPaused() }
    }
}