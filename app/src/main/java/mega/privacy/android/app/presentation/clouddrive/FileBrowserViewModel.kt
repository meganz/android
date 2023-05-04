package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetFileBrowserChildrenUseCase
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to FileBrowserFragment
 *
 * @param getRootFolder Fetch the root node
 * @param getBrowserChildrenNode Fetch the cloud drive nodes
 * @param monitorMediaDiscoveryView Monitor media discovery view settings
 * @param monitorNodeUpdates Monitor node updates
 * @param getFileBrowserParentNodeHandle To get parent handle of current node
 * @param getIsNodeInRubbish To get current node is in rubbish
 * @param getFileBrowserChildrenUseCase [GetFileBrowserChildrenUseCase]
 * @param getCloudSortOrder [GetCloudSortOrder]
 * @param monitorViewType [MonitorViewType] check view type
 * @param setViewType [SetViewType] to set view type
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val getRootFolder: GetRootFolder,
    private val getBrowserChildrenNode: GetBrowserChildrenNode,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val getFileBrowserParentNodeHandle: GetParentNodeHandle,
    private val getIsNodeInRubbish: IsNodeInRubbish,
    private val getFileBrowserChildrenUseCase: GetFileBrowserChildrenUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
) : ViewModel() {

    private val _state = MutableStateFlow(FileBrowserState())

    /**
     * State flow
     */
    val state: StateFlow<FileBrowserState> = _state

    /**
     * Stack to maintain folder navigation clicks
     */
    private val lastPositionStack = Stack<Int>()
    private val handleStack = Stack<Long>()

    init {
        monitorMediaDiscovery()
        refreshNodes()
        monitorFileBrowserChildrenNodes()
        checkViewType()
    }

    /**
     * This will monitor media discovery from [MonitorMediaDiscoveryView] and update
     * [FileBrowserState.mediaDiscoveryViewSettings]
     */
    private fun monitorMediaDiscovery() {
        viewModelScope.launch {
            monitorMediaDiscoveryView().collect { mediaDiscoveryViewSettings ->
                _state.update {
                    it.copy(
                        mediaDiscoveryViewSettings = mediaDiscoveryViewSettings
                            ?: MediaDiscoveryViewSettings.INITIAL.ordinal
                    )
                }
            }
        }
    }

    /**
     * This method will monitor view type and update it on state
     */
    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    /**
     * This will monitor FileBrowserNodeUpdates from [MonitorNodeUpdates] and
     * will update [FileBrowserState.nodes]
     */
    private fun monitorFileBrowserChildrenNodes() {
        viewModelScope.launch {
            monitorNodeUpdates().collect {
                checkForNodeIsInRubbish(it.changes)
            }
        }
    }

    /**
     * This will update handle for fileBrowser if any node is deleted from browser and
     * moved to rubbish bin
     * we are in same screen else will simply refresh nodes with parentID
     * @param changes [Map] of [Node], list of [NodeChanges]
     */
    private suspend fun checkForNodeIsInRubbish(changes: Map<Node, List<NodeChanges>>) {
        changes.forEach { (node, _) ->
            if (node is FolderNode) {
                if (node.isInRubbishBin && _state.value.fileBrowserHandle == node.id.longValue) {
                    while (handleStack.isNotEmpty() && getIsNodeInRubbish(handleStack.peek())) {
                        handleStack.pop()
                    }
                    handleStack.takeIf { stack -> stack.isNotEmpty() }?.peek()?.let { parent ->
                        setBrowserParentHandle(parent)
                        return
                    }
                }
            }
        }
        setPendingRefreshNodes()
    }

    private fun setPendingRefreshNodes() {
        _state.update { it.copy(isPendingRefresh = true) }
    }

    /**
     * Set the current browser handle to the UI state
     *
     * @param handle the id of the current browser handle to set
     */
    fun setBrowserParentHandle(handle: Long) = viewModelScope.launch {
        handleStack.push(handle)
        _state.update {
            it.copy(
                fileBrowserHandle = handle,
                mediaHandle = handle
            )
        }
        refreshNodes()
    }

    /**
     * Get the browser parent handle
     * If not previously set, set the browser parent handle to root handle
     *
     * @return the handle of the browser section
     */
    fun getSafeBrowserParentHandle(): Long = runBlocking {
        if (_state.value.fileBrowserHandle == -1L) {
            setBrowserParentHandle(getRootFolder()?.handle ?: MegaApiJava.INVALID_HANDLE)
        }
        return@runBlocking _state.value.fileBrowserHandle
    }

    /**
     * If a folder only contains images or videos, then go to MD mode directly
     *
     * @param parentHandle the folder handle
     * @param mediaDiscoveryViewSettings [mediaDiscoveryViewSettings]
     * @return true is should enter MD mode, otherwise is false
     */
    suspend fun shouldEnterMediaDiscoveryMode(
        parentHandle: Long,
        mediaDiscoveryViewSettings: Int,
    ): Boolean =
        getBrowserChildrenNode(parentHandle)?.let { nodes ->
            if (nodes.isEmpty() || mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.DISABLED.ordinal) {
                false
            } else {
                nodes.firstOrNull { node ->
                    node.isFolder
                            || MimeTypeList.typeForName(node.name).isSvgMimeType
                            || (!MimeTypeList.typeForName(node.name).isImage
                            && !MimeTypeList.typeForName(node.name).isVideoMimeType)
                }?.let {
                    false
                } ?: true
            }
        } ?: false

    /**
     * This will refresh file browser nodes and update [FileBrowserState.nodes]
     */
    fun refreshNodes() {
        viewModelScope.launch {
            val nodeList =
                getNodeUiItems(getFileBrowserChildrenUseCase(_state.value.fileBrowserHandle))
            _state.update {
                it.copy(
                    nodes = getBrowserChildrenNode(_state.value.fileBrowserHandle) ?: emptyList(),
                    parentHandle = getFileBrowserParentNodeHandle(_state.value.fileBrowserHandle),
                    nodesList = nodeList,
                    sortOrder = getCloudSortOrder()
                )
            }
        }
    }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<Node>): List<NodeUIItem> {
        val existingNodeList = state.value.nodesList
        return nodeList.mapIndexed { index, node ->
            val isSelected = state.value.selectedNodeHandles.contains(node.id.longValue)
            NodeUIItem(
                node = node,
                isSelected = if (existingNodeList.size > index) isSelected else false,
                isInvisible = if (existingNodeList.size > index) existingNodeList[index].isInvisible else false
            )
        }
    }

    /**
     * Handles back click of rubbishBinFragment
     */
    fun onBackPressed() {
        _state.value.parentHandle?.let {
            setBrowserParentHandle(it)
            handleStack.takeIf { stack -> stack.isNotEmpty() }?.pop()
        }
    }

    /**
     * Pop scroll position for previous depth
     *
     * @return last position saved
     */
    fun popLastPositionStack(): Int = lastPositionStack.takeIf { it.isNotEmpty() }?.pop() ?: 0

    /**
     * Push lastPosition to stack
     * @param lastPosition last position to be added to stack
     */
    private fun pushPositionOnStack(lastPosition: Int) {
        lastPositionStack.push(lastPosition)
    }

    /**
     * Performs action when folder is clicked from adapter
     * @param lastFirstVisiblePosition visible position based on listview type
     * @param handle node handle
     */
    fun onFolderItemClicked(lastFirstVisiblePosition: Int, handle: Long) {
        pushPositionOnStack(lastFirstVisiblePosition)
        setBrowserParentHandle(handle)
    }

    /**
     * Updates the value of [FileBrowserState.currentViewType]
     *
     * @param newViewType The new [ViewType]
     */
    fun setCurrentViewType(newViewType: ViewType) {
        _state.update { it.copy(currentViewType = newViewType) }
    }

    /**
     * Mark handled pending refresh
     *
     */
    fun markHandledPendingRefresh() {
        _state.update { it.copy(isPendingRefresh = false) }
    }

    /**
     * Select all [NodeUIItem]
     */
    fun selectAllNodes() {

    }

    /**
     *  Changes the Transfer Over Quota banner visibility based on certain conditions
     */
    fun changeTransferOverQuotaBannerVisibility() {

    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem) {
        val index =
            _state.value.nodesList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        if (_state.value.isInSelection) {
            updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
        } else {
            if (nodeUIItem.node is FileNode) {
                _state.update {
                    it.copy(
                        itemIndex = index,
                        currentFileNode = nodeUIItem.node
                    )
                }

            } else {
                onFolderItemClicked(0, nodeUIItem.id.longValue)
            }
        }
    }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem) {
        nodeUIItem.isSelected = true
        val index =
            _state.value.nodesList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        val newNodesList = _state.value.nodesList.updateItemAt(index = index, item = nodeUIItem)
        val selectedNodeList = _state.value.selectedNodeHandles.toMutableList()
        selectedNodeList.add(nodeUIItem.id.longValue)
        _state.update {
            it.copy(
                selectedFileNodes = if (nodeUIItem.node is FileNode) it.selectedFileNodes + 1 else it.selectedFileNodes,
                selectedFolderNodes = if (nodeUIItem.node is FolderNode) it.selectedFolderNodes + 1 else it.selectedFolderNodes,
                nodesList = newNodesList,
                isInSelection = true,
                selectedNodeHandles = selectedNodeList
            )
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeInSelectionState(nodeUIItem: NodeUIItem, index: Int) {
        nodeUIItem.isSelected = !nodeUIItem.isSelected
        val selectedNodeHandle = state.value.selectedNodeHandles.toMutableList()
        val pair = if (nodeUIItem.isSelected) {
            selectedNodeHandle.add(nodeUIItem.node.id.longValue)
            selectNode(nodeUIItem)
        } else {
            selectedNodeHandle.remove(nodeUIItem.node.id.longValue)
            unSelectNode(nodeUIItem)
        }
        val newNodesList = _state.value.nodesList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedFileNodes = pair.first,
                selectedFolderNodes = pair.second,
                nodesList = newNodesList,
                isInSelection = pair.first > 0 || pair.second > 0,
                selectedNodeHandles = selectedNodeHandle
            )
        }
    }

    /**
     * select a node
     * @param nodeUIItem
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun selectNode(nodeUIItem: NodeUIItem): Pair<Int, Int> {
        var totalSelectedFileNode = state.value.selectedFileNodes
        var totalSelectedFolderNode = state.value.selectedFolderNodes
        if (nodeUIItem.node is FolderNode) {
            totalSelectedFolderNode = _state.value.selectedFolderNodes + 1
        } else if (nodeUIItem.node is FileNode) {
            totalSelectedFileNode = _state.value.selectedFileNodes + 1
        }
        return Pair(totalSelectedFileNode, totalSelectedFolderNode)
    }

    /**
     * un select a node
     * @param nodeUIItem
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun unSelectNode(nodeUIItem: NodeUIItem): Pair<Int, Int> {
        var totalSelectedFileNode = state.value.selectedFileNodes
        var totalSelectedFolderNode = state.value.selectedFolderNodes
        if (nodeUIItem.node is FolderNode) {
            totalSelectedFolderNode = _state.value.selectedFolderNodes - 1
        } else if (nodeUIItem.node is FileNode) {
            totalSelectedFileNode = _state.value.selectedFileNodes - 1
        }
        return Pair(totalSelectedFileNode, totalSelectedFolderNode)
    }

    /**
     * This method will toggle view type
     */
    fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }
}