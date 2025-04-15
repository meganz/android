package mega.privacy.android.app.modalbottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetCompletedTransferByIdUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ManageTransferSheetViewModel @Inject constructor(
    private val getCompletedTransferByIdUseCase: GetCompletedTransferByIdUseCase,
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageTransferSheetUiState())

    /**
     * Ui state
     */
    val uiState = _uiState.asStateFlow()

    val transferId =
        savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID) ?: 0

    init {
        getCompletedTransfer()
    }

    private fun getCompletedTransfer() {
        viewModelScope.launch {
            runCatching {
                getCompletedTransferByIdUseCase(transferId)
            }.onSuccess { transfer ->
                transfer?.handle?.let { checkShareOption(it) }
                _uiState.update { it.copy(transfer = transfer) }
            }.onFailure {
                Timber.e("Error getting completed transfer: $it")
            }
        }
    }

    private fun checkShareOption(transferHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeAccessPermission(NodeId(transferHandle))
            }.onSuccess {
                val showShareOption = it == AccessPermission.OWNER
                _uiState.update { it.copy(iAmNodeOwner = showShareOption) }
            }.onFailure {
                Timber.e("Error getting access permission: $it")
            }
        }
    }

    /**
     * Removes a completed transfer.
     *
     * @param transfer transfer to remove
     * @param isRemovedCache If ture, remove cache file, otherwise doesn't remove cache file
     */
    fun completedTransferRemoved(transfer: CompletedTransfer, isRemovedCache: Boolean) =
        viewModelScope.launch {
            deleteCompletedTransferUseCase(transfer, isRemovedCache)
        }
}

/**
 * Manage transfer sheet ui state
 *
 * @property transfer
 */
data class ManageTransferSheetUiState(
    val transfer: CompletedTransfer? = null,
    val iAmNodeOwner: Boolean = false,
)