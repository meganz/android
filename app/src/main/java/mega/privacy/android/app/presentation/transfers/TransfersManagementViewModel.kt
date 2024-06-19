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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.GetNumPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersSizeUseCase
import mega.privacy.android.domain.usecase.transfers.completed.IsCompletedTransfersEmptyUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreAllTransfersPausedUseCase
import javax.inject.Inject

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingTransfersUseCase      [GetNumPendingTransfersUseCase]
 * @property isCompletedTransfersEmptyUseCase   [IsCompletedTransfersEmptyUseCase]
 * @property areAllTransfersPausedUseCase       [AreAllTransfersPausedUseCase]
 * @property transfersInfoMapper                [TransfersInfoMapper]
 * @property transfersManagement                [TransfersManagement]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
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
    private var lastTransfersSizeInfo = TransfersSizeInfo()

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
                    .onStart {
                        //in invokeLegacy we only receive updates with transfer updates, so we need to force a first update
                        emit(TransfersSizeInfo())
                    }
            }
            val samplePeriodFinal = samplePeriod ?: DEFAULT_SAMPLE_PERIOD
            if (samplePeriodFinal > 0) {
                flow.sample(samplePeriodFinal)
            } else {
                flow
            }.collect { transfersInfo ->
                lastTransfersSizeInfo = transfersInfo
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
        viewModelScope.launch {
            updateUiState(lastTransfersSizeInfo)
        }
    }

    /**
     * get pending download and upload
     */
    private suspend fun updateUiState(
        transfersSizeInfo: TransfersSizeInfo,
    ) {
        val areTransfersPaused = areAllTransfersPausedUseCase()
        _state.update {
            it.copy(
                transfersInfo = transfersInfoMapper(
                    numPendingDownloadsNonBackground = transfersSizeInfo.pendingDownloads,
                    numPendingUploads = transfersSizeInfo.pendingUploads,
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
