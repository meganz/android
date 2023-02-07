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
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandle
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import nz.mega.sdk.MegaNode
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
    private val checkNameCollision: CheckNameCollision,
    private var moveNodeByHandle: MoveNodeByHandle,
    private var copyNodeByHandle: CopyNodeByHandle,
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
     * sets the node and updates its state
     */
    fun updateNode(node: MegaNode) {
        this.node = node
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    historyVersions = getFileHistoryNumVersions(node.handle),
                    isNodeInInbox = isNodeInInbox(node.handle),
                )
            }
        }
    }

    /**
     * Check if the device is connected to Internet and fire and event if not
     */
    fun checkAndHandleIsDeviceConnected() =
        if (!isConnected) {
            _uiState.updateEvent(FileInfoOneOffViewEvent.NotConnected)
            false
        } else {
            true
        }

    /**
     * Tries to move the node to the destination parentHandle node
     * First it checks for collisions, firing the [FileInfoOneOffViewEvent.CollisionDetected] event if corresponds
     * It sets [FileInfoJobInProgressState.Moving] while moving.
     * It will launch [FileInfoOneOffViewEvent.FinishedMoving] at the end with an exception if something went wrong
     */
    fun moveNodeCheckingCollisions(parentHandle: NodeId) =
        performBlockSettingProgress(FileInfoJobInProgressState.Moving) {
            if (checkCollision(parentHandle, NameCollisionType.MOVE)) {
                move(parentHandle)
            }
        }

    /**
     * Tries to copy the node to the destination parentHandle node
     * First it checks for collisions, firing the [FileInfoOneOffViewEvent.CollisionDetected] event if corresponds
     * It sets [FileInfoJobInProgressState.Copying] while copying.
     * It will launch [FileInfoOneOffViewEvent.FinishedCopying] at the end with an exception if something went wrong
     */
    fun copyNodeCheckingCollisions(parentHandle: NodeId) =
        performBlockSettingProgress(FileInfoJobInProgressState.Copying) {
            if (checkCollision(parentHandle, NameCollisionType.COPY)) {
                copy(parentHandle)
            }
        }

    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent(event: FileInfoOneOffViewEvent) {
        if (_uiState.value.oneOffViewEvent == event) {
            _uiState.updateEvent(null)
        }
    }

    /**
     * Get latest value of [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * returns if the node is in the inbox or not
     */
    fun isNodeInInbox() = _uiState.value.isNodeInInbox

    private fun performBlockSettingProgress(
        progressState: FileInfoJobInProgressState,
        block: suspend () -> Unit,
    ) {
        if (checkAndHandleIsDeviceConnected()) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.update {
                    it.copy(jobInProgressState = progressState)
                }
                block()
                _uiState.update {
                    it.copy(jobInProgressState = null)
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
            _uiState.updateEvent(FileInfoOneOffViewEvent.CollisionDetected(nameCollision))
            false
        } catch (throwable: Throwable) {
            if (throwable is MegaNodeException.ChildDoesNotExistsException) {
                true
            } else {
                _uiState.updateEvent(FileInfoOneOffViewEvent.GeneralError)
                false
            }
        }

    /**
     * Moves the node.
     *
     * @param parentHandle Parent handle in which the node will be moved.
     */
    private suspend fun move(parentHandle: NodeId) {
        val moved = runCatching {
            moveNodeByHandle(NodeId(node.handle), parentHandle)
        }
        _uiState.updateEvent(FileInfoOneOffViewEvent.FinishedMoving(moved.exceptionOrNull()))
    }


    /**
     * Copies the node.
     *
     * @param parentHandle Parent handle in which the node will be copied.
     */
    private suspend fun copy(parentHandle: NodeId) {
        val copied = runCatching {
            copyNodeByHandle(NodeId(node.handle), parentHandle)
        }
        _uiState.updateEvent(FileInfoOneOffViewEvent.FinishedCopying(copied.exceptionOrNull()))
    }

    private fun MutableStateFlow<FileInfoViewState>.updateEvent(event: FileInfoOneOffViewEvent?) =
        this.update { it.copy(oneOffViewEvent = event) }
}