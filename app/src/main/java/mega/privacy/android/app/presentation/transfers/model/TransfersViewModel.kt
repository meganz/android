package mega.privacy.android.app.presentation.transfers.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.view.navigation.compose.TransfersArgs
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransfersUiState())
    val uiState = _uiState.asStateFlow()

    private val transfersArgs = TransfersArgs(savedStateHandle)

    init {
        updateSelectedTab(transfersArgs.tabIndex)
        monitorInProgressTransfers()
        monitorStorageOverQuota()
        monitorTransferOverQuota()
        monitorPausedTransfers()
    }

    private fun monitorInProgressTransfers() {
        viewModelScope.launch {
            monitorInProgressTransfersUseCase().collectLatest { inProgressTransfers ->
                _uiState.update { state ->
                    state.copy(
                        inProgressTransfers = inProgressTransfers.values
                            .sortedBy { it.priority }.toImmutableList(),
                    )
                }
            }
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

    /**
     * Pause or resume a transfer by tag.
     */
    fun playOrPauseTransfer(tag: Int) {
        runCatching { uiState.value.inProgressTransfers.first { it.tag == tag }.isPaused }
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
}