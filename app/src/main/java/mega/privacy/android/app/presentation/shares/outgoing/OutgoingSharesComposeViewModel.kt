package mega.privacy.android.app.presentation.shares.outgoing

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
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetOutgoingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to OutgoingSharesComposeFragment
 *
 * @param getRootNodeUseCase Fetch the root node
 * @param monitorNodeUpdatesUseCase Monitor node updates
 * @param monitorContactUpdatesUseCase Monitor contact updates
 * @param getParentNodeUseCase [GetParentNodeUseCase] To get parent node of current node
 * @param isNodeInRubbishBinUseCase [IsNodeInRubbishBinUseCase] To get current node is in rubbish
 * @param getOutgoingSharesChildrenNodeUseCase [GetOutgoingSharesChildrenNodeUseCase] To get children of current node
 * @param getCloudSortOrder [GetCloudSortOrder] To get cloud sort order
 * @param monitorViewType [MonitorViewType] Check view type
 * @param setViewType [SetViewType] To set view type
 * @param handleOptionClickMapper [HandleOptionClickMapper] Handle option click click mapper
 * @param monitorRefreshSessionUseCase [MonitorRefreshSessionUseCase] Monitor refresh session
 * @param fileDurationMapper [FileDurationMapper] To map file duration
 * @param monitorOfflineNodeUpdatesUseCase [MonitorOfflineNodeUpdatesUseCase] Monitor offline node updates
 * @param monitorConnectivityUseCase [MonitorConnectivityUseCase] Monitor connectivity
 * @param durationInSecondsTextMapper [DurationInSecondsTextMapper] To map duration in seconds to text
 */
@HiltViewModel
class OutgoingSharesComposeViewModel @Inject constructor(
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorContactUpdatesUseCase: MonitorContactUpdates,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val getOutgoingSharesChildrenNodeUseCase: GetOutgoingSharesChildrenNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
    private val handleOptionClickMapper: HandleOptionClickMapper,
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase,
    private val fileDurationMapper: FileDurationMapper,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OutgoingSharesState())

    /**
     * Immutable State flow
     */
    val state = _state.asStateFlow()

    /**
     * Stack to maintain folder navigation clicks
     */
    private val handleStack = Stack<Long>()

    init {
        checkContactVerificationWarning()
        refreshNodes()
        monitorChildrenNodes()
        monitorContactUpdates()
        checkViewType()
        monitorRefreshSession()
        monitorOfflineNodes()
        monitorConnectivity()
    }

    private fun checkContactVerificationWarning() {
        viewModelScope.launch {
            runCatching {
                getContactVerificationWarningUseCase()
            }.onSuccess { enabled ->
                _state.update {
                    it.copy(isContactVerificationOn = enabled)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorContactUpdates() {
        val changesToObserve = setOf(
            UserChanges.AuthenticationInformation,
            UserChanges.Firstname,
            UserChanges.Lastname,
            UserChanges.Alias
        )
        viewModelScope.launch {
            monitorContactUpdatesUseCase().collectLatest { updates ->
                Timber.d("Received contact update")
                if (updates.changes.values.any { it.any { change -> changesToObserve.contains(change) } }) {
                    checkContactVerificationWarning()
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
     * will update [OutgoingSharesState.nodesList]
     */
    private fun monitorChildrenNodes() {
        viewModelScope.launch {
            runCatching {
                monitorNodeUpdatesUseCase().catch {
                    Timber.e(it)
                }.collect {
                    checkContactVerificationWarning()
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
                    while (handleStack.isNotEmpty() && isNodeInRubbishBinUseCase(NodeId(handleStack.peek()))) {
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

    private fun setPendingRefreshNodes() {
        _state.update { it.copy(isPendingRefresh = true) }
    }

    /**
     * Updates the current Handle [OutgoingSharesState.currentHandle]
     *
     * @param handle The new node handle to be set
     */
    fun setCurrentHandle(handle: Long) =
        viewModelScope.launch {
            handleStack.push(handle)
            _state.update {
                it.copy(
                    currentHandle = handle,
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

    /**
     * This method will handle the sort order change event
     */
    fun onSortOrderChanged() {
        setPendingRefreshNodes()
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

        val childrenNodes = getOutgoingSharesChildrenNodeUseCase(currentHandle)
        val nodeUIItems = getNodeUiItems(childrenNodes)
        val sortOrder = getCloudSortOrder()

        _state.update {
            it.copy(
                nodesList = nodeUIItems,
                isLoading = false,
                sortOrder = sortOrder,
                updateToolbarTitleEvent = triggered,
                currentNodeName = getNodeByIdUseCase(NodeId(currentHandle))?.name,
            )
        }
    }

    /**
     * Get current tree depth
     */
    fun outgoingTreeDepth() = if (_state.value.isInRootLevel) 0 else handleStack.size

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<ShareNode>): List<NodeUIItem<ShareNode>> {
        val existingNodeList = state.value.nodesList
        return nodeList.mapIndexed { index, node ->
            val isSelected =
                state.value.selectedNodes.find { it.id.longValue == node.id.longValue } != null
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
     * Navigate back to the Outgoing Shares Root Level hierarchy
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
     * Goes back one level from the Outgoing Shares hierarchy
     */
    fun performBackNavigation() {
        viewModelScope.launch {
            runCatching {
                handleAccessedFolderOnBackPress()
                if (handleStack.firstOrNull() == state.value.currentHandle) {
                    handleStack.pop()
                    goBackToRootLevel()
                } else {
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
                        // Exit OutgoingShares if there is nothing left in the Back Stack
                        _state.update {
                            it.copy(
                                openedFolderNodeHandles = emptySet(),
                                exitOutgoingSharesEvent = triggered
                            )
                        }
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
    fun onFolderItemClicked(folderHandle: Long) {
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
     * Dismiss the Verify Contact Dialog by clearing the email
     */
    fun dismissVerifyContactDialog() {
        _state.update {
            it.copy(
                verifyContactDialog = null
            )
        }
    }

    private fun showVerifyContactDialog(email: String?) {
        _state.update {
            it.copy(
                verifyContactDialog = email
            )
        }
    }

    private fun checkShareContactStatus(nodeUIItem: NodeUIItem<ShareNode>): Boolean {
        val shareData = nodeUIItem.node.shareData
        val email = shareData?.user ?: return true
        if (shareData.count > 0) {
            return true
        } else if (shareData.isPending) {
            // Show a dialog if the contact is pending
            showVerifyContactDialog(nodeUIItem.node.shareData?.user)
            return false
        } else if (!shareData.isVerified) {
            // Open the Authenticity Credentials if the contact is not verified
            _state.update {
                it.copy(openAuthenticityCredentials = triggered(email))
            }
            return false
        }
        return true
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<ShareNode>) {
        if (!checkShareContactStatus(nodeUIItem)) return

        val index =
            _state.value.nodesList.indexOfFirst { it.node == nodeUIItem.node }
        if (_state.value.isInSelection) {
            updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
        }
    }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem<ShareNode>): Boolean {
        // Turn off selection if the node is unverified share
        if (!checkShareContactStatus(nodeUIItem)) return false
        val index =
            _state.value.nodesList.indexOfFirst { it.node == nodeUIItem.node }
        updateNodeInSelectionState(nodeUIItem = nodeUIItem, index = index)
        return true
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
                    it.copy(optionsItemInfo = optionsItemInfo)
                }
            }
        }
    }

    /**
     * Consume open authenticity credentials
     */
    fun consumeOpenAuthenticityCredentials() {
        _state.update { it.copy(openAuthenticityCredentials = consumed()) }
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
     * Consumes the Exit Outgoing Shares Event
     */
    fun consumeExitOutgoingSharesEvent() {
        _state.update { it.copy(exitOutgoingSharesEvent = consumed) }
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
