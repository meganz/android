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
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.GetNumPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.completed.IsCompletedTransfersEmptyUseCase
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingTransfersUseCase      [GetNumPendingTransfersUseCase]
 * @property isCompletedTransfersEmptyUseCase   [IsCompletedTransfersEmptyUseCase]
 * @property transfersInfoMapper                [TransfersInfoMapper]
 * @property transfersManagement                [TransfersManagement]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingTransfersUseCase: GetNumPendingTransfersUseCase,
    private val isCompletedTransfersEmptyUseCase: IsCompletedTransfersEmptyUseCase,
    private val transfersInfoMapper: TransfersInfoMapper,
    private val transfersManagement: TransfersManagement,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorTransfersSize: MonitorTransfersStatusUseCase,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val samplePeriod: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(TransferManagementUiState())
    private val shouldShowCompletedTab = SingleLiveEvent<Boolean>()
    private var lastTransfersStatusInfo = TransfersStatusInfo()

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
            val flow = monitorTransfersSize(
                uploadsWorkerFlag = getFeatureFlagValueUseCase(AppFeatures.UploadWorker),
                activeTransfersInCameraUploadsFlag = getFeatureFlagValueUseCase(AppFeatures.ActiveTransfersInCameraUploads),
            )

            val samplePeriodFinal = samplePeriod ?: DEFAULT_SAMPLE_PERIOD
            if (samplePeriodFinal > 0) {
                flow.sample(samplePeriodFinal)
            } else {
                flow
            }.collect { transfersInfo ->
                lastTransfersStatusInfo = transfersInfo
                updateUiState(transfersInfo)
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
    fun checkTransfersInfo(unHideWidget: Boolean = false) {
        if (unHideWidget) {
            _state.update {
                it.copy(hideTransfersWidget = false)
            }
        }
        updateUiState(lastTransfersStatusInfo)
    }

    /**
     * get pending download and upload
     */
    private fun updateUiState(
        transfersStatusInfo: TransfersStatusInfo,
    ) {
        _state.update {
            it.copy(
                transfersInfo = transfersInfoMapper(
                    numPendingDownloadsNonBackground = transfersStatusInfo.pendingDownloads,
                    numPendingUploads = transfersStatusInfo.pendingUploads,
                    isTransferError = transfersManagement.shouldShowNetworkWarning || transfersManagement.getAreFailedTransfers(),
                    isTransferOverQuota = transfersStatusInfo.transferOverQuota,
                    isStorageOverQuota = transfersStatusInfo.storageOverQuota,
                    areTransfersPaused = transfersStatusInfo.paused,
                    totalSizeTransferred = transfersStatusInfo.totalSizeTransferred,
                    totalSizeToTransfer = transfersStatusInfo.totalSizeToTransfer,
                )
            )
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
