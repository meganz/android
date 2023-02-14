package mega.privacy.android.app.presentation.fileinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.GetThumbnail
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.presentation.fileinfo.FileInfoActivity]
 */
@HiltViewModel
class FileInfoViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorConnectivity: MonitorConnectivity,
    private val getFileHistoryNumVersions: GetFileHistoryNumVersions,
    private val isNodeInInbox: IsNodeInInbox,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val checkNameCollision: CheckNameCollision,
    private val moveNodeByHandle: MoveNodeByHandle,
    private val copyNodeByHandle: CopyNodeByHandle,
    private val moveNodeToRubbishByHandle: MoveNodeToRubbishByHandle,
    private val deleteNodeByHandle: DeleteNodeByHandle,
    private val deleteNodeVersionsByHandle: DeleteNodeVersionsByHandle,
    private val getThumbnail: GetThumbnail,
    private val getPreview: GetPreview,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileInfoViewState())

    /**
     * the state of the view
     */
    val uiState = _uiState.asStateFlow()

    /**
     * the node whose information are displayed
     */
    lateinit var node: MegaNode
        private set

    /**
     * Sets the node and updates its state
     */
    fun updateNode(node: MegaNode = this.node) {
        this.node = node
        viewModelScope.launch {
            _uiState.update {
                val clearProgressState =
                    it.jobInProgressState == FileInfoJobInProgressState.InitialLoading
                it.copy(
                    historyVersions = getFileHistoryNumVersions(node.handle),
                    isNodeInInbox = isNodeInInbox(node.handle),
                    isNodeInRubbish = isNodeInRubbish(node.handle),
                    jobInProgressState = if (clearProgressState) null else it.jobInProgressState,
                )
            }
        }
        updatePreview()
    }

    private fun updatePreview() {
        viewModelScope.launch {
            if (_uiState.value.thumbnailUriString == null) {
                _uiState.update {
                    it.copy(thumbnailUriString = getThumbnail(node.handle)?.uriStringIfExists())
                }
            }
            if (node.hasPreview() && _uiState.value.previewUriString == null) {
                _uiState.update {
                    it.copy(previewUriString = getPreview(node.handle)?.uriStringIfExists())
                }
            }
        }
    }

    /**
     * Check if the device is connected to Internet and fire and event if not
     */
    fun checkAndHandleIsDeviceConnected() =
        if (!isConnected) {
            _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NotConnected)
            false
        } else {
            true
        }

    /**
     * Tries to move the node to the destination parentHandle node
     * First it checks for collisions, firing the [FileInfoOneOffViewEvent.CollisionDetected] event if corresponds
     * It sets [FileInfoJobInProgressState.Moving] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    fun moveNodeCheckingCollisions(parentHandle: NodeId) =
        performBlockSettingProgress(FileInfoJobInProgressState.Moving) {
            if (checkCollision(parentHandle, NameCollisionType.MOVE)) {
                runCatching {
                    moveNodeByHandle(NodeId(node.handle), parentHandle)
                }
            } else {
                null
            }
        }

    /**
     * Tries to copy the node to the destination parentHandle node
     * First it checks for collisions, firing the [FileInfoOneOffViewEvent.CollisionDetected] event if corresponds
     * It sets [FileInfoJobInProgressState.Copying] while copying.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    fun copyNodeCheckingCollisions(parentHandle: NodeId) =
        performBlockSettingProgress(FileInfoJobInProgressState.Copying) {
            if (checkCollision(parentHandle, NameCollisionType.COPY)) {
                runCatching {
                    copyNodeByHandle(NodeId(node.handle), parentHandle)
                }
            } else {
                null
            }
        }

    /**
     * It checks if the node is in the rubbish bin,
     * if it's not in the rubbish bin, moves the node to the rubbish bin
     * if it's already in the rubbish bin, deletes the node.
     * It will sets the proper [FileInfoJobInProgressState] and launch the proper [FileInfoOneOffViewEvent.Finished]
     */
    fun removeNode() {
        if (_uiState.value.isNodeInRubbish) {
            deleteNode()
        } else {
            moveNodeToRubbishBin()
        }
    }

    /**
     * Deletes the history versions of the node
     * It sets [FileInfoJobInProgressState.DeletingVersions] while deleting.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    fun deleteHistoryVersions() {
        performBlockSettingProgress(FileInfoJobInProgressState.DeletingVersions) {
            runCatching {
                deleteNodeVersionsByHandle(NodeId(node.handle))
            }
        }
    }

    /**
     * Tries to move the node to the rubbish bin
     * It sets [FileInfoJobInProgressState.MovingToRubbish] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    private fun moveNodeToRubbishBin() {
        performBlockSettingProgress(FileInfoJobInProgressState.MovingToRubbish) {
            runCatching {
                moveNodeToRubbishByHandle(NodeId(node.handle))
            }
        }
    }

    /**
     * Tries to delete the node
     * It sets [FileInfoJobInProgressState.Deleting] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    private fun deleteNode() {
        performBlockSettingProgress(FileInfoJobInProgressState.Deleting) {
            runCatching {
                deleteNodeByHandle(NodeId(node.handle))
            }
        }
    }

    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent(event: FileInfoOneOffViewEvent) {
        if (_uiState.value.oneOffViewEvent == event) {
            _uiState.updateEventAndClearProgress(null)
        }
    }

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    /**
     * Is connected
     */
    private val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * returns if the node is in the inbox or not
     */
    fun isNodeInInbox() = _uiState.value.isNodeInInbox

    /**
     * Performs a job setting [progressState] state while in progress
     * First checks the device connection
     * @param progressState the [FileInfoJobInProgressState] that describes the job that will be done
     * @param block that performs the job
     */
    private fun performBlockSettingProgress(
        progressState: FileInfoJobInProgressState,
        block: suspend () -> Result<*>?,
    ) {
        if (checkAndHandleIsDeviceConnected()) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.update {
                    it.copy(jobInProgressState = progressState)
                }
                val result = block()
                // if there's a result, the job has finished, (for instance: collision detected returns null because it handles the ui update)
                if (result != null) {
                    _uiState.updateEventAndClearProgress(
                        FileInfoOneOffViewEvent.Finished(
                            jobFinished = progressState,
                            exception = result.exceptionOrNull()
                        ),
                    )
                }
            }
        }
    }

    /**
     * Checks if there is a name collision before moving or copying the node.
     *
     * @param parentHandle Parent handle of the node in which the node will be moved or copied.
     * @param type         Type of name collision to check.
     * @return true if there are no collision detected, so it can go ahead with the move or copy
     */
    private suspend fun checkCollision(parentHandle: NodeId, type: NameCollisionType) =
        try {
            val nameCollision = checkNameCollision(
                NodeId(node.handle),
                parentHandle,
                type
            )
            _uiState.updateEventAndClearProgress(
                FileInfoOneOffViewEvent.CollisionDetected(
                    nameCollision
                )
            )
            false
        } catch (throwable: Throwable) {
            if (throwable is MegaNodeException.ChildDoesNotExistsException) {
                true
            } else {
                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.GeneralError)
                false
            }
        }

    private fun MutableStateFlow<FileInfoViewState>.updateEventAndClearProgress(event: FileInfoOneOffViewEvent?) =
        this.update {
            it.copy(
                oneOffViewEvent = event,
                jobInProgressState = null,
            )
        }

    private fun File?.uriStringIfExists() = this?.takeIf { it.exists() }?.toURI()?.toString()
}