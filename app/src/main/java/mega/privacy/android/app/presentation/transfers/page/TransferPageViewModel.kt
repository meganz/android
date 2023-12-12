package mega.privacy.android.app.presentation.transfers.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.DeleteFailedOrCanceledTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteAllCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseAllTransfersUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class TransferPageViewModel @Inject constructor(
    private val pauseAllTransfersUseCase: PauseAllTransfersUseCase,
    private val cancelTransfersUseCase: CancelTransfersUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val deleteAllCompletedTransfersUseCase: DeleteAllCompletedTransfersUseCase,
    private val deleteFailedOrCanceledTransfersUseCase: DeleteFailedOrCanceledTransfersUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TransferPageUiState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    private var pauseOrResumeJob: Job? = null

    /**
     * Set the current transfers tab to the UI state
     *
     * @param tab transfer tab to set
     */
    fun setTransfersTab(tab: TransfersTab) {
        _state.update { it.copy(transfersTab = tab) }
    }

    /**
     * Transfer tab
     */
    val transferTab: TransfersTab
        get() = state.value.transfersTab

    /**
     * Pause or resume transfers
     *
     * @param isPause
     */
    fun pauseOrResumeTransfers(isPause: Boolean) {
        if (pauseOrResumeJob?.isActive == true) return
        pauseOrResumeJob = viewModelScope.launch {
            val result = runCatching {
                pauseAllTransfersUseCase(isPause)
            }
            _state.update { it.copy(pauseOrResultResult = result) }
        }
    }

    /**
     * Cancels all transfers, uploads and downloads
     */
    fun cancelAllTransfers() {
        viewModelScope.launch {
            runCatching {
                cancelTransfersUseCase()
            }.onSuccess {
                _state.update { it.copy(cancelTransfersResult = Result.success(Unit)) }
            }.onFailure { error ->
                _state.update { it.copy(cancelTransfersResult = Result.failure(error)) }
                Timber.e(error)
            }
        }
    }

    /**
     * Stop camera uploads
     */
    fun stopCameraUploads() = viewModelScope.launch {
        runCatching { stopCameraUploadsUseCase(restartMode = CameraUploadsRestartMode.StopAndDisable) }
            .onFailure { Timber.d(it) }
    }

    /**
     * Resets the state of the cancelTransfersResult
     */
    fun onCancelTransfersResultConsumed() =
        _state.update { state -> state.copy(cancelTransfersResult = null) }

    fun deleteAllCompletedTransfers() {
        viewModelScope.launch {
            val result = runCatching { deleteAllCompletedTransfersUseCase() }
            _state.update { state -> state.copy(deleteAllCompletedTransfersResult = result) }
        }
    }

    /**
     * Delete failed or cancelled transfer
     */
    fun deleteFailedOrCancelledTransfers() {
        viewModelScope.launch {
            val result = runCatching { deleteFailedOrCanceledTransfersUseCase() }
            _state.update { state -> state.copy(deleteFailedOrCancelledTransfersResult = result) }
        }
    }

    /**
     * Mark pause or result result consumed
     */
    fun markPauseOrResultResultConsumed() =
        _state.update { state -> state.copy(pauseOrResultResult = null) }

    /**
     * Mark delete failed or cancelled transfer result consumed
     */
    fun markDeleteFailedOrCancelledTransferResultConsumed() =
        _state.update { state -> state.copy(deleteFailedOrCancelledTransfersResult = null) }

    /**
     * Mark delete all completed transfers result consumed
     */
    fun markDeleteAllCompletedTransfersResultConsumed() =
        _state.update { state -> state.copy(deleteAllCompletedTransfersResult = null) }
}

/**
 * Transfer page ui state
 *
 * @property transfersTab
 * @property pauseOrResultResult
 * @property cancelTransfersResult
 * @property deleteFailedOrCancelledTransfersResult
 * @property deleteAllCompletedTransfersResult
 */
data class TransferPageUiState(
    val transfersTab: TransfersTab = TransfersTab.PENDING_TAB,
    val pauseOrResultResult: Result<Boolean>? = null,
    val cancelTransfersResult: Result<Unit>? = null,
    val deleteFailedOrCancelledTransfersResult: Result<List<CompletedTransfer>>? = null,
    val deleteAllCompletedTransfersResult: Result<Unit>? = null,
)
