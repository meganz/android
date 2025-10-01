package mega.privacy.android.app.presentation.shares.links

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNodeType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.MonitorFolderNodeDeleteUpdatesUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to LinksComposeFragment
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LinksViewModel @Inject constructor(
    private val monitorPublicLinksUseCase: MonitorPublicLinksUseCase,
    private val monitorFolderNodeDeleteUpdatesUseCase: MonitorFolderNodeDeleteUpdatesUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val handleOptionClickMapper: HandleOptionClickMapper,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase,
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
) : ViewModel() {

    private val currentFlow = Channel<Flow<LinksUiState>>()

    /** Private UI state */
    private val _state = MutableStateFlow(LinksUiState())

    /** Public immutable UI State */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.emitAll(currentFlow.consumeAsFlow().flatMapLatest { it })
        }
        monitorConnectivity()
        observeFlow(publicLinks())
        monitorFolderNodeDeleteUpdates()
    }

    private fun monitorFolderNodeDeleteUpdates() {
        viewModelScope.launch {
            monitorFolderNodeDeleteUpdatesUseCase()
                .catch {
                    Timber.e(it)
                }
                .collect {
                    val currentNodeHandle = getCurrentNodeHandle()
                    if (it.contains(currentNodeHandle)) {
                        performBackNavigation()
                    }
                }
        }
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
            parentNode = null,
            isLoading = false,
            openedFolderNodeHandles = setOf(-1L),
        )
    }

    private fun openFolder(parentNode: PublicLinkFolder) {
        _state.update {
            it.copy(
                parentNode = parentNode,
                isLoading = true,
                openedFolderNodeHandles = it.openedFolderNodeHandles.toMutableSet()
                    .apply { add(parentNode.id.longValue) },
                updateToolbarTitleEvent = triggered
            )
        }
        observeFlow(childLinks(parentNode))
    }

    private fun closeFolder(currentFolder: PublicLinkFolder) {
        viewModelScope.launch {
            // If the parent of the current folder is already in the rubbish bin, we need to navigate back again
            currentFolder.parent?.takeIf {
                it.id.longValue != -1L
            }?.let { parent ->
                _state.update { state ->
                    state.copy(
                        openedFolderNodeHandles = state.openedFolderNodeHandles.toMutableSet()
                            .apply {
                                remove(currentFolder.id.longValue)
                            }
                    )
                }
                val isNodeInRubbish = isNodeInRubbishBinUseCase(parent.node.id)
                if (isNodeInRubbish) {
                    _state.update {
                        it.copy(
                            parentNode = parent,
                            isLoading = true
                        )
                    }
                    performBackNavigation()
                    return@launch
                }
            }
            observeFlow(currentFolder.parent?.let { childLinks(it) } ?: publicLinks())
        }
    }

    private fun childLinks(parentNode: PublicLinkFolder) =
        parentNode.children.map { list ->
            _state.value.copy(
                parentNode = parentNode,
                nodesList = getNodeUiItems(list),
                sortOrder = getSortOrder(),
                isLoading = false
            )
        }

    /**
     * This will open the folder by node handle if it is a folder
     */
    fun openFolderByHandle(handle: Long): Boolean {
        return state.value.nodesList
            .firstOrNull { it.node.id.longValue == handle && it.node is PublicLinkFolder }
            ?.node
            ?.let {
                openFolder(it as PublicLinkFolder)
                true
            } ?: false
    }

    /**
     * Open the folder by node handle with retry after 200ms, max 3 times
     * Note: retry was added as a failsafe in case the method is called from ManagerActivity before nodeList is updated
     */
    fun openFolderByHandleWithRetry(handle: Long) {
        viewModelScope.launch {
            var retryCount = 0
            var success = openFolderByHandle(handle)
            while (!success && retryCount < 3) {
                delay(200L)
                success = openFolderByHandle(handle)
                retryCount++
            }
        }
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
     * This will reset the stack and will fetch the root links nodes
     */
    fun resetToRoot() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    parentNode = null,
                    isLoading = true
                )
            }
        }
        observeFlow(publicLinks())
    }

    /**
     * This will map list of [PublicLinkNode] to [NodeUIItem]
     */
    private suspend fun getNodeUiItems(nodeList: List<PublicLinkNode>): List<NodeUIItem<PublicLinkNode>> {
        val rubbishBinNode = getRubbishBinFolderUseCase()
        return nodeList.mapIndexed { _, node ->
            val accessPermission =
                getNodeAccessUseCase(nodeId = node.id) ?: AccessPermission.UNKNOWN
            val canBeMovedToRubbishBin =
                rubbishBinNode != null && checkNodeCanBeMovedToTargetNode(
                    nodeId = NodeId(longValue = node.id.longValue),
                    targetNodeId = NodeId(longValue = rubbishBinNode.id.longValue)
                )
            val isSelected =
                state.value.selectedNodes.firstOrNull { it.id == node.id.longValue } != null
            NodeUIItem(
                node = node,
                isSelected = isSelected,
                accessPermission = accessPermission,
                canBeMovedToRubbishBin = canBeMovedToRubbishBin
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
            }
        }.onFailure {
            Timber.e(it)
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
        _state.value.parentNode?.let { parentNode ->
            closeFolder(parentNode)
            _state.update { it.copy(updateToolbarTitleEvent = triggered) }
        } ?: run {
            _state.update { it.copy(exitLinksPageEvent = triggered) }
        }
    }

    /**
     * This will refresh link nodes and update [LinksUiState.nodesList]
     */
    fun refreshLinkNodes(showLoading: Boolean = true) {
        if (showLoading)
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        isLoading = true
                    )
                }
            }
        if (state.value.isInRootLevel) {
            observeFlow(publicLinks())
        } else {
            _state.value.parentNode?.let {
                observeFlow(childLinks(it))
            }
        }
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
                selectedNodeHandle = state.value.selectedNodes.map { it.id }
            )
            if (optionsItemInfo.optionClickedType == OptionItems.DOWNLOAD_CLICKED) {
                _state.update {
                    it.copy(
                        downloadEvent = triggered(
                            TransferTriggerEvent.StartDownloadNode(
                                nodes = optionsItemInfo.selectedNode,
                                withStartMessage = false,
                            )
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
        val selectedNodes = mutableListOf<SelectedNode>()
        selectedNodeList.forEach {
            if (it.node is FileNode) totalFolderNode++
            if (it.node is FolderNode) totalFileNode++
            selectedNodes.add(
                SelectedNode(
                    id = it.node.id.longValue,
                    type = SelectedNodeType.toSelectedNodeType(it.node),
                    isTakenDown = it.node.isTakenDown,
                    isExported = it.node.exportedData != null,
                    isIncomingShare = it.node.isIncomingShare,
                    accessPermission = it.accessPermission,
                    canBeMovedToRubbishBin = it.canBeMovedToRubbishBin
                )
            )
        }
        _state.update {
            it.copy(
                nodesList = selectedNodeList,
                isInSelection = true,
                selectedFolderNodes = totalFolderNode,
                selectedFileNodes = totalFileNode,
                selectedNodes = selectedNodes
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
    fun clearAllNodesSelection() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    nodesList = selectionClearedNodeUiItemList(),
                    selectedFileNodes = 0,
                    selectedFolderNodes = 0,
                    isInSelection = false,
                    selectedNodes = emptyList(),
                    optionsItemInfo = null
                )
            }
        }
    }

    /**
     * Returns current node handle from UI state
     */
    fun getCurrentNodeHandle(): Long = _state.value.currentFolderNodeHandle

    /**
     * Clear the selections of items from NodesUiList
     */
    private fun selectionClearedNodeUiItemList(): List<NodeUIItem<PublicLinkNode>> {
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
        val selectedNodes = state.value.selectedNodes.toMutableList()
        val pair = if (nodeUIItem.isSelected) {
            selectedNodes.add(
                SelectedNode(
                    id = nodeUIItem.node.id.longValue,
                    type = SelectedNodeType.toSelectedNodeType(from = nodeUIItem.node),
                    isTakenDown = nodeUIItem.node.isTakenDown,
                    isExported = nodeUIItem.node.exportedData != null,
                    isIncomingShare = nodeUIItem.node.isIncomingShare,
                    accessPermission = nodeUIItem.accessPermission,
                    canBeMovedToRubbishBin = nodeUIItem.canBeMovedToRubbishBin
                )
            )
            selectNode(nodeUIItem)
        } else {
            selectedNodes.removeAll { it.id == nodeUIItem.node.id.longValue }
            unSelectNode(nodeUIItem)
        }
        val newNodesList = _state.value.nodesList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedFileNodes = pair.first,
                selectedFolderNodes = pair.second,
                nodesList = newNodesList,
                isInSelection = pair.first > 0 || pair.second > 0,
                selectedNodes = selectedNodes,
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

    /**
     *  Download file triggered
     */
    fun onDownloadFileTriggered(triggerEvent: TransferTriggerEvent) {
        _state.update {
            it.copy(
                downloadEvent = triggered(triggerEvent)
            )
        }
    }
}
