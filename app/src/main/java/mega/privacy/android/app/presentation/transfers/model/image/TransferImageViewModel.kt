package mega.privacy.android.app.presentation.transfers.model.image

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * ViewModel for Transfer items.
 *
 */
@HiltViewModel
class TransferImageViewModel @Inject constructor(
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
) : ViewModel() {

    private val _uiStateFlowMap =
        ConcurrentHashMap<Int, MutableStateFlow<TransferImageUiState>>()

    /**
     * Get the [StateFlow] for a given in progress transfer which has [tag] as identifier.
     */
    fun getUiStateFlow(tag: Int): StateFlow<TransferImageUiState> =
        getMutableStateFlow(tag)
            ?: MutableStateFlow(TransferImageUiState()).also { _uiStateFlowMap[tag] = it }

    /**
     * Add a new in progress transfer to the UI state.
     */
    fun addInProgressTransfer(inProgressTransfer: InProgressTransfer) = with(inProgressTransfer) {
        viewModelScope.launch {
            val fileTypeResId = getFileTypeIcon(fileName)

            if (this@with is InProgressTransfer.Download) {
                getMutableStateFlow(tag)?.update { state ->
                    state.copy(fileTypeResId = fileTypeResId)
                } ?: MutableStateFlow(TransferImageUiState(fileTypeResId = fileTypeResId))
                    .also { _uiStateFlowMap[tag] = it }

                runCatching { getThumbnailUseCase(nodeId.longValue, true) }
                    .onSuccess { thumbnail ->
                        getMutableStateFlow(tag)?.update { state ->
                            state.copy(previewUri = thumbnail?.toUri())
                        }
                    }.onFailure { Timber.w(it) }
            } else {
                getMutableStateFlow(tag)?.update { state ->
                    state.copy(
                        fileTypeResId = fileTypeResId,
                        previewUri = (this@with as InProgressTransfer.Upload).localPath.toUri()
                    )
                } ?: MutableStateFlow(
                    TransferImageUiState(
                        fileTypeResId = fileTypeResId,
                        previewUri = (this@with as InProgressTransfer.Upload).localPath.toUri()
                    )
                ).also { _uiStateFlowMap[tag] = it }
            }
        }
    }

    private fun getFileTypeIcon(fileName: String) =
        fileTypeIconMapper(fileName.substringAfterLast('.', ""))

    private fun getMutableStateFlow(tag: Int) = _uiStateFlowMap[tag]
}