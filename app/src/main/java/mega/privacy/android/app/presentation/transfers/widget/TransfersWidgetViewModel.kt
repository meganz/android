package mega.privacy.android.app.presentation.transfers.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.model.mapper.TransfersInfoMapper
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.extension.skipUnstable
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorLastTransfersHaveBeenCancelledUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.errorstatus.MonitorTransferInErrorStatusUseCase
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for managing transfers data.
 *
 * @property transfersInfoMapper                [TransfersInfoMapper]
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersWidgetViewModel @Inject constructor(
    private val transfersInfoMapper: TransfersInfoMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorTransfersStatusUseCase: MonitorTransfersStatusUseCase,
    monitorLastTransfersHaveBeenCancelledUseCase: MonitorLastTransfersHaveBeenCancelledUseCase,
    private val monitorTransferInErrorStatusUseCase: MonitorTransferInErrorStatusUseCase,
    private val samplePeriod: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(TransfersWidgetUiState())

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
    }

    private fun monitorFailedTransfers() {
        viewModelScope.launch(ioDispatcher) {
            monitorTransferInErrorStatusUseCase().collect { isInErrorStatus ->
                if (isInErrorStatus != _state.value.isTransferError) {
                    _state.update { state ->
                        if (isInErrorStatus) {
                            state.copy(isTransferError = true)
                        } else {
                            val transfersInfo = state.transfersInfo.copy(
                                status =
                                    if (state.transfersInfo.totalSizeToTransfer == 0L) TransfersStatus.Completed
                                    else TransfersStatus.Transferring
                            )
                            state.copy(transfersInfo = transfersInfo, isTransferError = false)
                        }
                    }
                }
            }
        }
    }

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

    companion object {
        private const val DEFAULT_SAMPLE_PERIOD = 500L
        internal val waitTimeToShowOffline = 30_000L.milliseconds
    }
}
