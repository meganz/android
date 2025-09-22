package mega.privacy.android.core.transfers.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.extension.skipUnstable
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorLastTransfersHaveBeenCancelledUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersStatusUseCase
import mega.privacy.android.domain.usecase.transfers.errorstatus.MonitorTransferInErrorStatusUseCase
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetStatus
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for managing transfers data.
 *
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class TransfersToolbarWidgetViewModel @Inject constructor(
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    monitorTransfersStatusUseCase: MonitorTransfersStatusUseCase,
    monitorLastTransfersHaveBeenCancelledUseCase: MonitorLastTransfersHaveBeenCancelledUseCase,
    private val monitorTransferInErrorStatusUseCase: MonitorTransferInErrorStatusUseCase,
    private val samplePeriod: Long?,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TransfersToolabarWidgetUiState())

    /**
     * Transfers info
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUserCredentialsUseCase()
                .catch { Timber.e(it) }
                .collectLatest { credentials ->
                    _state.update { it.copy(isUserLoggedIn = credentials != null) }
                }
        }

        viewModelScope.launch {
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
        viewModelScope.launch {
            monitorLastTransfersHaveBeenCancelledUseCase()
                .catch { Timber.e(it) }
                .collect {
                    _state.update {
                        it.copy(lastTransfersCancelled = true)
                    }
                }
        }
        viewModelScope.launch {
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
        viewModelScope.launch {
            monitorTransferInErrorStatusUseCase().collect { isInErrorStatus ->
                if (isInErrorStatus != _state.value.isTransferError) {
                    _state.update { state ->
                        if (isInErrorStatus) {
                            state.copy(isTransferError = true)
                        } else {
                            state.copy(
                                status = if (state.totalSizeToTransfer == 0L) {
                                    TransfersToolbarWidgetStatus.Completed
                                } else {
                                    TransfersToolbarWidgetStatus.Transferring
                                },
                                isTransferError = false
                            )
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
        val status = with(transfersStatusInfo) {
            if (pendingUploads + pendingDownloads <= 0) {
                when {
                    state.value.isTransferError -> TransfersToolbarWidgetStatus.Error
                    state.value.lastTransfersCancelled -> TransfersToolbarWidgetStatus.Idle
                    else -> TransfersToolbarWidgetStatus.Completed
                }
            } else {
                when {
                    paused -> TransfersToolbarWidgetStatus.Paused
                    (transferOverQuota && (pendingUploads <= 0 || storageOverQuota))
                            || (storageOverQuota && pendingDownloads <= 0) -> TransfersToolbarWidgetStatus.OverQuota

                    state.value.isTransferError || !state.value.isOnline -> TransfersToolbarWidgetStatus.Error
                    else -> TransfersToolbarWidgetStatus.Transferring
                }
            }
        }
        val newLastTransfersCancelled = _state.value.lastTransfersCancelled
                && status == TransfersToolbarWidgetStatus.Idle // new events can indicate not cancelled anymore (new transfers for instance)

        _state.update {
            it.copy(
                totalSizeAlreadyTransferred = transfersStatusInfo.totalSizeTransferred,
                totalSizeToTransfer = transfersStatusInfo.totalSizeToTransfer,
                status = status,
                lastTransfersCancelled = newLastTransfersCancelled
            )
        }
    }

    companion object {
        private const val DEFAULT_SAMPLE_PERIOD = 500L
        internal val waitTimeToShowOffline = 30_000L.milliseconds
    }
}
