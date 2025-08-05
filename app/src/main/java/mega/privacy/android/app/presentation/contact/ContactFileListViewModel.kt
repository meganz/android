package mega.privacy.android.app.presentation.contact

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
import mega.privacy.android.app.R
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactFileListActivity]
 */
@HiltViewModel
class ContactFileListViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase,
    private val filePrepareUseCase: FilePrepareUseCase,
    private val scannerHandler: ScannerHandler,
) : ViewModel() {
    private val _state = MutableStateFlow(ContactFileListUiState())

    /**
     * Ui State
     */
    val state = _state.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Move nodes to rubbish
     *
     * @param nodeHandles
     */
    fun moveNodesToRubbish(nodeHandles: List<Long>) {
        viewModelScope.launch {
            val result = runCatching {
                moveNodesToRubbishUseCase(nodeHandles)
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }

    /**
     * Mark handle move request result
     *
     */
    fun markHandleMoveRequestResult() {
        _state.update { it.copy(moveRequestResult = null) }
    }

    /**
     * Copy or Move nodes
     */
    fun copyOrMoveNodes(nodes: List<Long>, targetNode: Long, type: NodeNameCollisionType) {
        if (!isOnline()) {
            _state.update { it.copy(snackBarMessage = R.string.error_server_connection_problem) }
            return
        }
        val alertText = when (type) {
            NodeNameCollisionType.MOVE -> R.string.context_moving
            NodeNameCollisionType.COPY -> R.string.context_copying
            else -> null
        }
        _state.update { it.copy(copyMoveAlertTextId = alertText) }
        val nodeMap = nodes.associateWith { targetNode }
        checkNameCollision(nodeMap, type)
    }

    private fun checkNameCollision(nodeMap: Map<Long, Long>, type: NodeNameCollisionType) =
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(nodes = nodeMap, type = type)
            }.onSuccess { result ->
                updateStateWithConflictNodes(result)
                initiateCopyOrMoveForNonConflictNodes(result)
            }.onFailure {
                Timber.e(it)
                _state.update { state -> state.copy(copyMoveAlertTextId = null) }
            }
        }

    private suspend fun initiateCopyOrMoveForNonConflictNodes(result: NodeNameCollisionsResult) {
        if (result.type == NodeNameCollisionType.MOVE) {
            moveNodes(result.noConflictNodes)
        } else {
            copyNodes(result.noConflictNodes)
        }
    }

    private fun updateStateWithConflictNodes(result: NodeNameCollisionsResult) = runCatching {
        result.conflictNodes.values.toList()
    }.onSuccess { collisions ->
        _state.update {
            it.copy(copyMoveAlertTextId = null, nodeNameCollisionResult = collisions)
        }
    }.onFailure {
        Timber.e(it)
    }

    /**
     * Mark handle node name collision result
     *
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nodeNameCollisionResult = emptyList()) }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    private suspend fun moveNodes(nodes: Map<Long, Long>) {
        val result = runCatching {
            moveNodesUseCase(nodes)
        }.onFailure { Timber.e(it) }
        _state.update { state -> state.copy(moveRequestResult = result) }
    }


    private suspend fun copyNodes(nodes: Map<Long, Long>) {
        val result = runCatching {
            copyNodesUseCase(nodes)
        }.onFailure { Timber.e(it) }
        _state.update {
            it.copy(moveRequestResult = result, copyMoveAlertTextId = null)
        }
    }

    /**
     * on Consume Snack Bar Message event
     */
    fun onConsumeSnackBarMessageEvent() {
        viewModelScope.launch {
            _state.update { it.copy(snackBarMessage = null) }
        }
    }

    /**
     * Uploads a file to the specified destination.
     *
     * @param file The file to upload.
     * @param destination The destination where the file will be uploaded.
     */
    fun uploadFile(
        file: File,
        destination: Long,
    ) {
        uploadFiles(
            mapOf(file.absolutePath to null),
            NodeId(destination)
        )
    }

    /**
     * Uploads a list of files to the specified destination.
     */
    fun uploadFiles(
        pathsAndNames: Map<String, String?>,
        destinationId: NodeId,
    ) {
        _state.update { state ->
            state.copy(
                uploadEvent = triggered(
                    TransferTriggerEvent.StartUpload.Files(
                        pathsAndNames = pathsAndNames,
                        destinationId = destinationId,
                    )
                )
            )
        }
    }

    internal suspend fun getNodeContentUri(handle: Long) = getNodeContentUriByHandleUseCase(handle)

    /**
     * Prepare files
     */
    suspend fun prepareFiles(uris: List<Uri>) =
        filePrepareUseCase(uris.map { UriPath(it.toString()) })

    /**
     * Consume upload event
     */
    fun consumeUploadEvent() {
        _state.update { it.copy(uploadEvent = consumed()) }
    }

    /**
     * Prepares the ML Kit Document Scanner from Google Play Services
     */
    fun prepareDocumentScanner() {
        viewModelScope.launch {
            runCatching {
                scannerHandler.prepareDocumentScanner()
            }.onSuccess { gmsDocumentScanner ->
                _state.update { it.copy(gmsDocumentScanner = gmsDocumentScanner) }
            }.onFailure { exception ->
                _state.update {
                    it.copy(
                        documentScanningError = if (exception is InsufficientRAMToLaunchDocumentScanner) {
                            DocumentScanningError.InsufficientRAM
                        } else {
                            DocumentScanningError.GenericError
                        }
                    )
                }
            }
        }
    }

    /**
     * When the system fails to open the ML Kit Document Scanner, display a generic error message
     */
    fun onDocumentScannerFailedToOpen() {
        _state.update { it.copy(documentScanningError = DocumentScanningError.GenericError) }
    }

    /**
     * Resets the value of [ContactFileListUiState.gmsDocumentScanner]
     */
    fun onGmsDocumentScannerConsumed() {
        _state.update { it.copy(gmsDocumentScanner = null) }
    }

    /**
     * Resets the value of [ContactFileListUiState.documentScanningError]
     */
    fun onDocumentScanningErrorConsumed() {
        _state.update { it.copy(documentScanningError = null) }
    }

    /**
     * set leave folder node ids
     *
     */
    fun setLeaveFolderNodeIds(nodeIds: List<Long>) {
        _state.update { it.copy(leaveFolderNodeIds = nodeIds) }
    }

    /**
     * clear leave folder node ids
     *
     */
    fun clearLeaveFolderNodeIds() {
        _state.update { it.copy(leaveFolderNodeIds = null) }
    }
}
