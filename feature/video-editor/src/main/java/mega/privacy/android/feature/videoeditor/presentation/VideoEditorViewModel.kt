package mega.privacy.android.feature.videoeditor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import mega.privacy.android.feature.videoeditor.presentation.model.VideoEditorUiState
import timber.log.Timber
import java.io.File

/**
 * ViewModel for the video editor screen.
 */
@HiltViewModel(assistedFactory = VideoEditorViewModel.Factory::class)
internal class VideoEditorViewModel @AssistedInject constructor(
    @Assisted private val nodeHandle: Long,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    init {
        downloadVideo()
    }

    private fun downloadVideo() {
        viewModelScope.launch {
            val node = runCatching { getNodeByIdUseCase(NodeId(nodeHandle)) }
                .onFailure { Timber.e(it, "Failed to resolve node $nodeHandle") }
                .getOrNull() as? TypedFileNode
            if (node == null) {
                emitError()
                return@launch
            }

            val downloadPath = runCatching { getFilePreviewDownloadPathUseCase() }
                .onFailure { Timber.e(it, "Failed to resolve cache download path") }
                .getOrNull()
            if (downloadPath == null) {
                emitError()
                return@launch
            }

            val destFile = File(downloadPath, node.name)
            // Reuse a previously cached copy when it is already fully downloaded.
            if (destFile.exists() && destFile.length() == node.size) {
                emitReady(destFile)
                return@launch
            }
            destFile.delete()

            runCatching {
                downloadNodeUseCase(
                    node = node,
                    destinationPath = downloadPath,
                    appData = listOf(TransferAppData.PreviewDownload),
                    isHighPriority = true,
                ).collect { event -> handleTransferEvent(event, destFile) }
            }.onFailure {
                Timber.e(it, "Video download failed for node $nodeHandle")
                emitError()
            }
        }
    }

    private fun handleTransferEvent(event: TransferEvent, destFile: File) {
        when (event) {
            is TransferEvent.TransferUpdateEvent ->
                _uiState.update { it.copy(downloadProgress = event.transfer.progress.intValue) }

            is TransferEvent.TransferFinishEvent -> {
                if (event.error == null && destFile.exists() && destFile.length() > 0L) {
                    emitReady(destFile)
                } else {
                    Timber.e(event.error, "Video download finished with error for node $nodeHandle")
                    emitError()
                }
            }

            else -> Unit
        }
    }

    private fun emitReady(destFile: File) {
        _uiState.update {
            it.copy(
                isLoading = false,
                downloadProgress = 100,
                videoFilePath = destFile.path,
                isError = false,
            )
        }
    }

    private fun emitError() {
        _uiState.update { it.copy(isLoading = false, isError = true) }
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long): VideoEditorViewModel
    }
}
