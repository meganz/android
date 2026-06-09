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
import kotlinx.coroutines.CoroutineScope
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
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
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
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
    private val getPreviewUseCase: GetPreviewUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    /** The resolved source node, retained from the download so the exported copy can be
     * uploaded back to its parent folder. */
    private var sourceNode: TypedFileNode? = null

    /** Tag of the in-progress download transfer */
    private var downloadTransferTag: Int? = null

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
            _uiState.update { it.copy(fileName = node.name, fileSizeBytes = node.size) }
            loadPreviewImage(node)

            // Check cache first
            val existingLocalFile = runCatching {
                getNodePreviewFileUseCase(node)
            }.onFailure {
                Timber.e(it, "Failed to resolve existing local file for node $nodeHandle")
            }.getOrNull()

            if (existingLocalFile != null && existingLocalFile.length() == node.size) {
                emitReady(existingLocalFile)
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
            destFile.delete()

            _uiState.update { it.copy(isDownloading = true) }
            runCatching {
                downloadNodeUseCase(
                    node = node,
                    destinationPath = downloadPath,
                    appData = listOf(TransferAppData.BackgroundTransfer),
                    isHighPriority = true,
                ).collect { event -> handleTransferEvent(event, destFile) }
            }.onFailure {
                Timber.e(it, "Video download failed for node $nodeHandle")
                emitError()
            }
        }
    }

    private fun handleTransferEvent(event: TransferEvent, destFile: File) {
        // Capture the tag so the transfer can be cancelled at the SDK level on dismiss.
        downloadTransferTag = event.transfer.tag
        when (event) {
            is TransferEvent.TransferUpdateEvent ->
                _uiState.update { it.copy(downloadProgress = event.transfer.progress.intValue) }

            is TransferEvent.TransferFinishEvent -> {
                // Finished (success or error): nothing left to cancel.
                downloadTransferTag = null
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

    /**
     * Loads a still image for the preview shown while preparing. Prefers the higher-resolution
     * preview; falls back to the thumbnail when no preview is available. Runs in the background so
     * it never delays the download.
     */
    private fun loadPreviewImage(node: TypedFileNode) {
        viewModelScope.launch {
            val image = runCatching { getPreviewUseCase(node) }
                .onFailure { Timber.e(it, "Failed to load preview for node $nodeHandle") }
                .getOrNull()
                ?: runCatching { getThumbnailUseCase(node.id.longValue) }
                    .onFailure { Timber.e(it, "Failed to load thumbnail for node $nodeHandle") }
                    .getOrNull()
            if (image != null) {
                _uiState.update { it.copy(previewImagePath = image.path) }
            }
        }
    }

    private fun emitReady(destFile: File) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isDownloading = false,
                downloadProgress = 100,
                videoFilePath = destFile.path,
                isError = false,
            )
        }
    }

    private fun emitError() {
        _uiState.update { it.copy(isLoading = false, isDownloading = false, isError = true) }
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

    /**
     * Cancels the in-progress source download at the SDK level when the user dismisses the
     * preparing dialog. Runs on [applicationScope] so the cancellation completes even though the
     * caller immediately navigates back.
     */
    fun cancelDownload() {
        val tag = downloadTransferTag ?: return
        downloadTransferTag = null
        applicationScope.launch {
            runCatching { cancelTransferByTagUseCase(tag) }
                .onFailure { Timber.e(it, "Failed to cancel download transfer $tag") }
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
