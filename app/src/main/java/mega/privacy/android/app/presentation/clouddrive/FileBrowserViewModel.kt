package mega.privacy.android.app.presentation.clouddrive

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetBandWidthOverQuotaDelayUseCase
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetFileBrowserChildrenUseCase
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
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
 * @param handleOptionClickMapper [HandleOptionClickMapper] handle option click click mapper
 * @param monitorRefreshSessionUseCase [MonitorRefreshSessionUseCase]
 * @param getBandWidthOverQuotaDelayUseCase [GetBandWidthOverQuotaDelayUseCase]
 * @param transfersManagement [TransfersManagement]
 * @param containsMediaItemUseCase [ContainsMediaItemUseCase]
 * @param isAvailableOfflineUseCase [IsAvailableOfflineUseCase]
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
    private val handleOptionClickMapper: HandleOptionClickMapper,
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase,
    private val getBandWidthOverQuotaDelayUseCase: GetBandWidthOverQuotaDelayUseCase,
    private val transfersManagement: TransfersManagement,
    private val containsMediaItemUseCase: ContainsMediaItemUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
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
        monitorRefreshSession()
        changeTransferOverQuotaBannerVisibility()
    }

    private fun monitorRefreshSession() {
        viewModelScope.launch {
            monitorRefreshSessionUseCase().collect {
                setPendingRefreshNodes()
            }
        }
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
     * handle logic if back from Media Discovery
     */
    suspend fun handleBackFromMD() {
        handleStack.pop()
        val currentParent =
            _state.value.parentHandle ?: getRootFolder()?.handle ?: MegaApiJava.INVALID_HANDLE
        _state.update {
            it.copy(
                fileBrowserHandle = currentParent,
                mediaHandle = currentParent
            )
        }
        refreshNodesState()
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
            refreshNodesState()
        }
    }

    private suspend fun refreshNodesState() {
        val typedNodeList = getFileBrowserChildrenUseCase(_state.value.fileBrowserHandle)
        val nodeList = getNodeUiItems(typedNodeList)
        val nodes = getBrowserChildrenNode(_state.value.fileBrowserHandle) ?: emptyList()
        val hasMediaFile: Boolean = containsMediaItemUseCase(typedNodeList)
        val isRootNode = getRootFolder()?.handle == _state.value.fileBrowserHandle
        _state.update {
            it.copy(
                showMediaDiscoveryIcon = !isRootNode && hasMediaFile,
                nodes = nodes,
                parentHandle = getFileBrowserParentNodeHandle(_state.value.fileBrowserHandle),
                nodesList = nodeList,
                sortOrder = getCloudSortOrder(),
                isFileBrowserEmpty = MegaApiJava.INVALID_HANDLE == _state.value.fileBrowserHandle ||
                        getRootFolder()?.handle == _state.value.fileBrowserHandle
            )
        }
    }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private suspend fun getNodeUiItems(nodeList: List<TypedNode>): List<NodeUIItem<TypedNode>> {
        val existingNodeList = state.value.nodesList
        return nodeList.mapIndexed { index, node ->
            val isSelected = state.value.selectedNodeHandles.contains(node.id.longValue)
            NodeUIItem(
                node = node,
                isSelected = if (existingNodeList.size > index) isSelected else false,
                isInvisible = if (existingNodeList.size > index) existingNodeList[index].isInvisible else false,
                isAvailableOffline = isAvailableOfflineUseCase(node)
            )
        }
    }

    /**
     * Handles back click of rubbishBinFragment
     */
    fun onBackPressed() {
        _state.update {
            it.copy(showMediaDiscoveryIcon = false)
        }
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
    fun onFolderItemClicked(
        lastFirstVisiblePosition: Int,
        handle: Long,
    ) {
        viewModelScope.launch {
            setBrowserParentHandle(handle)
            if (shouldEnterMediaDiscoveryMode(
                    parentHandle = handle,
                    mediaDiscoveryViewSettings = state.value.mediaDiscoveryViewSettings
                )
            ) {
                _state.update { state ->
                    state.copy(
                        showMediaDiscovery = true,
                        showMediaDiscoveryIcon = true
                    )
                }
            } else {
                pushPositionOnStack(lastFirstVisiblePosition)
            }
        }
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
                nodesList = selectedNodeList,
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
    private fun selectAllNodesUiList(): List<NodeUIItem<TypedNode>> {
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
    private fun clearNodeUiItemList(): List<NodeUIItem<TypedNode>> {
        return _state.value.nodesList.map {
            it.copy(isSelected = false)
        }
    }

    /**
     *  Changes the Transfer Over Quota banner visibility based on certain conditions
     */
    fun changeTransferOverQuotaBannerVisibility() {
        viewModelScope.launch {
            val overQuotaBannerTimeDelay = getBandWidthOverQuotaDelayUseCase()
            _state.update {
                it.copy(
                    shouldShowBannerVisibility = transfersManagement.isTransferOverQuotaBannerShown,
                    bannerTime = overQuotaBannerTimeDelay
                )
            }
        }
    }

    /**
     * OnBannerDismiss clicked
     */
    fun onBannerDismissClicked() {
        changeTransferOverQuotaBannerVisibility()
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
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
    fun onLongItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.nodesList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeInSelectionState(nodeUIItem: NodeUIItem<TypedNode>, index: Int) {
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
                selectedNodeHandles = selectedNodeHandle,
                optionsItemInfo = null
            )
        }
    }

    /**
     * select a node
     * @param nodeUIItem
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun selectNode(nodeUIItem: NodeUIItem<TypedNode>): Pair<Int, Int> {
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
    private fun unSelectNode(nodeUIItem: NodeUIItem<TypedNode>): Pair<Int, Int> {
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
            _state.update {
                it.copy(
                    optionsItemInfo = optionsItemInfo
                )
            }
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
                showMediaDiscovery = false
            )
        }
    }
}