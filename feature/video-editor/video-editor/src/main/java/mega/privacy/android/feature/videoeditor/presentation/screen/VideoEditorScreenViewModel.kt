package mega.privacy.android.feature.videoeditor.presentation.screen

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import mega.privacy.android.feature.videoeditor.presentation.screen.model.VideoEditorUiState
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
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
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    /** The resolved source node, retained from the download so the exported copy can be
     * uploaded back to its parent folder. */
    private var sourceNode: TypedFileNode? = null

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
            sourceNode = node

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
     * Uploads the encoded copy ([outputUri]) into the source's parent folder via the app's transfer
     * subsystem, under a collision-safe name. Surfaced as [VideoEditorUiState.transferEvent]; the
     * app shows the upload-started snackbar and the editor closes once the event is handed off.
     */
    fun onExportSucceeded(outputUri: Uri) {
        val node = sourceNode
        val path = outputUri.path
        if (node == null || path == null) {
            Timber.e("Cannot upload export: source node or output path missing")
            onExportFailed()
            return
        }
        viewModelScope.launch {
            runCatching {
                val collision = FileNameCollision(
                    collisionHandle = node.id.longValue,
                    name = node.name,
                    size = node.size,
                    lastModified = node.modificationTime,
                    parentHandle = node.parentId.longValue,
                    path = UriPath(path),
                    pitagTrigger = PitagTrigger.NotApplicable,
                )
                getNodeNameCollisionRenameNameUseCase(collision)
            }.onSuccess { renameName ->
                _uiState.update {
                    it.copy(
                        transferEvent = triggered(
                            TransferTriggerEvent.StartUpload.Files(
                                pathsAndNames = mapOf(path to renameName),
                                destinationId = node.parentId,
                                specificStartMessage = context.getString(sharedR.string.photo_editor_upload_message),
                                pitagTrigger = PitagTrigger.NotApplicable,
                            ),
                        ),
                    )
                }
            }.onFailure {
                Timber.e(it, "Failed to resolve upload name for the exported video")
                onExportFailed()
            }
        }
    }

    /** Called when the editor's export fails, to report it to the user. */
    fun onExportFailed() {
        viewModelScope.launch { snackbarEventQueue.queueMessage(EXPORT_FAILURE_MESSAGE) }
    }

    /** Clear the upload [TransferTriggerEvent] once the host has handed it to the transfer subsystem. */
    fun consumeTransferEvent() {
        _uiState.update { it.copy(transferEvent = consumed()) }
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long): VideoEditorScreenViewModel
    }

    private companion object {
        const val EXPORT_FAILURE_MESSAGE = "Couldn't save video"
    }
}
