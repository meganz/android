package mega.privacy.android.app.presentation.shares.links

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to LinksComposeFragment
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LinksViewModel @Inject constructor(
    private val monitorPublicLinksUseCase: MonitorPublicLinksUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val handleOptionClickMapper: HandleOptionClickMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val currentFlow = Channel<Flow<LinksUiState>>()

    /** Private UI state */
    private val _state = MutableStateFlow(LinksUiState())

    /** Public immutable UI State */
    val state = _state.asStateFlow()

    /** Stack to maintain folder navigation clicks */
    private val handleStack = Stack<Long>()

    init {
        viewModelScope.launch {
            _state.emitAll(currentFlow.consumeAsFlow().flatMapLatest { it })
        }
        monitorConnectivity()
        observeFlow(publicLinks())
    }

    private fun observeFlow(flow: Flow<LinksUiState>) {
        viewModelScope.launch {
            currentFlow.send(flow)
        }
    }

    private fun publicLinks() = monitorPublicLinksUseCase().map { list ->
        _state.value.copy(
            nodesList = getNodeUiItems(list),
            sortOrder = getSortOrder(),
            parentNode = null
        )
    }

    private fun openFolder(parentNode: PublicLinkFolder) {
        _state.update {
            it.copy(
                parentNode = parentNode
            )
        }
        observeFlow(childLinks(parentNode))
    }

    private fun closeFolder(currentFolder: PublicLinkFolder) {
        observeFlow(currentFolder.parent?.let { childLinks(it) } ?: publicLinks())
    }

    private fun childLinks(parentNode: PublicLinkFolder) =
        parentNode.children.map { list ->
            _state.value.copy(
                parentNode = parentNode,
                nodesList = getNodeUiItems(list),
                sortOrder = getSortOrder()
            )
        }

    private suspend fun getSortOrder() =
        if (state.value.parentNode == null) getLinksSortOrder() else getCloudSortOrder()

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect {
                _state.update { state -> state.copy(isConnected = it) }
            }
        }
    }

    /**
     * This will map list of [PublicLinkNode] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<PublicLinkNode>): List<NodeUIItem<PublicLinkNode>> {
        return nodeList.mapIndexed { index, node ->
            val isSelected = state.value.selectedNodeHandles.contains(node.id.longValue)
            NodeUIItem(
                node = node,
                isSelected = isSelected,
            )
        }
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<PublicLinkNode>) {
        runCatching {
            val index =
                state.value.nodesList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
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

                } else if (nodeUIItem.node is PublicLinkFolder) {
                    onFolderItemClicked(nodeUIItem.node)
                }
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Performs action when folder is clicked from adapter
     * @param linkFolderNode node handle
     */
    private fun onFolderItemClicked(
        linkFolderNode: PublicLinkFolder,
    ) {
        viewModelScope.launch {
            val handle = linkFolderNode.id.longValue
            handleStack.push(handle)
            openFolder(linkFolderNode)
        }
    }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem<PublicLinkNode>) {
        val index =
            _state.value.nodesList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
    }


    /**
     * Handles Back Navigation events
     */
    fun performBackNavigation() {
        _state.value.parentNode?.let {
            closeFolder(it)
            handleStack.takeIf { stack -> stack.isNotEmpty() }?.pop()
            _state.update { it.copy(updateToolbarTitleEvent = triggered) }
        } ?: run {
            _state.update { it.copy(exitLinksPageEvent = triggered) }
        }
    }

    /**
     * When item is clicked on activity
     */
    fun onItemPerformedClicked() {
        _state.update {
            it.copy(
                currentFileNode = null,
                itemIndex = -1,
            )
        }
    }

    /**
     * This will refresh link nodes and update [LinksUiState.nodesList]
     */
    fun refreshLinkNodes() {
        observeFlow(publicLinks())
    }

    /**
     * Select a node
     * @param nodeUIItem
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun selectNode(nodeUIItem: NodeUIItem<PublicLinkNode>): Pair<Int, Int> {
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
     * Unselect a node
     * @param nodeUIItem
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun unSelectNode(nodeUIItem: NodeUIItem<PublicLinkNode>): Pair<Int, Int> {
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
     * Handles option info based on [MenuItem]
     * @param item [MenuItem]
     */
    fun onOptionItemClicked(item: MenuItem) {
        viewModelScope.launch {
            val optionsItemInfo = handleOptionClickMapper(
                item = item,
                selectedNodeHandle = state.value.selectedNodeHandles
            )
            if (getFeatureFlagValueUseCase(AppFeatures.DownloadWorker) && optionsItemInfo.optionClickedType == OptionItems.DOWNLOAD_CLICKED) {
                _state.update {
                    it.copy(
                        downloadEvent = triggered(
                            TransferTriggerEvent.StartDownloadNode(optionsItemInfo.selectedNode)
                        )
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        optionsItemInfo = optionsItemInfo
                    )
                }
            }
        }
    }

    /**
     * Select all [NodeUIItem]
     */
    fun selectAllNodes() {
        val selectedNodeList = selectAllNodesUiList()
        var totalFolderNode = 0
        var totalFileNode = 0
        val selectedNodeHandles = mutableListOf<Long>()
        selectedNodeList.forEach {
            if (it.node is FileNode) totalFolderNode++
            if (it.node is FolderNode) totalFileNode++
            selectedNodeHandles.add(it.node.id.longValue)
        }
        _state.update {
            it.copy(
                nodesList = selectedNodeList,
                isInSelection = true,
                selectedFolderNodes = totalFolderNode,
                selectedFileNodes = totalFileNode,
                selectedNodeHandles = selectedNodeHandles
            )
        }
    }

    /**
     * Returns list of all selected Nodes
     */
    private fun selectAllNodesUiList(): List<NodeUIItem<PublicLinkNode>> {
        return _state.value.nodesList.map {
            it.copy(isSelected = true)
        }
    }

    /**
     * Clear All [NodeUIItem]
     */
    fun clearAllNodes() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    nodesList = clearNodeUiItemList(),
                    selectedFileNodes = 0,
                    selectedFolderNodes = 0,
                    isInSelection = false,
                    selectedNodeHandles = emptyList(),
                    optionsItemInfo = null
                )
            }
        }
    }

    /**
     * Clear the selections of items from NodesUiList
     */
    private fun clearNodeUiItemList(): List<NodeUIItem<PublicLinkNode>> {
        return _state.value.nodesList.map {
            it.copy(isSelected = false)
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeInSelectionState(nodeUIItem: NodeUIItem<PublicLinkNode>, index: Int) {
        nodeUIItem.isSelected = !nodeUIItem.isSelected
        val selectedNodeHandles = state.value.selectedNodeHandles.toMutableList()
        val pair = if (nodeUIItem.isSelected) {
            selectedNodeHandles.add(nodeUIItem.node.id.longValue)
            selectNode(nodeUIItem)
        } else {
            selectedNodeHandles.remove(nodeUIItem.node.id.longValue)
            unSelectNode(nodeUIItem)
        }
        val newNodesList = _state.value.nodesList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedFileNodes = pair.first,
                selectedFolderNodes = pair.second,
                nodesList = newNodesList,
                isInSelection = pair.first > 0 || pair.second > 0,
                selectedNodeHandles = selectedNodeHandles,
                optionsItemInfo = null
            )
        }
    }

    /**
     * Consume download event
     */
    fun consumeDownloadEvent() {
        _state.update {
            it.copy(downloadEvent = consumed())
        }
    }

    /**
     * Consumes the Exit Links Page Event
     */
    fun consumeExitLinksPageEvent() {
        _state.update { it.copy(exitLinksPageEvent = consumed) }
    }

    /**
     * Consumes the Update Toolbar Title Event
     */
    fun consumeUpdateToolbarTitleEvent() {
        _state.update { it.copy(updateToolbarTitleEvent = consumed) }
    }

}