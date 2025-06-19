package mega.privacy.android.app.presentation.transfers.model.completed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadDocumentFileUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadParentDocumentFileUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for completed transfer actions.
 *
 * @property uiState The UI state for completed transfer actions.
 */
@HiltViewModel
class CompletedTransferActionsViewModel @Inject constructor(
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val getDownloadParentDocumentFileUseCase: GetDownloadParentDocumentFileUseCase,
    private val getDownloadDocumentFileUseCase: GetDownloadDocumentFileUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompletedTransferActionsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOnline -> _uiState.update { it.copy(isOnline = isOnline) } }
        }
    }

    /**
     * Check completed transfer to the view model state.
     */
    fun checkCompletedTransferActions(completedTransfer: CompletedTransfer) {
        with(completedTransfer) {
            _uiState.update { state -> state.copy(completedTransfer = this) }

            checkShareOption(handle)

            if (isContentUriDownload) {
                getParentDocumentFile(path)
                getDocumentFile(path, fileName)
            }
        }
    }

    private fun checkShareOption(transferHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeAccessPermission(NodeId(transferHandle))
            }.onSuccess { accessPermission ->
                _uiState.update { it.copy(amINodeOwner = accessPermission == AccessPermission.OWNER) }
            }.onFailure {
                Timber.e("Error getting access permission: $it")
            }
        }
    }

    private fun getParentDocumentFile(path: String) {
        viewModelScope.launch {
            runCatching {
                getDownloadParentDocumentFileUseCase(path)
            }.onSuccess { parentDocumentFile ->
                _uiState.update { state -> state.copy(parentUri = parentDocumentFile?.uri?.toUri()) }
            }.onFailure {
                Timber.e("Error getting parent document file: $it")
            }
        }
    }

    private fun getDocumentFile(path: String, fileName: String) {
        viewModelScope.launch {
            runCatching {
                getDownloadDocumentFileUseCase(path, fileName)
            }.onSuccess { documentFile ->
                _uiState.update { it.copy(fileUri = documentFile?.uri?.toUri()) }
            }.onFailure {
                Timber.e("Error getting document file: $it")
            }
        }
    }

    /**
     * Open with action for completed transfer.
     */
    fun openWith(completedTransfer: CompletedTransfer) {
        val fileType = fileTypeInfoMapper(completedTransfer.fileName).mimeType

        if (completedTransfer.originalPath.startsWith(File.separator)) {
            File(completedTransfer.originalPath).let { localFile ->
                if (localFile.exists()) {
                    triggerOpenWith(file = localFile, fileType = fileType)
                } else {
                    triggerOpenWith()
                }
            }
        } else {
            uiState.value.fileUri?.let { fileUri ->
                triggerOpenWith(uri = fileUri, fileType = fileType)
            } ?: triggerOpenWith()
        }
    }

    private fun triggerOpenWith(file: File? = null, uri: Uri? = null, fileType: String? = null) {
        _uiState.update { state ->
            state.copy(
                openWithEvent = triggered(
                    OpenWithEvent(
                        file = file,
                        uri = uri,
                        fileType = fileType
                    )
                )
            )
        }
    }

    /**
     * Consume the open with event.
     */
    fun onConsumeOpenWithEvent() {
        _uiState.update { state -> state.copy(openWithEvent = consumed()) }
    }

    /**
     * Share link action for completed transfer.
     */
    fun shareLink(handle: Long) {
        viewModelScope.launch {
            runCatching { getNodeByHandleUseCase(handle) }
                .onFailure {
                    Timber.e(it)
                    _uiState.update { state ->
                        state.copy(shareLinkEvent = triggered(ShareLinkEvent()))
                    }
                }.onSuccess {
                    _uiState.update { state ->
                        state.copy(shareLinkEvent = triggered(ShareLinkEvent(it)))
                    }
                }
        }
    }

    /**
     * Consume the share link event.
     */
    fun onConsumeShareLinkEvent() {
        _uiState.update { state -> state.copy(shareLinkEvent = consumed()) }
    }

    /**
     * Clear completed transfer action.
     */
    fun clearTransfer(completedTransfer: CompletedTransfer) {
        viewModelScope.launch {
            runCatching { deleteCompletedTransferUseCase(completedTransfer, false) }
                .onFailure { Timber.e(it) }
        }
    }
}