package mega.privacy.android.app.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.inbox.model.InboxState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class associated to InboxFragment
 *
 * @property getChildrenNode Get Children Node
 * @property getCloudSortOrder Get Cloud Sort Order
 * @property getInboxNode Get Inbox Node
 * @property getNodeByHandle Get Node By Handle
 * @property getParentNodeHandle Get Parent Node Handle
 * @property monitorBackupFolder Monitor Backup Folder
 * @property monitorNodeUpdates Monitor Global Node Updates
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getInboxNode: GetInboxNode,
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    /**
     * The Inbox UI State
     */
    private val _state = MutableStateFlow(InboxState())

    /**
     * The Inbox UI State accessible outside the ViewModel
     */
    val state: StateFlow<InboxState> = _state

    /**
     * The Root Inbox Node
     */
    private var rootInboxNode: MegaNode? = null

    /**
     * Perform the following actions upon ViewModel initialization
     */
    init {
        getRootInboxNode()
        observeNodeUpdates()
        observeMyBackupsFolderUpdates()
    }

    /**
     * Uses [monitorNodeUpdates] to observe any Node updates
     *
     * A received Node update will refresh the list of nodes and hide the multiple item selection feature
     */
    private fun observeNodeUpdates() = viewModelScope.launch {
        monitorNodeUpdates().collect {
            _state.update { inboxState ->
                refreshNodes().let { updatedNodes ->
                    inboxState.copy(
                        hideMultipleItemSelection = true,
                        nodes = updatedNodes,
                    )
                }
            }
        }
    }

    /**
     * Retrieves the Root Inbox Node
     */
    private fun getRootInboxNode() = viewModelScope.launch {
        rootInboxNode = getInboxNode()
    }

    /**
     * Uses [monitorBackupFolder] to observe any updates in the My Backups folder
     */
    private fun observeMyBackupsFolderUpdates() = viewModelScope.launch {
        monitorBackupFolder()
            .map {
                // Emit an Invalid Handle if the Result is a failure
                Timber.e("Unable to retrieve the My Backups Folder")
                it.getOrDefault(NodeId(MegaApiJava.INVALID_HANDLE))
            }
            .collectLatest { myBackupsFolderNodeId ->
                Timber.d("The My Backups ID is: ${myBackupsFolderNodeId.id}")
                onMyBackupsFolderUpdateReceived(myBackupsFolderNodeId)
            }
    }

    /**
     *
     * Processes updates received from the My Backups Folder
     *
     * If the current Parent Node ID is invalid, use the My Backups Folder Node ID to refresh the
     * list of Nodes
     *
     * Update the UI State values after performing a successful refresh
     *
     * @param nodeId The updated My Backups Node ID
     */
    private suspend fun onMyBackupsFolderUpdateReceived(nodeId: NodeId) {
        _state.update { inboxState ->
            if (inboxState.currentParentNodeId.id == -1L) {
                refreshNodes(nodeId).let { updatedNodes ->
                    inboxState.copy(
                        myBackupsFolderNodeId = nodeId,
                        currentParentNodeId = nodeId,
                        nodes = updatedNodes,
                    )
                }
            } else {
                refreshNodes().let { updatedNodes ->
                    inboxState.copy(
                        myBackupsFolderNodeId = nodeId,
                        nodes = updatedNodes
                    )
                }
            }
        }
    }

    /**
     * Refreshes the list of Inbox Nodes
     */
    fun refreshInboxNodes() = viewModelScope.launch {
        refreshNodes().also { setNodes(it) }
    }

    /**
     * Retrieves the list of Nodes by performing the following steps:
     *
     * 2. Check if [InboxState.currentParentNodeId] equals -1L or [MegaApiJava.INVALID_HANDLE]. If either condition is true, return an empty list
     * 3. Call the Use Case [getNodeByHandle] to retrieve the Parent Node from [InboxState.currentParentNodeId]. If the Parent Node is null, return an empty list
     * 4. Call the Use Case [getChildrenNode] to retrieve and return the list of Nodes under the Parent Node
     *
     * @param nodeId The Node ID used to retrieve the current list of Nodes. Defaults to [InboxState.currentParentNodeId]
     *
     * @return a List of Inbox Nodes
     */
    private suspend fun refreshNodes(nodeId: NodeId = _state.value.currentParentNodeId): List<MegaNode> =
        nodeId.let { parentNodeId ->
            if (isValidNodeId(parentNodeId)) {
                getNodeByHandle(parentNodeId.id)?.let { retrievedParentNode ->
                    getChildrenNode(
                        parent = retrievedParentNode,
                        order = getCloudSortOrder(),
                    )
                }.orEmpty()
            } else {
                emptyList()
            }
        }

    /**
     * Checks whether the [NodeId] is valid or not
     *
     * @param nodeId The Node ID
     * @return true if the Node ID satisfies either condition, and false if otherwise
     */
    private fun isValidNodeId(nodeId: NodeId): Boolean =
        nodeId.id != -1L || nodeId.id != MegaApiJava.INVALID_HANDLE

    /**
     * Handles the Back Press Behavior from Inbox after checking certain conditions
     */
    fun handleBackPress() {
        viewModelScope.launch {
            // Either one of the following conditions will constitute the user exiting the screen:
            // 1. The Grandparent Node ID does not exist
            // 2. The Root Inbox Node does not exist
            // 3. The Grandparent Node ID is the same with the Root Inbox ID
            val grandParentNodeId = getParentNodeHandle(_state.value.currentParentNodeId.id)
            if (grandParentNodeId == null || rootInboxNode == null || (grandParentNodeId == rootInboxNode?.handle)) {
                onExitInbox(true)
            } else {
                // Otherwise, proceed to retrieve the Nodes of the Grandparent Node
                _state.update { inboxState ->
                    refreshNodes(NodeId(grandParentNodeId)).let { updatedNodes ->
                        inboxState.copy(
                            currentParentNodeId = NodeId(grandParentNodeId),
                            triggerBackPress = true,
                            nodes = updatedNodes,
                        )
                    }
                }
            }
        }
    }

    /**
     * Notifies [InboxState.shouldExitInbox] that the Inbox screen has acknowledged to exit the screen
     */
    fun exitInboxHandled() {
        onExitInbox(false)
    }

    /**
     * Updates the value of [InboxState.shouldExitInbox]
     *
     * @param exitInbox Whether the User should exit the Inbox or not
     */
    private fun onExitInbox(exitInbox: Boolean) {
        _state.update { it.copy(shouldExitInbox = exitInbox) }
    }

    /**
     * Notifies [InboxState.triggerBackPress] that the Inbox screen has handled the Back Press
     * behavior by setting its value to false
     */
    fun triggerBackPressHandled() {
        _state.update { it.copy(triggerBackPress = false) }
    }

    /**
     * Updates the value of [InboxState.nodes]
     *
     * @param nodes The List of Nodes
     */
    private fun setNodes(nodes: List<MegaNode>) {
        _state.update { it.copy(nodes = nodes) }
    }

    /**
     * Notifies [InboxState.currentParentNodeId] that the Inbox screen has handled the current Parent Node ID
     *
     * @param nodeId The new current Parent Node ID
     */
    fun updateCurrentParentNodeId(nodeId: Long) {
        onCurrentParentNodeId(nodeId)
    }

    /**
     * Checks whether the the Inbox screen is currently on the Backups Folder level by comparing the values of
     * [InboxState.currentParentNodeId] and [InboxState.myBackupsFolderNodeId]
     *
     * @return true if both values are equal or [InboxState.currentParentNodeId] is -1L, and false if otherwise
     */
    fun isCurrentlyOnBackupFolderLevel(): Boolean = with(_state.value) {
        (this.currentParentNodeId == this.myBackupsFolderNodeId) || this.currentParentNodeId.id == -1L
    }

    /**
     * Given a [Long], the function updates the value of [InboxState.currentParentNodeId]
     *
     * @param currentParentNodeId The current Parent Node ID
     */
    private fun onCurrentParentNodeId(currentParentNodeId: Long) {
        _state.update { it.copy(currentParentNodeId = NodeId(currentParentNodeId)) }
    }

    /**
     * Notifies [InboxState.hideMultipleItemSelection] that the Inbox screen has handled the hiding
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
}