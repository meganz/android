package mega.privacy.android.app.presentation.clouddrive

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.quota.GetBandwidthOverQuotaDelayUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to FileBrowserFragment
 *
 * @param getRootNodeUseCase Fetch the root node
 * @param monitorMediaDiscoveryView Monitor media discovery view settings
 * @param monitorNodeUpdatesUseCase Monitor node updates
 * @param getParentNodeUseCase To get parent node of current node
 * @param getIsNodeInRubbish To get current node is in rubbish
 * @param getFileBrowserNodeChildrenUseCase [GetFileBrowserNodeChildrenUseCase]
 * @param getCloudSortOrder [GetCloudSortOrder]
 * @param monitorViewType [MonitorViewType] check view type
 * @param setViewType [SetViewType] to set view type
 * @param handleOptionClickMapper [HandleOptionClickMapper] handle option click click mapper
 * @param monitorRefreshSessionUseCase [MonitorRefreshSessionUseCase]
 * @param getBandwidthOverQuotaDelayUseCase [GetBandwidthOverQuotaDelayUseCase]
 * @param transfersManagement [TransfersManagement]
 * @param containsMediaItemUseCase [ContainsMediaItemUseCase]
 * @param fileDurationMapper [FileDurationMapper]
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getIsNodeInRubbish: IsNodeInRubbish,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
    private val handleOptionClickMapper: HandleOptionClickMapper,
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase,
    private val getBandwidthOverQuotaDelayUseCase: GetBandwidthOverQuotaDelayUseCase,
    private val transfersManagement: TransfersManagement,
    private val containsMediaItemUseCase: ContainsMediaItemUseCase,
    private val fileDurationMapper: FileDurationMapper,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(FileBrowserState())

    /**
     * State flow
     */
    val state: StateFlow<FileBrowserState> = _state

    /**
     * Stack to maintain folder navigation clicks
     */
    private val handleStack = Stack<Long>()

    init {
        monitorMediaDiscovery()
        refreshNodes()
        monitorFileBrowserChildrenNodes()
        checkViewType()
        monitorRefreshSession()
        changeTransferOverQuotaBannerVisibility()
        monitorOfflineNodes()
        monitorConnectivity()
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect {
                _state.update { state -> state.copy(isConnected = it) }
            }
        }
    }

    /**
     * A shorthand way of retrieving the [FileBrowserState]
     *
     * @return the [FileBrowserState]
     */
    fun state() = _state.value

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
     * This will monitor FileBrowserNodeUpdates from [MonitorNodeUpdatesUseCase] and
     * will update [FileBrowserState.nodesList]
     */
    private fun monitorFileBrowserChildrenNodes() {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().catch {
                Timber.e(it)
            }.collect {
                checkForNodeIsInRubbish(it.changes)
            }
        }
    }

    private fun monitorOfflineNodes() {
        viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase().collect {
                setPendingRefreshNodes()
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
                        setFileBrowserHandle(parent)
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
     * Updates the current File Browser Handle [FileBrowserState.fileBrowserHandle]
     *
     * @param handle The new File Browser Handle to be set
     */
    fun setFileBrowserHandle(handle: Long) =
        viewModelScope.launch {
            handleStack.push(handle)
            _state.update {
                it.copy(
                    fileBrowserHandle = handle,
                )
            }
            refreshNodesState()
        }

    /**
     * Immediately opens a Node in order to display its contents
     *
     * @param folderHandle The Folder Handle
     */
    suspend fun openFileBrowserWithSpecificNode(folderHandle: Long) {
        handleStack.push(folderHandle)
        _state.update {
            it.copy(
                fileBrowserHandle = folderHandle,
                accessedFolderHandle = folderHandle,
                openedFolderNodeHandles = emptySet(),
            )
        }
        refreshNodesState()
        if (shouldEnterMediaDiscoveryMode(
                folderHandle = folderHandle,
                mediaDiscoveryViewSettings = state.value.mediaDiscoveryViewSettings,
            )
        ) {
            setMediaDiscoveryVisibility(
                isMediaDiscoveryOpen = true,
                isMediaDiscoveryOpenedByIconClick = false,
            )
        }
    }

    /**
     * Get the browser parent handle
     * If not previously set, set the browser parent handle to root handle
     *
     * @return the handle of the browser section
     */
    fun getSafeBrowserParentHandle(): Long = runBlocking {
        if (_state.value.fileBrowserHandle == -1L) {
            setFileBrowserHandle(
                getRootNodeUseCase()?.id?.longValue ?: MegaApiJava.INVALID_HANDLE
            )
        }
        return@runBlocking _state.value.fileBrowserHandle
    }

    /**
     * If a folder only contains images or videos, then go to MD mode directly
     *
     * @param folderHandle the folder handle
     * @param mediaDiscoveryViewSettings [mediaDiscoveryViewSettings]
     * @return true is should enter MD mode, otherwise is false
     */
    suspend fun shouldEnterMediaDiscoveryMode(
        folderHandle: Long,
        mediaDiscoveryViewSettings: Int,
    ): Boolean =
        getFileBrowserNodeChildrenUseCase(folderHandle).let { nodes ->
            if (nodes.isEmpty() || mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.DISABLED.ordinal) {
                false
            } else {
                nodes.firstOrNull { node ->
                    node is TypedFolderNode
                            || MimeTypeList.typeForName(node.name).isSvgMimeType
                            || (!MimeTypeList.typeForName(node.name).isImage
                            && !MimeTypeList.typeForName(node.name).isVideoMimeType)
                }?.let {
                    false
                } ?: true
            }
        }

    /**
     * Refreshes the File Browser Nodes
     */
    fun refreshNodes() {
        viewModelScope.launch {
            refreshNodesState()
        }
    }

    private suspend fun refreshNodesState() {
        val fileBrowserHandle = _state.value.fileBrowserHandle
        val rootNode = getRootNodeUseCase()?.id?.longValue
        val isRootNode = fileBrowserHandle == rootNode

        val childrenNodes = getFileBrowserNodeChildrenUseCase(fileBrowserHandle)
        val showMediaDiscoveryIcon = !isRootNode && containsMediaItemUseCase(childrenNodes)
        val nodeUIItems = getNodeUiItems(childrenNodes)
        val sortOrder = getCloudSortOrder()
        val isFileBrowserEmpty = isRootNode || (fileBrowserHandle == MegaApiJava.INVALID_HANDLE)

        _state.update {
            it.copy(
                showMediaDiscoveryIcon = showMediaDiscoveryIcon,
                nodesList = nodeUIItems,
                isLoading = false,
                sortOrder = sortOrder,
                isFileBrowserEmpty = isFileBrowserEmpty,
            )
        }
    }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<TypedNode>): List<NodeUIItem<TypedNode>> {
        val existingNodeList = state.value.nodesList
        return nodeList.mapIndexed { index, node ->
            val isSelected = state.value.selectedNodeHandles.contains(node.id.longValue)
            val fileDuration = if (node is FileNode) {
                fileDurationMapper(node.type)?.let { durationInSecondsTextMapper(it) }
            } else null
            NodeUIItem(
                node = node,
                isSelected = if (existingNodeList.size > index) isSelected else false,
                isInvisible = if (existingNodeList.size > index) existingNodeList[index].isInvisible else false,
                fileDuration = fileDuration
            )
        }
    }

    /**
     * Checks if Media Discovery is open or not
     *
     * @return true if Media Discovery is indicated to be open
     */
    fun isMediaDiscoveryOpen() = _state.value.isMediaDiscoveryOpen

    /**
     * Sets the Media Discovery Visibility
     *
     * @param isMediaDiscoveryOpen If true, this indicates that Media Discovery is open
     * @param isMediaDiscoveryOpenedByIconClick true if Media Discovery was accessed by clicking the
     * Media Discovery Icon
     */
    fun setMediaDiscoveryVisibility(
        isMediaDiscoveryOpen: Boolean,
        isMediaDiscoveryOpenedByIconClick: Boolean,
    ) {
        _state.update {
            it.copy(
                isMediaDiscoveryOpen = isMediaDiscoveryOpen,
                isMediaDiscoveryOpenedByIconClick = isMediaDiscoveryOpenedByIconClick,
            )
        }
    }

    /**
     * Navigate back to the Cloud Drive Root Level hierarchy
     */
    suspend fun goBackToRootLevel() {
        _state.update {
            it.copy(
                accessedFolderHandle = null,
                isMediaDiscoveryOpen = false,
                isMediaDiscoveryOpenedByIconClick = false,
            )
        }
        getRootNodeUseCase()?.id?.longValue?.let { rootHandle ->
            Timber.d("Root Node Exists. Set data from the Root Node")
            setFileBrowserHandle(rootHandle)
        }
    }

    /**
     * Exits the Media Discovery
     *
     * @param performBackNavigation If true, goes back one level from the Cloud Drive hierarchy
     */
    suspend fun exitMediaDiscovery(performBackNavigation: Boolean) {
        if (performBackNavigation) {
            handleStack.pop()
            handleAccessedFolderOnBackPress()
            removeCurrentNodeFromUiStateSet()

            val parentHandle =
                getParentNodeUseCase(NodeId(_state.value.fileBrowserHandle))?.id?.longValue
                    ?: getRootNodeUseCase()?.id?.longValue ?: MegaApiJava.INVALID_HANDLE
            _state.update { it.copy(fileBrowserHandle = parentHandle) }
            setMediaDiscoveryVisibility(
                isMediaDiscoveryOpen = false,
                isMediaDiscoveryOpenedByIconClick = false,
            )
            refreshNodesState()
        } else {
            setMediaDiscoveryVisibility(
                isMediaDiscoveryOpen = false,
                isMediaDiscoveryOpenedByIconClick = false,
            )
        }
    }

    /**
     * Removes the current Node from the Set of opened Folder Nodes in UiState
     */
    private fun removeCurrentNodeFromUiStateSet() {
        _state.update {
            it.copy(
                isLoading = true,
                openedFolderNodeHandles = it.openedFolderNodeHandles.toMutableSet()
                    .apply { remove(_state.value.fileBrowserHandle) },
            )
        }
    }

    /**
     * Goes back one level from the Cloud Drive hierarchy
     */
    suspend fun performBackNavigation() {
        handleAccessedFolderOnBackPress()
        getParentNodeUseCase(NodeId(_state.value.fileBrowserHandle))?.id?.longValue?.let { parentHandle ->
            removeCurrentNodeFromUiStateSet()
            setFileBrowserHandle(parentHandle)
            handleStack.takeIf { stack -> stack.isNotEmpty() }?.pop()
            // Update the Toolbar Title
            _state.update { it.copy(updateToolbarTitleEvent = triggered) }
        } ?: run {
            // Exit File Browser if there is nothing left in the Back Stack
            _state.update {
                it.copy(
                    openedFolderNodeHandles = emptySet(),
                    exitFileBrowserEvent = triggered
                )
            }
        }
    }

    /**
     * Checks and updates State Parameters if the User performs a Back Navigation event, and is in
     * the Folder Level that he/she immediately accessed
     */
    private fun handleAccessedFolderOnBackPress() {
        if (_state.value.fileBrowserHandle == _state.value.accessedFolderHandle) {
            _state.update {
                it.copy(
                    isAccessedFolderExited = true,
                    accessedFolderHandle = null,
                )
            }
        }
    }

    /**
     * Performs specific actions upon clicking a Folder Node
     *
     * @param folderHandle The Folder Handle
     */
    private fun onFolderItemClicked(folderHandle: Long) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    openedFolderNodeHandles = it.openedFolderNodeHandles.toMutableSet()
                        .apply { add(_state.value.fileBrowserHandle) },
                )
            }
            setFileBrowserHandle(folderHandle)
            if (shouldEnterMediaDiscoveryMode(
                    folderHandle = folderHandle,
                    mediaDiscoveryViewSettings = state.value.mediaDiscoveryViewSettings,
                )
            ) {
                setMediaDiscoveryVisibility(
                    isMediaDiscoveryOpen = true,
                    isMediaDiscoveryOpenedByIconClick = false,
                )
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
            val overQuotaBannerTimeDelay = getBandwidthOverQuotaDelayUseCase()
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
                onFolderItemClicked(nodeUIItem.id.longValue)
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
     * Consume download event
     */
    fun consumeDownloadEvent() {
        _state.update {
            it.copy(downloadEvent = consumed())
        }
    }

    /**
     * Consumes the Exit File Browser Event
     */
    fun consumeExitFileBrowserEvent() {
        _state.update { it.copy(exitFileBrowserEvent = consumed) }
    }

    /**
     * Consumes the Update Toolbar Title Event
     */
    fun consumeUpdateToolbarTitleEvent() {
        _state.update { it.copy(updateToolbarTitleEvent = consumed) }
    }

    /**
     * Checks if the User has left the Folder that was immediately accessed
     *
     * @return true if the User left the accessed Folder
     */
    fun isAccessedFolderExited() = _state.value.isAccessedFolderExited

    /**
     * Resets the value of [FileBrowserState.isAccessedFolderExited]
     */
    fun resetIsAccessedFolderExited() =
        _state.update { it.copy(isAccessedFolderExited = false) }
}
