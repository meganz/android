package mega.privacy.android.app.modalbottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetCompletedTransferByIdUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadDocumentFileUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadParentDocumentFileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ManageTransferSheetViewModel @Inject constructor(
    private val getCompletedTransferByIdUseCase: GetCompletedTransferByIdUseCase,
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val getDownloadParentDocumentFileUseCase: GetDownloadParentDocumentFileUseCase,
    private val getDownloadDocumentFileUseCase: GetDownloadDocumentFileUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
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
                transfer?.let {
                    checkShareOption(it.handle)

                    if (it.isContentUriDownload) {
                        getParentDocumentFile(it)
                        getDocumentFile(it)
                    }
                }
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

    private fun getParentDocumentFile(transfer: CompletedTransfer) {
        viewModelScope.launch {
            runCatching {
                getDownloadParentDocumentFileUseCase(transfer.path)
            }.onSuccess { parentDocumentFile ->
                parentDocumentFile?.let {
                    getParentFilePath(it)
                    _uiState.update { state -> state.copy(parentDocumentFile = it) }
                }
            }.onFailure {
                Timber.e("Error getting parent document file: $it")
            }
        }
    }

    private fun getDocumentFile(transfer: CompletedTransfer) {
        viewModelScope.launch {
            runCatching {
                getDownloadDocumentFileUseCase(transfer.path, transfer.fileName)
            }.onSuccess { documentFile ->
                _uiState.update { it.copy(documentFile = documentFile) }
            }.onFailure {
                Timber.e("Error getting document file: $it")
            }
        }
    }

    private fun getParentFilePath(parentDocumentFile: DocumentEntity) {
        viewModelScope.launch {
            runCatching {
                getPathByDocumentContentUriUseCase(parentDocumentFile.getUriString())
            }.onSuccess { path ->
                _uiState.update { it.copy(parentFilePath = path) }
            }.onFailure {
                Timber.e("Error getting parent file path: $it")
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
    val parentFilePath: String? = null,
    val parentDocumentFile: DocumentEntity? = null,
    val documentFile: DocumentEntity? = null,
    val iAmNodeOwner: Boolean = false,
) {
    val parentUri = parentDocumentFile?.uri?.toUri()
    val fileUri = documentFile?.uri?.toUri()
}