package mega.privacy.android.app.presentation.rubbishbin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import java.util.Stack
import javax.inject.Inject

/**
 * [ViewModel] class associated to RubbishBinFragment
 *
 * @param getRubbishBinChildrenNode [GetRubbishBinChildrenNode] Fetch the rubbish bin nodes
 * @param monitorNodeUpdates Monitor node updates
 * @param getRubbishBinParentNodeHandle [GetParentNodeHandle] Fetch parent handle
 * @param getRubbishBinChildren [GetRubbishBinChildren] Fetch Rubbish Bin [Node]
 * @param setViewType [SetViewType] to set view type
 * @param monitorViewType [MonitorViewType] check view type
 */
@HiltViewModel
class RubbishBinViewModel @Inject constructor(
    private val getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val getRubbishBinParentNodeHandle: GetParentNodeHandle,
    private val getRubbishBinChildren: GetRubbishBinChildren,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
) : ViewModel() {

    /**
     * The RubbishBin UI State
     */
    private val _state = MutableStateFlow(RubbishBinState())

    /**
     * The RubbishBin UI State accessible outside the ViewModel
     */
    val state: StateFlow<RubbishBinState> = _state

    /**
     * Stack to maintain folder navigation clicks
     */
    private val lastPositionStack = Stack<Int>()

    init {
        nodeUpdates()
        checkViewType()
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
     * Uses MonitorNodeUpdates to observe any Node updates
     * A received Node update will refresh the list of nodes
     */
    private fun nodeUpdates() {
        viewModelScope.launch {
            refreshNodes()
            monitorNodeUpdates().collect {
                checkForDeletedNodes(it.changes)
            }
        }
    }

    /**
     * Set the current rubbish bin handle to the UI state
     *
     * @param handle the id of the current rubbish bin parent handle to set
     */
    fun setRubbishBinHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(rubbishBinHandle = handle) }
        refreshNodes()
    }

    /**
     * Retrieves the list of Nodes
     * Call the Use Case [getRubbishBinChildrenNode] to retrieve and return the list of Nodes
     *
     * @return a List of Inbox Nodes
     */
    fun refreshNodes() {
        viewModelScope.launch {
            val nodeList = getNodeUiItems(getRubbishBinChildren(_state.value.rubbishBinHandle))
            _state.update {
                it.copy(
                    nodes = getRubbishBinChildrenNode(_state.value.rubbishBinHandle) ?: emptyList(),
                    parentHandle = getRubbishBinParentNodeHandle(_state.value.rubbishBinHandle),
                    nodeList = nodeList,
                    sortOrder = getCloudSortOrder()
                )
            }
        }
    }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<Node>): List<NodeUIItem> {
        return nodeList.map {
            NodeUIItem(node = it, isSelected = false, isInvisible = false)
        }
    }

    /**
     * Handles back click of rubbishBinFragment
     */
    fun onBackPressed() {
        _state.value.parentHandle?.let {
            setRubbishBinHandle(it)
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
        setRubbishBinHandle(handle)
        onItemPerformedClicked()
    }

    /**
     * When item is clicked on activity
     */
    fun onItemPerformedClicked() {
        _state.update {
            it.copy(
                megaNode = null,
                itemIndex = -1
            )
        }
    }

    /**
     * This will update handle for rubbishBin if any node is deleted from browser and
     * we are in same screen else will simply refresh nodes with parentID
     * if restored and we are inside folder, it will simply refresh rubbish node
     * @param changes [Map] of [Node], list of [NodeChanges]
     */
    private fun checkForDeletedNodes(changes: Map<Node, List<NodeChanges>>) {
        changes.forEach { (key, value) ->
            if (value.contains(NodeChanges.Remove) && _state.value.rubbishBinHandle == key.id.longValue) {
                setRubbishBinHandle(key.parentId.longValue)
                return@forEach
            } else if (value.contains(NodeChanges.Parent) && _state.value.rubbishBinHandle == key.id.longValue) {
                setRubbishBinHandle(-1)
                return@forEach
            }
        }
        refreshNodes()
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
            _state.value.nodeList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        if (_state.value.isInSelection) {
            updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
        } else {
            val megaNode = _state.value.nodes.find { it.handle == nodeUIItem.id.longValue }
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        megaNode = megaNode,
                        itemIndex = index
                    )
                }
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
            _state.value.nodeList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        val newNodesList = _state.value.nodeList.updateItemAt(index = index, item = nodeUIItem)
        val selectedNodeList = _state.value.selectedNodeHandles.toMutableList()
        selectedNodeList.add(nodeUIItem.id.longValue)
        _state.update {
            it.copy(
                selectedFileNodes = if (nodeUIItem.node is FileNode) it.selectedFileNodes + 1 else it.selectedFileNodes,
                selectedFolderNodes = if (nodeUIItem.node is FolderNode) it.selectedFolderNodes + 1 else it.selectedFolderNodes,
                nodeList = newNodesList,
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
        var totalSelectedFileNode = _state.value.selectedFileNodes
        var totalSelectedFolderNode = _state.value.selectedFolderNodes
        val selectedNodeHandle = _state.value.selectedNodeHandles.toMutableList()
        if (nodeUIItem.isSelected) {
            if (nodeUIItem.node is FolderNode) {
                totalSelectedFolderNode = _state.value.selectedFolderNodes + 1
            } else if (nodeUIItem.node is FileNode) {
                totalSelectedFileNode = _state.value.selectedFileNodes + 1
            }
            selectedNodeHandle.add(nodeUIItem.node.id.longValue)
        } else {
            if (nodeUIItem.node is FolderNode) {
                totalSelectedFolderNode = _state.value.selectedFolderNodes - 1
            } else if (nodeUIItem.node is FileNode) {
                totalSelectedFileNode = _state.value.selectedFileNodes - 1
            }
            selectedNodeHandle.remove(nodeUIItem.node.id.longValue)
        }
        val newNodesList = _state.value.nodeList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedFolderNodes = totalSelectedFolderNode,
                selectedFileNodes = totalSelectedFileNode,
                nodeList = newNodesList,
                isInSelection = totalSelectedFolderNode > 0 || totalSelectedFileNode > 0,
                selectedNodeHandles = selectedNodeHandle
            )
        }
    }

    /**
     * Select all [NodeUIItem]
     */
    fun selectAllNodes() {
        val selectedNodeList = selectAllNodesUiList()
        var totalFolderNode = 0
        var totalFileNode = 0
        val selectedNodeHandle = mutableListOf<Long>()
        selectedNodeList.forEach {
            if (it.node is FileNode) totalFolderNode++
            if (it.node is FolderNode) totalFileNode++
            selectedNodeHandle.add(it.node.id.longValue)
        }
        _state.update {
            it.copy(
                nodeList = selectedNodeList,
                isInSelection = true,
                selectedFolderNodes = totalFolderNode,
                selectedFileNodes = totalFileNode,
                selectedNodeHandles = selectedNodeHandle
            )
        }
    }

    /**
     * Returns list of all selected Nodes
     */
    private fun selectAllNodesUiList(): List<NodeUIItem> {
        return _state.value.nodeList.map {
            it.copy(isSelected = true)
        }
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

    /**
     * Clear the selections of items from NodesUiList and reset count of other Nodes
     */
    fun clearAllSelectedNodes() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    nodeList = clearNodeUiItemList(),
                    selectedFileNodes = 0,
                    selectedFolderNodes = 0,
                    isInSelection = false,
                    selectedNodeHandles = emptyList(),
                    selectedMegaNodes = null
                )
            }
        }
    }

    /**
     * Clear the selections of items from NodesUiList
     */
    private fun clearNodeUiItemList(): List<NodeUIItem> {
        return _state.value.nodeList.map {
            it.copy(isSelected = false)
        }
    }

    /**
     * This method will get List of [MegaNode] from handle to restore data
     */
    fun onRestoreClicked() {
        val megaNodeList = mutableListOf<MegaNode>()
        _state.value.selectedNodeHandles.forEach {
            val selectedMegaNode = state.value.nodes.find { megaNode -> megaNode.handle == it }
            selectedMegaNode?.let { megaNode ->
                megaNodeList.add(megaNode)
            }
        }
        updateSelectedMegaNode(megaNodeList)
    }

    /**
     * Update selected mega node to state
     */
    private fun updateSelectedMegaNode(selectedMegaNode: List<MegaNode>?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedMegaNodes = selectedMegaNode
                )
            }
        }
    }
}
