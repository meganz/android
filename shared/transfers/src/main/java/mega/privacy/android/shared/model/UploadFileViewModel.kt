package mega.privacy.android.shared.transfers.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val filePrepareUseCase: FilePrepareUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadFileUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Proceed the given list of URIs for upload under the specified parent node,
     * handling storage quotas and file name collisions, and triggering appropriate UI events.
     *
     * @param uris List of [Uri] files to be uploaded.
     * @param parentNodeId [NodeId] for the parent folder node where files will be uploaded. If invalid, falls back to root node.
     * @param pitagTrigger [PitagTrigger] to indicate whether to trigger PiTAG for this upload.
     */
    fun proceedUris(uris: List<Uri>, parentNodeId: NodeId, pitagTrigger: PitagTrigger) {
        viewModelScope.launch {
            if (isInPayWall()) return@launch
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                val entities = filePrepareUseCase(uris.map { UriPath(it.toString()) })
                // Uris can become unreadable between selection and upload confirmation
                // (deleted files, expired permission grants); surface an error instead
                // of finishing silently with the UI locked in the processing state.
                if (entities.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            uploadErrorEvent = triggered(
                                IllegalStateException("None of the uris could be prepared for upload")
                            ),
                        )
                    }
                    return@launch
                }
                uploadDocuments(entities, parentNodeId, pitagTrigger)
            }.onFailure(::onUploadFailure)
        }
    }

    /**
     * Proceed documents the app created itself for upload under the specified parent node,
     * handling storage quotas and file name collisions like [proceedUris]. Unlike shared uris,
     * these do not go through file preparation, which rejects files inside the app's private
     * storage.
     *
     * @param documents [DocumentEntity] list describing the files to be uploaded.
     * @param parentNodeId [NodeId] for the parent folder node where files will be uploaded. If invalid, falls back to root node.
     * @param pitagTrigger [PitagTrigger] to indicate whether to trigger PiTAG for this upload.
     */
    fun proceedDocuments(
        documents: List<DocumentEntity>,
        parentNodeId: NodeId,
        pitagTrigger: PitagTrigger,
    ) {
        viewModelScope.launch {
            if (isInPayWall()) return@launch
            _uiState.update { it.copy(isProcessing = true) }
            runCatching { uploadDocuments(documents, parentNodeId, pitagTrigger) }
                .onFailure(::onUploadFailure)
        }
    }

    private suspend fun uploadDocuments(
        entities: List<DocumentEntity>,
        parentNodeId: NodeId,
        pitagTrigger: PitagTrigger,
    ) {
        val parentOrRootNodeId = if (parentNodeId.longValue != -1L) parentNodeId
        else getRootNodeUseCase()?.id ?: NodeId(-1L)
        val collisions = checkFileNameCollisionsUseCase(
            files = entities,
            parentNodeId = parentOrRootNodeId,
            pitagTrigger = pitagTrigger,
        )
        // resolve name collisions and keep uploading files without collisions
        if (collisions.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameCollisionEvent = triggered(collisions)
                )
            }
        }
        val collidedPaths = collisions.mapTo(mutableSetOf()) { it.path.value }
        val noCollisionPaths = entities.filter {
            collidedPaths.contains(it.uri.value).not()
        }
        if (noCollisionPaths.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    startUploadEvent = triggered(
                        TransferTriggerEvent.StartUpload.Files(
                            pathsAndNames = noCollisionPaths.associate { entity -> entity.uri.value to null },
                            destinationId = parentOrRootNodeId,
                            pitagTrigger = pitagTrigger,
                        )
                    )
                )
            }
        }
    }

    private fun onUploadFailure(e: Throwable) {
        _uiState.update {
            it.copy(
                isProcessing = false,
                uploadErrorEvent = triggered(e)
            )
        }
        Timber.e(e)
    }

    /**
     * Emits a single [TransferTriggerEvent.StartChatUpload.Files] addressed at every chat in
     * [chatIds] for the upstream transfer handler. Short-circuits with [overQuotaEvent] when the
     * account is in [StorageState.PayWall].
     */
    fun attachFilesToChat(uris: List<Uri>, chatIds: List<Long>, pitagTrigger: PitagTrigger) {
        viewModelScope.launch {
            if (isInPayWall()) return@launch
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    startUploadEvent = triggered(
                        TransferTriggerEvent.StartChatUpload.Files(
                            chatIds = chatIds,
                            uris = uris.map { uri -> UriPath(uri.toString()) },
                            pitagTrigger = pitagTrigger,
                        )
                    )
                )
            }
        }
    }

    private fun isInPayWall(): Boolean {
        val state = runCatching {
            monitorStorageStateEventUseCase().value.storageState
        }.getOrNull()

        return if (state == StorageState.PayWall) {
            _uiState.update { it.copy(overQuotaEvent = triggered) }
            true
        } else {
            false
        }
    }

    fun onConsumeOverQuotaEvent() {
        _uiState.update { it.copy(overQuotaEvent = consumed) }
    }

    fun onConsumeNameCollisionEvent() {
        _uiState.update { it.copy(nameCollisionEvent = consumed()) }
    }

    fun onConsumeStartUploadEvent() {
        _uiState.update { it.copy(startUploadEvent = consumed()) }
    }

    fun onConsumeUploadErrorEvent() {
        _uiState.update { it.copy(uploadErrorEvent = consumed()) }
    }
}

