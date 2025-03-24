package mega.privacy.android.app.presentation.transfers

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.extension.skipUnstable
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.CompletedTransferState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.GetNumPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorLastTransfersHaveBeenCancelledUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.completed.IsCompletedTransfersEmptyUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for managing transfers data.
 *
 * @property getNumPendingTransfersUseCase      [GetNumPendingTransfersUseCase]
 * @property isCompletedTransfersEmptyUseCase   [IsCompletedTransfersEmptyUseCase]
 * @property transfersInfoMapper                [TransfersInfoMapper]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersManagementViewModel @Inject constructor(
    private val getNumPendingTransfersUseCase: GetNumPendingTransfersUseCase,
    private val isCompletedTransfersEmptyUseCase: IsCompletedTransfersEmptyUseCase,
    private val transfersInfoMapper: TransfersInfoMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorTransfersStatusUseCase: MonitorTransfersStatusUseCase,
    monitorLastTransfersHaveBeenCancelledUseCase: MonitorLastTransfersHaveBeenCancelledUseCase,
    private val monitorCompletedTransfersEventUseCase: MonitorCompletedTransferEventUseCase,
    private val samplePeriod: Long?,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TransferManagementUiState())
    private val shouldShowCompletedTab = SingleLiveEvent<Boolean>()

    /**
     * Transfers info
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val samplePeriodFinal = samplePeriod ?: DEFAULT_SAMPLE_PERIOD
            if (samplePeriodFinal > 0) {
                monitorTransfersStatusUseCase().sample(samplePeriodFinal)
            } else {
                monitorTransfersStatusUseCase()
            }
                .catch { Timber.e(it) }
                .collect { transfersInfo ->
                    updateUiState(transfersInfo)
                }
        }
        viewModelScope.launch(ioDispatcher) {
            monitorLastTransfersHaveBeenCancelledUseCase()
                .catch { Timber.e(it) }
                .collect {
                    _state.update {
                        it.copy(lastTransfersCancelled = true)
                    }
                }
        }
        viewModelScope.launch(ioDispatcher) {
            monitorConnectivityUseCase()
                .skipUnstable(waitTimeToShowOffline) { it }
                .catch { Timber.e(it) }
                .collect { online ->
                    _state.update {
                        it.copy(isOnline = online)
                    }
                }
        }
        monitorFailedTransfers()
        monitorTransferOverQuota()
    }

    private fun monitorFailedTransfers() {
        viewModelScope.launch(ioDispatcher) {
            monitorCompletedTransfersEventUseCase().collect { completedState ->
                if (completedState == CompletedTransferState.Error) {
                    _state.update { state -> state.copy(isTransferError = true) }
                }
            }
        }
    }

    private fun monitorTransferOverQuota() {
        viewModelScope.launch {
            monitorTransferOverQuotaUseCase().collectLatest { isTransferOverQuota ->
                _state.update { state -> state.copy(isTransferOverQuota = isTransferOverQuota) }

                if (isTransferOverQuota) {
                    checkTransferOverQuotaWarning()
                }
            }
        }
    }

    /**
     * Checks if the transfer is over quota.
     */
    fun isTransferOverQuota() = state.value.isTransferOverQuota

    /**
     * Checks if should show transfer errors. If so, updates state as the error is immediately consumed.
     */
    fun shouldCheckTransferError() = state.value.isTransferError.let { isTransferError ->
        if (isTransferError) {
            _state.update { state ->
                val transferInfo = state.transfersInfo.let {
                    if (it.status == TransfersStatus.TransferError) {
                        it.copy(
                            status =
                            if (it.totalSizeToTransfer == 0L) TransfersStatus.Completed
                            else TransfersStatus.Transferring
                        )
                    } else {
                        it
                    }
                }
                state.copy(
                    isTransferError = false,
                    transfersInfo = transferInfo
                )
            }
        }

        isTransferError
    }

    /**
     * Notifies about updates on if should show or not the Completed tab.
     */
    fun onGetShouldCompletedTab(): LiveData<Boolean> = shouldShowCompletedTab

    /**
     * get pending download and upload
     */
    private fun updateUiState(
        transfersStatusInfo: TransfersStatusInfo,
    ) {
        val newTransferInfo = transfersInfoMapper(
            numPendingDownloadsNonBackground = transfersStatusInfo.pendingDownloads,
            numPendingUploads = transfersStatusInfo.pendingUploads,
            isTransferError = state.value.isTransferError,
            isOnline = state.value.isOnline,
            isTransferOverQuota = transfersStatusInfo.transferOverQuota,
            isStorageOverQuota = transfersStatusInfo.storageOverQuota,
            areTransfersPaused = transfersStatusInfo.paused,
            totalSizeTransferred = transfersStatusInfo.totalSizeTransferred,
            totalSizeToTransfer = transfersStatusInfo.totalSizeToTransfer,
            lastTransfersCancelled = _state.value.lastTransfersCancelled,
        )
        val newLastTransfersCancelled = _state.value.lastTransfersCancelled
                && newTransferInfo.status == TransfersStatus.Cancelled // new events can indicate not cancelled anymore (new transfers for instance)
        _state.update {
            it.copy(
                transfersInfo = newTransferInfo,
                lastTransfersCancelled = newLastTransfersCancelled,
            )
        }
    }

    /**
     * Checks if should show the Completed tab or not.
     */
    fun checkIfShouldShowCompletedTab() {
        viewModelScope.launch {
            shouldShowCompletedTab.value = shouldCheckTransferError() ||
                    !isCompletedTransfersEmptyUseCase() && getNumPendingTransfersUseCase() <= 0
        }
    }

    /**
     * Updates UI state to hide the transfers widget. It will be hide regardless of whether there are active transfers or not
     */
    fun hideTransfersWidget() {
        _state.update {
            it.copy(hideTransfersWidget = true)
        }
    }

    /**
     * Updates UI state to show the transfers widget when there are active transfers
     */
    fun showTransfersWidget() {
        _state.update {
            it.copy(hideTransfersWidget = false)
        }
    }

    /**
     * Gets from the UI state if the app is in the transfers section.
     */
    fun isInTransfersSection() = state.value.isInTransfersSection

    /**
     * Updates UI state to indicate that the app is in the transfers section.
     */
    fun setInTransfersSection(isInTransfersSection: Boolean) {
        _state.update {
            it.copy(isInTransfersSection = isInTransfersSection)
        }
    }

    /**
     * Resets the transfer over quota timestamp to the default value.
     */
    fun resetTransferOverQuotaTimestamp() {
        _state.update {
            it.copy(transferOverQuotaTimestamp = INVALID_TIMESTAMP)
        }
    }

    /**
     * Checks if a transfer over quota warning has to be shown.
     * If so, sets the timestamp to the current time to avoid show duplicated transfer over quota warnings.
     */
    private fun checkTransferOverQuotaWarning() {
        if (state.value.transferOverQuotaTimestamp == INVALID_TIMESTAMP
            || state.value.transferOverQuotaTimestamp - System.currentTimeMillis() > WAIT_TIME_TO_SHOW_TRANSFER_OVER_QUOTA_WARNING
        ) {
            _state.update {
                it.copy(
                    transferOverQuotaTimestamp = System.currentTimeMillis(),
                    transferOverQuotaWarning = true
                )
            }
        } else {
            onTransferOverQuotaWarningConsumed()
        }
    }

    /**
     * Consumes the transfer over quota warning.
     */
    fun onTransferOverQuotaWarningConsumed() {
        _state.update {
            it.copy(transferOverQuotaWarning = false)
        }
    }

    companion object {
        internal const val INVALID_TIMESTAMP = -1L
        private const val WAIT_TIME_TO_SHOW_TRANSFER_OVER_QUOTA_WARNING = 60000L
        private const val DEFAULT_SAMPLE_PERIOD = 500L
        internal val waitTimeToShowOffline = 30_000L.milliseconds
    }
}
