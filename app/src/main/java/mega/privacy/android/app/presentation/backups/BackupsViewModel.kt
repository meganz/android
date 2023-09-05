package mega.privacy.android.app.presentation.backups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.backups.model.BackupsState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class associated to [BackupsFragment]
 *
 * @property getChildrenNode [GetChildrenNode]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property getNodeByHandle [GetNodeByHandle]
 * @property getParentNodeHandle [GetParentNodeHandle]
 * @property monitorBackupFolder [MonitorBackupFolder]
 * @property monitorNodeUpdates [MonitorNodeUpdates]
 * @property monitorViewType [MonitorViewType]
 */
@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {

    /**
     * The Backups UI State
     */
    private val _state = MutableStateFlow(BackupsState())

    /**
     * The Backups UI State accessible outside the ViewModel
     */
    val state: StateFlow<BackupsState> = _state

    /**
     * The My Backups Folder Node
     */
    private var myBackupsFolderNode: NodeId = NodeId(-1L)

    /**
     * Perform the following actions upon ViewModel initialization
     */
    init {
        observeNodeUpdates()
        observeMyBackupsFolderUpdates()
        observeViewType()
    }

    /**
     * Uses [monitorNodeUpdates] to observe any Node updates
     *
     * A received Node update will refresh the list of nodes and hide the multiple item selection feature
     */
    private fun observeNodeUpdates() = viewModelScope.launch {
        monitorNodeUpdates().collect {
            _state.update { it.copy(isPendingRefresh = true) }
        }
    }

    /**
     * Uses [monitorBackupFolder] to observe any updates in the My Backups folder
     */
    private fun observeMyBackupsFolderUpdates() = viewModelScope.launch {
        monitorBackupFolder()
            .catch { Timber.w("Exception monitoring backups folder: $it") }
            .map {
                // Emit an Invalid Handle if the Result is a failure
                Timber.e("Unable to retrieve the My Backups Folder")
                it.getOrDefault(NodeId(MegaApiJava.INVALID_HANDLE))
            }
            .collectLatest { newMyBackupsFolder ->
                Timber.d("The My Backups Folder Handle is: ${newMyBackupsFolder.longValue}")
                onMyBackupsFolderUpdateReceived(newMyBackupsFolder)
            }
    }

    /**
     * Uses [monitorViewType] to observe any View Type updates
     */
    private fun observeViewType() = viewModelScope.launch {
        monitorViewType().collect { viewType ->
            _state.update { it.copy(currentViewType = viewType) }
        }
    }

    /**
     *
     * Processes updates received from the My Backups Folder
     *
     * If the Backups Handle is invalid, use the My Backups Folder Handle to refresh the
     * list of Nodes
     *
     * Update the UI State values after performing a successful refresh
     *
     * @param newMyBackupsFolder The updated My Backups Folder
     */
    private suspend fun onMyBackupsFolderUpdateReceived(newMyBackupsFolder: NodeId) {
        myBackupsFolderNode = newMyBackupsFolder

        _state.update { backupsState ->
            if (backupsState.backupsHandle == -1L) {
                refreshNodes(newMyBackupsFolder.longValue).let { updatedNodes ->
                    backupsState.copy(
                        backupsHandle = newMyBackupsFolder.longValue,
                        nodes = updatedNodes,
                    )
                }
            } else {
                refreshNodes().let { updatedNodes ->
                    backupsState.copy(nodes = updatedNodes)
                }
            }
        }
    }

    /**
     * Refreshes Backups nodes and hides the selection
     */
    fun refreshBackupsNodesAndHideSelection() = viewModelScope.launch {
        refreshNodes().let { updatedNodes ->
            _state.update {
                it.copy(
                    hideMultipleItemSelection = true,
                    nodes = updatedNodes,
                )
            }
        }
    }

    /**
     * Refreshes the list of Backups Nodes
     */
    fun refreshBackupsNodes() = viewModelScope.launch {
        refreshNodes().also { setNodes(it) }
    }

    /**
     * Retrieves the list of Nodes by performing the following steps:
     *
     * 2. Check if [BackupsState.backupsHandle] equals -1L or [MegaApiJava.INVALID_HANDLE]. If either condition is true, return an empty list
     * 3. Call the Use Case [getNodeByHandle] to retrieve the Parent Node from [BackupsState.backupsHandle]. If the Parent Node is null, return an empty list
     * 4. Call the Use Case [getChildrenNode] to retrieve and return the list of Nodes under the Parent Node
     *
     * @param nodeHandle The Node Handle used to retrieve the current list of Nodes. Defaults to [BackupsState.backupsHandle]
     *
     * @return a List of Backup Nodes
     */
    private suspend fun refreshNodes(nodeHandle: Long = _state.value.backupsHandle): List<MegaNode> =
        if (nodeHandle != -1L) {
            getNodeByHandle(nodeHandle)?.let { parentNode ->
                getChildrenNode(
                    parent = parentNode,
                    order = getCloudSortOrder(),
                )
            }.orEmpty()
        } else {
            emptyList()
        }

    /**
     * Handles the Back Press Behavior from Backups after checking certain conditions
     */
    fun handleBackPress() {
        viewModelScope.launch {
            // Either one of the following conditions will constitute the user exiting the screen:
            // 1. The My Backups Folder Handle is -1L
            // 2. The Handles of the My Backups Folder and the UI State are the same
            // 3. The retrieved Parent Node Handle does not exist
            val backupsHandle = _state.value.backupsHandle

            if (myBackupsFolderNode.longValue == -1L || myBackupsFolderNode.longValue == backupsHandle) {
                onExitBackups(true)
            } else {
                getParentNodeHandle(backupsHandle)?.let { parentNodeHandle ->
                    // Proceed to retrieve the Nodes of the Parent Node
                    _state.update { backupsState ->
                        refreshNodes(parentNodeHandle).let { updatedNodes ->
                            backupsState.copy(
                                backupsHandle = parentNodeHandle,
                                triggerBackPress = true,
                                nodes = updatedNodes,
                            )
                        }
                    }
                } ?: run { onExitBackups(true) }
            }
        }
    }

    /**
     * Notifies [BackupsState.shouldExitBackups] that the Backups screen has acknowledged to exit the screen
     */
    fun exitBackupsHandled() {
        onExitBackups(false)
    }

    /**
     * Updates the value of [BackupsState.shouldExitBackups]
     *
     * @param exitBackups Whether the User should exit the Backups page or not
     */
    private fun onExitBackups(exitBackups: Boolean) {
        _state.update { it.copy(shouldExitBackups = exitBackups) }
    }

    /**
     * Notifies [BackupsState.triggerBackPress] that the Backups screen has handled the Back Press
     * behavior by setting its value to false
     */
    fun triggerBackPressHandled() {
        _state.update { it.copy(triggerBackPress = false) }
    }

    /**
     * Updates the value of [BackupsState.nodes]
     *
     * @param nodes The List of Nodes
     */
    private fun setNodes(nodes: List<MegaNode>) {
        _state.update { it.copy(nodes = nodes) }
    }

    /**
     * Notifies [BackupsState.backupsHandle] that the Backups screen has handled the Backups Handle
     *
     * @param nodeHandle The new Backups Handle
     */
    fun updateBackupsHandle(nodeHandle: Long) {
        onBackupsHandle(nodeHandle)
    }

    /**
     * Checks whether the the Backups screen is currently on the Backups Folder level by comparing
     * the Node Handles of [BackupsState.backupsHandle] and [myBackupsFolderNode]
     *
     * @return true if both values are equal or [BackupsState.backupsHandle] is -1L, and false if otherwise
     */
    fun isCurrentlyOnBackupFolderLevel(): Boolean = with(_state.value) {
        (this.backupsHandle == myBackupsFolderNode.longValue) || this.backupsHandle == -1L
    }

    /**
     * Given a [Long], the function updates the value of [BackupsState.backupsHandle]
     *
     * @param backupsHandle The Backups Handle
     */
    private fun onBackupsHandle(backupsHandle: Long) {
        _state.update { it.copy(backupsHandle = backupsHandle) }
    }

    /**
     * Notifies [BackupsState.hideMultipleItemSelection] that the Backups screen has handled the hiding
     * of the Multiple Item Selection by setting its value to false
     *
     */
    fun hideMultipleItemSelectionHandled() {
        _state.update { it.copy(hideMultipleItemSelection = false) }
    }

    /**
     * Get Cloud Sort Order
     */
    fun getOrder() = runBlocking { getCloudSortOrder() }

    /**
     * Mark handled pending refresh
     */
    fun markHandledPendingRefresh() {
        _state.update { it.copy(isPendingRefresh = false) }
    }
}