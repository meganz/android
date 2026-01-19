package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CleanRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinNodeChildrenUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.rubbishbin.model.NewRubbishBinUiState
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava
import timber.log.Timber

/**
 * M3 ViewModel for RubbishBin using new NodeUiItem model
 *
 * @param monitorNodeUpdatesUseCase Monitor node updates
 * @param getParentNodeUseCase [mega.privacy.android.domain.usecase.GetParentNodeUseCase] Fetch parent node
 * @param getNodesByIdInChunkUseCase [mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase] Fetch list of nodes in chunks for progressive loading
 * @param isNodeDeletedFromBackupsUseCase Checks whether the deleted Node came from Backups or not
 * @param setViewType [mega.privacy.android.domain.usecase.viewtype.SetViewType] to set view type
 * @param monitorViewType [mega.privacy.android.domain.usecase.viewtype.MonitorViewType] check view type
 * @param getRubbishBinFolderUseCase [mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase]
 * @param nodeUiItemMapper [mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper] to convert TypedNode to NodeUiItem
 * @param nodeSortConfigurationUiMapper [mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper] for sort configuration mapping
 */
@HiltViewModel(assistedFactory = NewRubbishBinViewModel.Factory::class)
class NewRubbishBinViewModel @AssistedInject constructor(
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getRubbishBinNodeChildrenUseCase: GetRubbishBinNodeChildrenUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val setCloudSortOrder: SetCloudSortOrder,
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val cleanRubbishBinUseCase: CleanRubbishBinUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @Assisted private val navKey: RubbishBinNavKey,
) : ViewModel() {
    private val args = navKey
    private val highlightedNodeId = navKey.highlightedNodeHandle?.let { NodeId(it) }

    /**
     * The RubbishBin UI State
     */
    private val _uiState = MutableStateFlow(NewRubbishBinUiState())

    /**
     * The RubbishBin UI State accessible outside the ViewModel
     */
    val uiState: StateFlow<NewRubbishBinUiState> = _uiState

    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    private var nodeMultiSelectionJob: Job? = null
    private var loadNodeChunksJob: Job? = null

    init {
        setupNodesLoading()
        nodeUpdates()
        monitorViewTypeChanges()
        getCloudSortOrderAndRefresh()
        monitorAccountDetail()
        checkSearchRevampEnabled()
    }

    private fun setupNodesLoading() {
        loadNodeChunksJob = viewModelScope.launch {
            val handle = args.handle
            val folderOrRootNodeId = if (handle == null) {
                getRubbishBinFolderUseCase()?.let { NodeId(it.id.longValue) } ?: NodeId(-1L)
            } else {
                NodeId(handle)
            }
            val title = if (handle == null) {
                LocalizedText.StringRes(sharedR.string.general_section_rubbish_bin)
            } else {
                val currentNode = getNodeByIdUseCase(folderOrRootNodeId)
                LocalizedText.Literal(currentNode?.name.orEmpty())
            }
            _uiState.update { it.copy(title = title, currentFolderId = folderOrRootNodeId) }
            getNodesByIdInChunkUseCase(folderOrRootNodeId)
                .catch { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(
                            nodesLoadingState = NodesLoadingState.Failed
                        )
                    }
                }
                .collect { (nodes, hasMore) ->
                    val nodeUiItems = nodeUiItemMapper(
                        nodeList = nodes,
                        nodeSourceType = NodeSourceType.RUBBISH_BIN,
                        existingItems = uiState.value.items,
                        highlightedNodeId = highlightedNodeId,
                    )
                    _uiState.update { state ->
                        state.copy(
                            items = nodeUiItems,
                            nodesLoadingState = if (hasMore) {
                                NodesLoadingState.PartiallyLoaded
                            } else {
                                NodesLoadingState.FullyLoaded
                            },
                            currentFolderId = folderOrRootNodeId,
                        )
                    }
                }
        }
    }

    /**
     * This method will monitor view type and update it on state
     */
    private fun monitorViewTypeChanges() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _uiState.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    private fun getCloudSortOrderAndRefresh() {
        viewModelScope.launch {
            runCatching {
                getCloudSortOrder()
            }.onSuccess { sortOrder ->
                val sortConfiguration = nodeSortConfigurationUiMapper(sortOrder)
                _uiState.update { it.copy(sortConfiguration = sortConfiguration) }
            }.onFailure {
                Timber.e(it, "Failed to get cloud sort order")
            }
        }
    }

    /**
     * Set sort configuration and refresh nodes
     */
    fun setSortConfiguration(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            val sortOrder = nodeSortConfigurationUiMapper(sortConfiguration)
            runCatching {
                setCloudSortOrder(sortOrder)
            }.onFailure {
                Timber.e(it, "Failed to set sort order")
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        sortConfiguration = sortConfiguration,
                        sortOrder = sortOrder
                    )
                }
                refreshNodes()
            }
        }
    }

    /**
     * Uses MonitorNodeUpdates to observe any Node updates
     * A received Node update will refresh the list of nodes
     */
    private fun nodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().collect {
                checkForDeletedNodes(it.changes)
            }
        }
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                val accountType = accountDetail.levelDetail?.accountType
                val businessStatus =
                    if (accountType?.isBusinessAccount == true) {
                        getBusinessStatusUseCase()
                    } else null

                _uiState.update {
                    it.copy(
                        accountType = accountType,
                        isHiddenNodesEnabled = true,
                        isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired
                    )
                }
            }.catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Set the current rubbish bin handle to the UI state
     */
    fun setRubbishBinHandle(handle: Long) = viewModelScope.launch {
        val nodeHandle =
            if (handle == _uiState.value.currentFolderId.longValue) MegaApiJava.INVALID_HANDLE else handle
        _uiState.update {
            it.copy(
                currentFolderId = NodeId(nodeHandle),
                items = emptyList(),
                nodesLoadingState = NodesLoadingState.Loading,
            )
        }
        setupNodesLoading()
    }

    /**
     * Retrieves the list of Nodes and converts them to NodeUiItems
     */
    fun refreshNodes() {
        loadNodeChunksJob?.cancel()
        viewModelScope.launch {
            runCatching {
                val currentNode = getNodeByIdUseCase(_uiState.value.currentFolderId)
                val nodeList =
                    getRubbishBinNodeChildrenUseCase(_uiState.value.currentFolderId.longValue)
                val parentNode = getParentNodeUseCase(_uiState.value.currentFolderId)

                // Convert to NodeUiItem using the mapper
                val nodeUiItems = nodeUiItemMapper(
                    nodeList = nodeList,
                    existingItems = _uiState.value.items,
                    nodeSourceType = NodeSourceType.RUBBISH_BIN,
                    highlightedNodeId = highlightedNodeId,
                )

                // Update title
                val title = currentNode?.name?.let { LocalizedText.Literal(it) }
                    ?: LocalizedText.Literal("")

                _uiState.update {
                    it.copy(
                        parentFolderId = parentNode?.id,
                        title = title,
                        items = nodeUiItems,
                        nodesLoadingState = NodesLoadingState.FullyLoaded,
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Handle item click - navigate to folder if it's a folder, open file if it's a file
     */
    fun onItemClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        if (_uiState.value.isInSelectionMode) {
            toggleItemSelection(nodeUiItem)
            return
        }

        when (val node = nodeUiItem.node) {
            is TypedFileNode -> {
                _uiState.update { it.copy(openedFileNode = node) }
            }

            is TypedFolderNode -> {
                onFolderItemClicked(node.id)
            }

            else -> Timber.e("Unsupported click")
        }
    }

    /**
     * Handle file node opened
     */
    fun onOpenedFileNodeHandled() {
        _uiState.update { it.copy(openedFileNode = null) }
    }

    /**
     * Handle item long click - toggle selection state
     */
    fun onItemLongClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        toggleItemSelection(nodeUiItem)
    }

    private fun toggleItemSelection(nodeUiItem: NodeUiItem<TypedNode>) {
        val updatedItems = _uiState.value.items.map { item ->
            if (item.node.id == nodeUiItem.node.id) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        _uiState.update { it.copy(items = updatedItems) }
    }

    /**
     * Performs action when folder is clicked from adapter
     */
    fun onFolderItemClicked(id: NodeId) {
        _uiState.update {
            it.copy(
                openFolderEvent = triggered(id)
            )
        }
    }

    /**
     * This will update handle for rubbishBin if any node is deleted from browser and
     * we are in same screen else will simply refresh nodes with parentID
     * if restored and we are inside folder, it will simply refresh rubbish node
     */
    private fun checkForDeletedNodes(changes: Map<Node, List<NodeChanges>>) {
        changes.forEach { (key, value) ->
            if (value.contains(NodeChanges.Remove) && _uiState.value.currentFolderId.longValue == key.id.longValue) {
                setRubbishBinHandle(key.parentId.longValue)
                return@forEach
            } else if (value.contains(NodeChanges.Parent) && _uiState.value.currentFolderId.longValue == key.id.longValue) {
                setRubbishBinHandle(-1)
                return@forEach
            }
        }
        refreshNodes()
    }

    /**
     * Select all NodeUiItems
     */
    fun selectAllNodes() {
        nodeMultiSelectionJob?.cancel()
        nodeMultiSelectionJob = viewModelScope.launch {
            runCatching {
                // Select all items that are already loaded
                performAllItemSelection()
                // If nodes are still loading, wait until fully loaded then select all
                if (uiState.value.nodesLoadingState.isInProgress) {
                    _uiState.update { state ->
                        state.copy(isSelecting = true)
                    }
                    uiState.first { it.nodesLoadingState.isComplete }
                    if (isActive) {
                        performAllItemSelection()
                    }
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isSelecting = false)
                }
                Timber.e(it, "Failed to select all nodes")
            }
        }
    }

    private fun performAllItemSelection() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = true) }
        _uiState.update { state ->
            state.copy(
                items = updatedItems,
                isSelecting = false
            )
        }
    }

    /**
     * This method will toggle view type
     */
    fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_uiState.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    /**
     * Clear the selections of items from NodeUiItems and reset selection state
     */
    fun clearAllSelectedNodes() {
        nodeMultiSelectionJob?.cancel()
        val updatedItems = _uiState.value.items.map { it.copy(isSelected = false) }
        _uiState.update { state ->
            state.copy(
                items = updatedItems,
                isSelecting = false
            )
        }
    }

    /**
     * Clear/Empty the rubbish bin
     */
    fun clearRubbishBin() = viewModelScope.launch {
        runCatching {
            cleanRubbishBinUseCase()
        }.onFailure { throwable ->
            Timber.e(throwable)
            setMessage(LocalizedText.StringRes(R.string.rubbish_bin_no_emptied))
        }.onSuccess {
            setMessage(LocalizedText.StringRes(R.string.rubbish_bin_emptied))
            refreshNodes()
        }
    }

    fun setMessage(message: LocalizedText) {
        _uiState.update {
            it.copy(messageEvent = triggered(message))
        }
    }

    fun onMessageShown() {
        _uiState.update {
            it.copy(messageEvent = consumed())
        }
    }

    fun onOpenFolderEventConsumed() {
        _uiState.update {
            it.copy(openFolderEvent = consumed())
        }
    }

    private fun checkSearchRevampEnabled() {
        viewModelScope.launch {
            runCatching { getFeatureFlagValueUseCase(AppFeatures.SearchRevamp) }
                .onSuccess { isEnabled ->
                    _uiState.update { it.copy(isSearchRevampEnabled = isEnabled) }
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: RubbishBinNavKey): NewRubbishBinViewModel
    }
}