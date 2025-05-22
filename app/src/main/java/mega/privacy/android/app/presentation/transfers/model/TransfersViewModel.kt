package mega.privacy.android.app.presentation.transfers.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.matchOrderWithNewAtEnd
import mega.privacy.android.app.extensions.moveElement
import mega.privacy.android.app.presentation.transfers.view.navigation.TransfersInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Transfers screen.
 *
 * @property uiState [TransfersUiState] for UI state.
 */
@HiltViewModel
class TransfersViewModel @Inject constructor(
    private val monitorInProgressTransfersUseCase: MonitorInProgressTransfersUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val pauseTransferByTagUseCase: PauseTransferByTagUseCase,
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase,
    private val cancelTransfersUseCase: CancelTransfersUseCase,
    private val monitorCompletedTransfersUseCase: MonitorCompletedTransfersUseCase,
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase,
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase,
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransfersUiState())
    val uiState = _uiState.asStateFlow()

    private val transfersInfo = savedStateHandle.toRoute<TransfersInfo>()

    init {
        updateSelectedTab(transfersInfo.tabIndex)
        monitorActiveTransfers()
        monitorStorageOverQuota()
        monitorTransferOverQuota()
        monitorPausedTransfers()
        monitorCompletedTransfers()
    }

    private fun monitorActiveTransfers() {
        viewModelScope.launch {
            monitorInProgressTransfersUseCase().collectLatest { activeTransfersMap ->
                val sortedTransfers = activeTransfersMap.values.sortedBy { it.priority }
                setTransfers(
                    if (reordering) {
                        sortedTransfers.matchOrderWithNewAtEnd(uiState.value.activeTransfers) { it.tag }
                    } else {
                        sortedTransfers
                    }
                )
            }
        }
    }

    private fun setTransfers(activeTransfers: Collection<InProgressTransfer>) {
        _uiState.update { state ->
            state.copy(
                activeTransfers = activeTransfers.toImmutableList(),
            )
        }
    }

    private fun monitorStorageOverQuota() {
        viewModelScope.launch {
            monitorStorageStateEventUseCase()
                .collectLatest { storageState ->
                    _uiState.update { state ->
                        state.copy(
                            isStorageOverQuota = storageState.storageState == StorageState.Red
                                    || storageState.storageState == StorageState.PayWall,
                        )
                    }
                }
        }
    }

    private fun monitorTransferOverQuota() {
        viewModelScope.launch {
            monitorTransferOverQuotaUseCase()
                .collectLatest { isTransferOverQuota ->
                    _uiState.update { state ->
                        state.copy(isTransferOverQuota = isTransferOverQuota)
                    }
                }
        }
    }

    private fun monitorPausedTransfers() {
        viewModelScope.launch {
            monitorPausedTransfersUseCase().collectLatest { areTransfersPaused ->
                _uiState.update { state ->
                    state.copy(areTransfersPaused = areTransfersPaused)
                }
            }
        }
    }

    private fun monitorCompletedTransfers() {
        viewModelScope.launch {
            monitorCompletedTransfersUseCase().collectLatest { completedTransfers ->
                val (completed, failed) = completedTransfers.partition { it.state == MegaTransfer.STATE_COMPLETED }
                _uiState.update { state ->
                    state.copy(
                        completedTransfers = completed
                            .sortedByDescending { it.timestamp }.toImmutableList(),
                        failedTransfers = failed
                            .sortedByDescending { it.timestamp }.toImmutableList(),
                    )
                }
            }
        }
    }

    /**
     * Pause or resume a transfer by tag.
     */
    fun playOrPauseTransfer(tag: Int) {
        runCatching { uiState.value.activeTransfers.first { it.tag == tag }.isPaused }
            .getOrNull()?.let { isPaused ->
                viewModelScope.launch {
                    runCatching { pauseTransferByTagUseCase(tag, !isPaused) }
                        .onFailure { Timber.e(it) }
                }
            }
    }

    /**
     * Resume transfers queue.
     */
    fun resumeTransfersQueue() {
        pauseTransfersQueue(pause = false)
    }

    /**
     * Pause transfers queue.
     */
    fun pauseTransfersQueue() {
        pauseTransfersQueue(pause = true)
    }

    private fun pauseTransfersQueue(pause: Boolean) {
        viewModelScope.launch {
            runCatching { pauseTransfersQueueUseCase(pause) }
                .onSuccess { paused ->
                    _uiState.update { state ->
                        state.copy(areTransfersPaused = paused)
                    }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Update selected tab.
     */
    fun updateSelectedTab(tabIndex: Int) {
        _uiState.update { state ->
            state.copy(selectedTab = tabIndex)
        }
    }

    /**
     * Cancel all transfers.
     */
    fun cancelAllTransfers() {
        viewModelScope.launch {
            runCatching { cancelTransfersUseCase() }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Retry all failed transfers.
     */
    fun retryAllFailedTransfers() {
        viewModelScope.launch {
            runCatching {

            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Clear all failed transfers.
     */
    fun clearAllFailedTransfers() {
        viewModelScope.launch {
            runCatching {

            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Clear all completed transfers.
     */
    fun clearAllCompletedTransfers() {
        viewModelScope.launch {
            runCatching {

            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Active transfers are being actively reordered, but still not confirmed.
     * Transfers are reordered in the ui-state but not in the SDK
     * @param fromIndex the index of the transfers that has been moved
     * @param toIndex the index that the transfer should be moved to
     */
    fun onActiveTransfersReorderPreview(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in uiState.value.activeTransfers.indices || toIndex !in uiState.value.activeTransfers.indices) {
            Timber.e(IndexOutOfBoundsException("Reordering indices are not correct: $fromIndex to $toIndex should be in ${uiState.value.activeTransfers.indices}"))
            return
        }
        reordering = true
        setTransfers(
            uiState.value.activeTransfers.toMutableList().moveElement(fromIndex, toIndex)
        )
    }

    /**
     * Active transfers reorder is confirmed. The change will be send to the SDK
     * @param transfer the transfer that has been confirmed, uiState position of this transfer will be send to the SDK
     */
    fun onActiveTransfersReorderConfirmed(transfer: InProgressTransfer) {
        reordering = false
        uiState.value.activeTransfers.indexOf(transfer).takeIf { it >= 0 }
            ?.let { destinationIndex ->
                viewModelScope.launch {
                    runCatching {
                        if (destinationIndex == 0) {
                            moveTransferToFirstByTagUseCase(transfer.tag)
                        } else if (destinationIndex >= uiState.value.activeTransfers.lastIndex) {
                            moveTransferToLastByTagUseCase(transfer.tag)
                        } else {
                            uiState.value.activeTransfers.getOrNull(destinationIndex + 1)?.tag?.let { prevTag ->
                                moveTransferBeforeByTagUseCase(
                                    tag = transfer.tag,
                                    prevTag = prevTag
                                )
                            }
                        }
                    }.onFailure {
                        Timber.d(it, "Error reordering active transfers")
                    }
                    reordering = false
                }
            }
    }

    //internal value to preserve previous priority while dragged changes are not send to SDK yet
    private var reordering = false
}