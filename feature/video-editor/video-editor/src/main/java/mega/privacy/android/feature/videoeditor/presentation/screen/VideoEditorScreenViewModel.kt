package mega.privacy.android.feature.videoeditor.presentation.screen

import android.net.Uri
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
import mega.privacy.android.feature.videoeditor.presentation.screen.model.VideoEditorUiState
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import timber.log.Timber
import java.io.File

/**
 * Host ViewModel for the video editor screen.
 *
 * Owns the MEGA-integration side of the screen: it downloads the source video
 * into cache (exposed via [uiState]) and uploads the exported result back to
 * MEGA cloud. The in-editor MVI state lives in a separate [mega.privacy.android.feature.videoeditor.presentation.editor.EditorViewModel];
 * the screen bridges the two.
 */
@HiltViewModel(assistedFactory = VideoEditorScreenViewModel.Factory::class)
internal class VideoEditorScreenViewModel @AssistedInject constructor(
    @Assisted private val nodeHandle: Long,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
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

    /**
     * Called when the editor finishes encoding the edited video to [outputUri].
     * Confirms the outcome to the user; the encoded file is the input for the
     * MEGA upload.
     */
    fun onExportSucceeded(outputUri: Uri) {
        viewModelScope.launch { snackbarEventQueue.queueMessage(EXPORT_SUCCESS_MESSAGE) }
    }

    /** Called when the editor's export fails, to report it to the user. */
    fun onExportFailed() {
        viewModelScope.launch { snackbarEventQueue.queueMessage(EXPORT_FAILURE_MESSAGE) }
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long): VideoEditorScreenViewModel
    }

    // TODO use string res
    private companion object {
        const val EXPORT_SUCCESS_MESSAGE = "Video saved"
        const val EXPORT_FAILURE_MESSAGE = "Couldn't save video"
    }
}
