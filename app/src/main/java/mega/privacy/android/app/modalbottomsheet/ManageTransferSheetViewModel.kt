package mega.privacy.android.app.modalbottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.transfer.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfer.GetCompletedTransferByIdUseCase
import javax.inject.Inject

@HiltViewModel
internal class ManageTransferSheetViewModel @Inject constructor(
    private val getCompletedTransferByIdUseCase: GetCompletedTransferByIdUseCase,
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageTransferSheetUiState())

    /**
     * Ui state
     */
    val uiState = _uiState.asStateFlow()

    init {
        val transferId =
            savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID) ?: 0
        viewModelScope.launch {
            runCatching {
                getCompletedTransferByIdUseCase(transferId)
            }.onSuccess { transfer ->
                _uiState.update { it.copy(transfer = transfer) }
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
data class ManageTransferSheetUiState(val transfer: CompletedTransfer? = null)