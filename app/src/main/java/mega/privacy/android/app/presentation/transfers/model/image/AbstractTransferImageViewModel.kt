package mega.privacy.android.app.presentation.transfers.model.image

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel for image Transfer items.
 *
 */
abstract class AbstractTransferImageViewModel(
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
) : ViewModel() {

    private val _uiStateFlowMap =
        ConcurrentHashMap<Int, MutableStateFlow<TransferImageUiState>>()

    /**
     * Get the [StateFlow] for a given in progress transfer which has [id] as identifier.
     */
    fun getUiStateFlow(id: Int): StateFlow<TransferImageUiState> =
        getMutableStateFlow(id)
            ?: MutableStateFlow(TransferImageUiState()).also { _uiStateFlowMap[id] = it }

    /**
     * Adds a new transfer to the UI state.
     */
    open fun <T> addTransfer(transfer: T) {}

    /**
     * Add a new node transfer to the UI state.
     */
    protected fun addNodeTransfer(id: Int, fileName: String, nodeId: NodeId) {
        viewModelScope.launch {
            val fileTypeResId = getFileTypeIcon(fileName)

            getMutableStateFlow(id)?.update { state ->
                state.copy(fileTypeResId = fileTypeResId)
            } ?: MutableStateFlow(TransferImageUiState(fileTypeResId = fileTypeResId))
                .also { _uiStateFlowMap[id] = it }

            runCatching { getThumbnailUseCase(nodeId.longValue, true) }
                .onSuccess { thumbnail ->
                    getMutableStateFlow(id)?.update { state ->
                        state.copy(previewUri = thumbnail?.toUri())
                    }
                }.onFailure { Timber.w(it) }
        }
    }

    /**
     * Add a new file transfer to the UI state.
     */
    protected fun addFileTransfer(id: Int, fileName: String, localPath: String) {
        viewModelScope.launch {
            val fileTypeResId = getFileTypeIcon(fileName)

            getMutableStateFlow(id)?.update { state ->
                state.copy(
                    fileTypeResId = fileTypeResId,
                    previewUri = localPath.toUri()
                )
            } ?: MutableStateFlow(
                TransferImageUiState(
                    fileTypeResId = fileTypeResId,
                    previewUri = localPath.toUri()
                )
            ).also { _uiStateFlowMap[id] = it }
        }
    }

    private fun getFileTypeIcon(fileName: String) =
        fileTypeIconMapper(fileName.substringAfterLast('.', ""))

    private fun getMutableStateFlow(tag: Int) = _uiStateFlowMap[tag]
}