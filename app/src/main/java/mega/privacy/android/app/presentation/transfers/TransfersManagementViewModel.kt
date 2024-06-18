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
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.TransfersStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.GetNumPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersSizeUseCase
import mega.privacy.android.domain.usecase.transfers.completed.IsCompletedTransfersEmptyUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreAllTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetNumPendingUploadsUseCase
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingDownloadsNonBackgroundUseCase    [GetNumPendingDownloadsNonBackgroundUseCase]
 * @property getNumPendingUploadsUseCase                   [GetNumPendingUploadsUseCase]
 * @property getNumPendingTransfersUseCase                 [GetNumPendingTransfersUseCase]
 * @property isCompletedTransfersEmptyUseCase       [IsCompletedTransfersEmptyUseCase]
 * @property areAllTransfersPausedUseCase                  [AreAllTransfersPausedUseCase]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingDownloadsNonBackgroundUseCase: GetNumPendingDownloadsNonBackgroundUseCase,
    private val getNumPendingUploadsUseCase: GetNumPendingUploadsUseCase,
    private val getNumPendingTransfersUseCase: GetNumPendingTransfersUseCase,
    private val isCompletedTransfersEmptyUseCase: IsCompletedTransfersEmptyUseCase,
    private val areAllTransfersPausedUseCase: AreAllTransfersPausedUseCase,
    private val transfersInfoMapper: TransfersInfoMapper,
    private val transfersManagement: TransfersManagement,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorTransfersSize: MonitorTransfersSizeUseCase,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val samplePeriod: Long?,
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
        viewModelScope.launch(ioDispatcher) {
            val flow = if (getFeatureFlagValueUseCase(AppFeatures.UploadWorker)) {
                monitorTransfersSize()
            } else {
                monitorTransfersSize.invokeLegacy()
            }
            val samplePeriodFinal = samplePeriod ?: DEFAULT_SAMPLE_PERIOD
            if (samplePeriodFinal > 0) {
                flow.sample(samplePeriodFinal)
            } else {
                flow
            }.collect { transfersInfo ->
                updateUiState(transfersInfo)
            }
        }
        checkTransfersState()
    }

    /**
     * Notifies about updates on if should show or not the Completed tab.
     */
    fun onGetShouldCompletedTab(): LiveData<Boolean> = shouldShowCompletedTab

    /**
     * Checks transfers info.
     */
    fun checkTransfersInfo(unHideWidget: Boolean) {
        val transfersInfo = _state.value.transfersInfo
        updateUiState(
            TransfersSizeInfo(
                totalSizeTransferred = transfersInfo.totalSizeAlreadyTransferred,
                totalSizeToTransfer = transfersInfo.totalSizeToTransfer,
            ), unHideWidget
        )
    }

    /**
     * get pending download and upload
     */
    private fun updateUiState(
        transfersSizeInfo: TransfersSizeInfo,
        unHideWidget: Boolean = false,
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(hideTransfersWidget = if (unHideWidget) false else it.hideTransfersWidget)
            }
            val numPendingDownloadsNonBackground =
                transfersSizeInfo.pendingDownloads ?: getNumPendingDownloadsNonBackgroundUseCase()
            val numPendingUploads =
                transfersSizeInfo.pendingUploads ?: getNumPendingUploadsUseCase()
            val areTransfersPaused = areAllTransfersPausedUseCase()
            _state.update {
                it.copy(
                    transfersInfo = transfersInfoMapper(
                        numPendingDownloadsNonBackground = numPendingDownloadsNonBackground,
                        numPendingUploads = numPendingUploads,
                        isTransferError = transfersManagement.shouldShowNetworkWarning || transfersManagement.getAreFailedTransfers(),
                        isTransferOverQuota = false,
                        isStorageOverQuota = false,
                        areTransfersPaused = areTransfersPaused,
                        totalSizeTransferred = transfersSizeInfo.totalSizeTransferred,
                        totalSizeToTransfer = transfersSizeInfo.totalSizeToTransfer,
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
                !isCompletedTransfersEmptyUseCase() && getNumPendingTransfersUseCase() <= 0
        }
    }

    /**
     * Checks if transfers are paused.
     */
    fun checkTransfersState() = viewModelScope.launch {
        areAllTransfersPausedUseCase().let { paused ->
            if (paused) {
                _state.update { it.copy(transfersInfo = it.transfersInfo.copy(status = TransfersStatus.Paused)) }
            } else {
                checkTransfersInfo(false)
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

    companion object {
        private const val DEFAULT_SAMPLE_PERIOD = 500L
    }
}
