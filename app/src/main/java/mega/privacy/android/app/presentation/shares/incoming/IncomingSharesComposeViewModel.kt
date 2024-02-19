package mega.privacy.android.app.presentation.shares.incoming

import android.view.MenuItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to IncomingSharesComposeFragment
 *
 * @param getRootNodeUseCase Fetch the root node
 * @param monitorNodeUpdatesUseCase Monitor node updates
 * @param monitorContactUpdatesUseCase Monitor contact updates
 * @param getParentNodeUseCase [GetParentNodeUseCase] To get parent node of current node
 * @param getIsNodeInRubbish [IsNodeInRubbish] To get current node is in rubbish
 * @param getIncomingSharesChildrenNodeUseCase [GetIncomingSharesChildrenNodeUseCase] To get children of current node
 * @param getCloudSortOrder [GetCloudSortOrder] To get cloud sort order
 * @param getOthersSortOrder [GetOthersSortOrder] To get others sort order
 * @param monitorViewType [MonitorViewType] Check view type
 * @param setViewType [SetViewType] To set view type
 * @param handleOptionClickMapper [HandleOptionClickMapper] Handle option click click mapper
 * @param monitorRefreshSessionUseCase [MonitorRefreshSessionUseCase] Monitor refresh session
 * @param fileDurationMapper [FileDurationMapper] To map file duration
 * @param monitorOfflineNodeUpdatesUseCase [MonitorOfflineNodeUpdatesUseCase] Monitor offline node updates
 * @param monitorConnectivityUseCase [MonitorConnectivityUseCase] Monitor connectivity
 * @param getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase] Get feature flag value
 * @param durationInSecondsTextMapper [DurationInSecondsTextMapper] To map duration in seconds to text
 * @param getContactVerificationWarningUseCase [GetContactVerificationWarningUseCase] Get contact verification warning
 * @param areCredentialsVerifiedUseCase [AreCredentialsVerifiedUseCase] Check if credentials are verified
 * @param getIncomingShareParentUserEmailUseCase [GetIncomingShareParentUserEmailUseCase] Get incoming share parent user email
 */
@HiltViewModel
class IncomingSharesComposeViewModel @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorContactUpdatesUseCase: MonitorContactUpdates,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getIsNodeInRubbish: IsNodeInRubbish,
    private val getIncomingSharesChildrenNodeUseCase: GetIncomingSharesChildrenNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
    private val handleOptionClickMapper: HandleOptionClickMapper,
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase,
    private val fileDurationMapper: FileDurationMapper,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(IncomingSharesState())

    /**
     * Immutable State flow
     */
    val state = _state.asStateFlow()

    /**
     * Stack to maintain folder navigation clicks
     */
    private val handleStack = Stack<Long>()

    init {
        checkContactVerification()
        refreshNodes()
        monitorChildrenNodes()
        monitorContactUpdates()
        checkViewType()
        monitorRefreshSession()
        monitorOfflineNodes()
        monitorConnectivity()
    }

    private fun monitorContactUpdates() {
        val changesToObserve = setOf(
            UserChanges.AuthenticationInformation,
            UserChanges.Firstname,
            UserChanges.Lastname
        )
        viewModelScope.launch {
            monitorContactUpdatesUseCase().collectLatest { updates ->
                Timber.d("Received contact update")
                if (updates.changes.values.any { it.any { change -> changesToObserve.contains(change) } }) {
                    refreshNodesState()
                }
            }
        }
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect {
                _state.update { state -> state.copy(isConnected = it) }
            }
        }
    }

    private fun monitorRefreshSession() {
        viewModelScope.launch {
            monitorRefreshSessionUseCase().collect {
                setPendingRefreshNodes()
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
     * This will monitor node updates from [MonitorNodeUpdatesUseCase] and
     * will update [IncomingSharesState.nodesList]
     */
    private fun monitorChildrenNodes() {
        viewModelScope.launch {
            runCatching {
                monitorNodeUpdatesUseCase().catch {
                    Timber.e(it)
                }.collect {
                    checkForNodeIsInRubbish(it.changes)
                }
            }.onFailure {
                Timber.e(it)
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

    private fun checkContactVerification() {
        viewModelScope.launch {
            val isContactVerificationOn = getContactVerificationWarningUseCase()
            _state.update {
                it.copy(isContactVerificationOn = isContactVerificationOn)
            }
        }
    }

    private fun checkIfSelectedFolderIsSharedByVerifiedContact() =
        viewModelScope.launch {
            if (_state.value.isContactVerificationOn) {
                val showBanner = if (_state.value.isInRootLevel) {
                    false
                } else {
                    val email =
                        getIncomingShareParentUserEmailUseCase(NodeId(_state.value.currentHandle))
                    val verified = email?.let { areCredentialsVerifiedUseCase(it) } ?: run { false }
                    !verified
                }
                _state.update { it.copy(showContactNotVerifiedBanner = showBanner) }
            }
        }

    /**
     * This will update current handle if any node is deleted from browser and
     * moved to rubbish bin
     * we are in same screen else will simply refresh nodes with parentID
     * @param changes [Map] of [Node], list of [NodeChanges]
     */
    private suspend fun checkForNodeIsInRubbish(changes: Map<Node, List<NodeChanges>>) {
        changes.forEach { (node, _) ->
            if (node is FolderNode) {
                if (node.isInRubbishBin && _state.value.currentHandle == node.id.longValue) {
                    while (handleStack.isNotEmpty() && getIsNodeInRubbish(handleStack.peek())) {
                        handleStack.pop()
                    }
                    handleStack.takeIf { stack -> stack.isNotEmpty() }?.peek()?.let { parent ->
                        setCurrentHandle(parent)
                        return
                    }
                }
            }
        }
        setPendingRefreshNodes()
    }

    /**
     * Returns the count of nodes in the current folder
     */
    fun getNodeCount() = _state.value.nodesList.size

    private fun setPendingRefreshNodes() {
        _state.update { it.copy(isPendingRefresh = true) }
    }

    /**
     * Updates the current Handle [IncomingSharesState.currentHandle]
     *
     * @param handle The new node handle to be set
     */
    fun setCurrentHandle(handle: Long) =
        viewModelScope.launch {
            handleStack.push(handle)
            _state.update {
                it.copy(
                    currentHandle = handle,
                    currentNodeName = getNodeByIdUseCase(NodeId(handle))?.name,
                    updateToolbarTitleEvent = triggered
                )
            }
            refreshNodesState()
        }

    /**
     * Get the current node handle
     */
    fun getCurrentNodeHandle() = _state.value.currentHandle

    /**
     * Refreshes the nodes
     */
    fun refreshNodes() {
        viewModelScope.launch {
            runCatching {
                refreshNodesState()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private suspend fun refreshNodesState() {
        val currentHandle = _state.value.currentHandle
        val isRootNode = _state.value.isInRootLevel

        /**
         * When a folder is opened, and user clicks on Shares bottom drawer item, clear the openedFolderNodeHandles
         */
        if (isRootNode && state.value.openedFolderNodeHandles.isNotEmpty()) {
            handleStack.clear()
            _state.update {
                it.copy(
                    isLoading = true,
                    openedFolderNodeHandles = emptySet()
                )
            }
        }

        val childrenNodes = getIncomingSharesChildrenNodeUseCase(currentHandle)
        val sortOrder = if (isRootNode) getOthersSortOrder() else getCloudSortOrder()
        checkIfSelectedFolderIsSharedByVerifiedContact()
        val nodeUIItems = getNodeUiItems(childrenNodes)
        _state.update {
            it.copy(
                nodesList = nodeUIItems,
                isLoading = false,
                sortOrder = sortOrder,
                updateToolbarTitleEvent = triggered
            )
        }
    }

    /**
     * Get current tree depth
     */
    fun incomingTreeDepth() = if (_state.value.isInRootLevel) 0 else handleStack.size

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<ShareNode>): List<NodeUIItem<ShareNode>> {
        with(state.value) {
            return nodeList.mapIndexed { index, node ->
                val isSelected = selectedNodes.find { it.id.longValue == node.id.longValue } != null
                val fileDuration = if (node is FileNode) {
                    fileDurationMapper(node.type)?.let { durationInSecondsTextMapper(it) }
                } else null
                NodeUIItem(
                    node = node,
                    isSelected = if (nodesList.size > index) isSelected else false,
                    isInvisible = if (nodesList.size > index) nodesList[index].isInvisible else false,
                    fileDuration = fileDuration,
                )
            }
        }
    }

    /**
     * Navigate back to the Incoming Shares Root Level hierarchy
     */
    fun goBackToRootLevel() {
        _state.update {
            it.copy(
                accessedFolderHandle = null,
                currentHandle = -1L,
                updateToolbarTitleEvent = triggered
            )
        }
        refreshNodes()
    }

    /**
     * Removes the current Node from the Set of opened Folder Nodes in UiState
     */
    private fun removeCurrentNodeFromUiStateSet() {
        val updatedOpenedFolderNodeHandles = _state.value.openedFolderNodeHandles.toMutableSet()
            .apply { remove(_state.value.currentHandle) }
        _state.update {
            it.copy(
                isLoading = true,
                openedFolderNodeHandles = updatedOpenedFolderNodeHandles,
            )
        }
    }

    /**
     * Goes back one level from the Incoming Shares hierarchy
     */
    fun performBackNavigation() {
        viewModelScope.launch {
            runCatching {
                handleAccessedFolderOnBackPress()
                getParentNodeUseCase(NodeId(_state.value.currentHandle))?.id?.longValue?.let { parentHandle ->
                    removeCurrentNodeFromUiStateSet()
                    val rootNode = getRootNodeUseCase()?.id?.longValue
                    if (rootNode == parentHandle) {
                        goBackToRootLevel()
                    } else {
                        setCurrentHandle(parentHandle)
                        handleStack.takeIf { stack -> stack.isNotEmpty() }?.pop()
                        // Update the Toolbar Title
                        _state.update { it.copy(updateToolbarTitleEvent = triggered) }
                    }
                } ?: run {
                    // Exit Incoming Shares if there is nothing left in the Back Stack
                    _state.update {
                        it.copy(
                            openedFolderNodeHandles = emptySet(),
                            exitIncomingSharesEvent = triggered
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Checks and updates State Parameters if the User performs a Back Navigation event, and is in
     * the Folder Level that user immediately accessed
     */
    private fun handleAccessedFolderOnBackPress() {
        if (_state.value.currentHandle == _state.value.accessedFolderHandle) {
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
        val updatedOpenedFolderNodeHandles = _state.value.openedFolderNodeHandles.toMutableSet()
            .apply { add(_state.value.currentHandle) }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    openedFolderNodeHandles = updatedOpenedFolderNodeHandles
                )
            }
            setCurrentHandle(folderHandle)
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
        val updatedState = _state.value.nodesList.map {
            it.copy(isSelected = true)
        }
        val selectedNodes = updatedState.map { it.node }.toSet()
        val totalSelectedFiles = updatedState.filterIsInstance<FileNode>().size
        val totalSelectedFolders = selectedNodes.size - totalSelectedFiles

        _state.update {
            it.copy(
                isInSelection = true,
                nodesList = updatedState.toList(),
                selectedNodes = selectedNodes,
                totalSelectedFileNodes = totalSelectedFiles,
                totalSelectedFolderNodes = totalSelectedFolders,
            )
        }
    }

    /**
     * Clear All [NodeUIItem]
     */
    fun clearAllNodes() {
        viewModelScope.launch {
            val clearedNodes = clearNodeUiItemList()
            _state.update {
                it.copy(
                    nodesList = clearedNodes,
                    totalSelectedFileNodes = 0,
                    totalSelectedFolderNodes = 0,
                    isInSelection = false,
                    selectedNodes = emptySet(),
                    optionsItemInfo = null
                )
            }
        }
    }

    /**
     * Clear the selections of items from NodesUiList
     */
    private fun clearNodeUiItemList(): List<NodeUIItem<ShareNode>> {
        return _state.value.nodesList.map {
            it.copy(isSelected = false)
        }
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<ShareNode>) {
        val index =
            _state.value.nodesList.indexOfFirst { it.node == nodeUIItem.node }
        if (_state.value.isInSelection) {
            updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
        } else {
            if (nodeUIItem.node is FileNode) {
                _state.update {
                    it.copy(itemIndex = index, currentFileNode = nodeUIItem.node)
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
    fun onLongItemClicked(nodeUIItem: NodeUIItem<ShareNode>) {
        // Turn off selection if the node is unverified share
        val index =
            _state.value.nodesList.indexOfFirst { it.node == nodeUIItem.node }
        updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeInSelectionState(nodeUIItem: NodeUIItem<ShareNode>, index: Int) {
        nodeUIItem.isSelected = !nodeUIItem.isSelected
        val selectedNodes = state.value.selectedNodes.toMutableSet()
        if (state.value.selectedNodes.contains(nodeUIItem.node)) {
            selectedNodes.remove(nodeUIItem.node)
        } else {
            selectedNodes.add(nodeUIItem.node)
        }
        val newNodesList =
            _state.value.nodesList.updateItemAt(index = index, item = nodeUIItem)
        val totalSelectedFiles = selectedNodes.filterIsInstance<FileNode>().size
        val totalSelectedFolders = selectedNodes.size - totalSelectedFiles

        _state.update {
            it.copy(
                totalSelectedFileNodes = totalSelectedFiles,
                totalSelectedFolderNodes = totalSelectedFolders,
                nodesList = newNodesList,
                isInSelection = selectedNodes.isNotEmpty(),
                selectedNodes = selectedNodes,
                optionsItemInfo = null
            )
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
                    it.copy(optionsItemInfo = optionsItemInfo)
                }
            }
        }
    }

    /**
     * When item is clicked on activity
     */
    fun onItemPerformedClicked() {
        _state.update {
            it.copy(currentFileNode = null, itemIndex = -1)
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
     * Consumes the Exit Incoming Shares Event
     */
    fun consumeExitIncomingSharesEvent() {
        _state.update { it.copy(exitIncomingSharesEvent = consumed) }
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
     * Resets the value of [IncomingSharesState.isAccessedFolderExited]
     */
    fun resetIsAccessedFolderExited() =
        _state.update { it.copy(isAccessedFolderExited = false) }
}
