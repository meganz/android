package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeNameByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.navigation.destination.CloudDrive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CloudDriveViewModel @Inject constructor(
    private val getNodeNameByIdUseCase: GetNodeNameByIdUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val scannerHandler: ScannerHandler,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val containsMediaItemUseCase: ContainsMediaItemUseCase,
    private val getCloudSortOrderUseCase: GetCloudSortOrder,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<CloudDrive>()
    private val highlightedNodeId = args.highlightedNodeHandle?.let { NodeId(it) }
    private val highlightedNodeNames = args.highlightedNodeNames
    internal val nodeSourceType = args.nodeSourceType
    private val _uiState = MutableStateFlow(
        CloudDriveUiState(
            title = LocalizedText.Literal(args.nodeName ?: ""),
            currentFolderId = NodeId(args.nodeHandle),
            isCloudDriveRoot = args.nodeHandle == -1L,
        )
    )
    internal val uiState = _uiState.asStateFlow()
    private var nodeMultiSelectionJob: Job? = null

    init {
        monitorViewType()
        viewModelScope.launch { updateTitle() }
        setupNodesLoading()
        monitorNodeUpdates()
        getCloudSortOrder()
    }

    /**
     * Process CloudDriveAction and call relevant methods
     */
    fun processAction(action: CloudDriveAction) {
        when (action) {
            is CloudDriveAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is CloudDriveAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is CloudDriveAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is CloudDriveAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is CloudDriveAction.SelectAllItems -> selectAllItems()
            is CloudDriveAction.DeselectAllItems -> deselectAllItems()
            is CloudDriveAction.SetHiddenNodesOnboarded -> setHiddenNodesOnboarded()
            is CloudDriveAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is CloudDriveAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
            is CloudDriveAction.StartDocumentScanning -> prepareDocumentScanner()
        }
    }

    private fun setupNodesLoading() {
        viewModelScope.launch {
            val folderId = uiState.value.currentFolderId
            val folderOrRootNodeId = if (folderId.longValue == -1L) {
                getRootNodeIdUseCase() ?: folderId
            } else {
                folderId
            }
            getNodesByIdInChunkUseCase(folderOrRootNodeId)
                .collect { (nodes, hasMore) ->
                    val nodeUiItems = nodeUiItemMapper(
                        nodeList = nodes,
                        nodeSourceType = nodeSourceType,
                        highlightedNodeId = highlightedNodeId,
                        highlightedNames = highlightedNodeNames,
                        existingItems = uiState.value.items,
                    )
                    val loadingState = when {
                        hasMore -> NodesLoadingState.PartiallyLoaded
                        else -> NodesLoadingState.FullyLoaded
                    }
                    val hasMediaItems =
                        uiState.value.hasMediaItems || containsMediaItemUseCase(nodes)
                    _uiState.update { state ->
                        state.copy(
                            items = nodeUiItems,
                            nodesLoadingState = loadingState,
                            currentFolderId = folderOrRootNodeId,
                            hasMediaItems = hasMediaItems
                        )
                    }
                }
        }
        viewModelScope.launch {
            if (isHiddenNodeFeatureFlagEnabled()) {
                combine(
                    monitorHiddenNodesEnabledUseCase()
                        .catch { Timber.e(it) },
                    monitorShowHiddenItemsUseCase()
                        .catch { Timber.e(it) },
                    ::Pair
                ).collectLatest { pair ->
                    val isHiddenNodesEnabled = pair.first
                    val showHiddenItems = pair.second
                    _uiState.update { state ->
                        state.copy(
                            isHiddenNodeSettingsLoading = false,
                            isHiddenNodesEnabled = isHiddenNodesEnabled,
                            showHiddenNodes = showHiddenItems
                        )
                    }
                }
                checkIfHiddenNodeIsOnboarded()
            } else {
                // Hidden nodes disabled, set loading state to false
                _uiState.update { state ->
                    state.copy(isHiddenNodeSettingsLoading = false)
                }
            }
        }
    }

    private fun getCloudSortOrder(refresh: Boolean = false) {
        viewModelScope.launch {
            runCatching {
                getCloudSortOrderUseCase()
            }.onSuccess { sortOrder ->
                val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
                _uiState.update { it.copy(selectedSortConfiguration = sortOrderPair) }
                if (refresh) {
                    refreshNodes()
                }
            }.onFailure {
                Timber.e(it, "Failed to get cloud sort order")
            }
        }
    }

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }.onSuccess {
                getCloudSortOrder(refresh = true)
            }
        }
    }

    private suspend fun refreshNodes() {
        val folderId = uiState.value.currentFolderId
        runCatching {
            val nodes = getFileBrowserNodeChildrenUseCase(folderId.longValue)
            val hasMediaItems = containsMediaItemUseCase(nodes)
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = nodeSourceType,
                highlightedNodeId = highlightedNodeId,
                highlightedNames = highlightedNodeNames,
                existingItems = uiState.value.items,
            )
            _uiState.update { state ->
                state.copy(
                    items = nodeUiItems,
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                    hasMediaItems = hasMediaItems
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(NodeId(args.nodeHandle)).collectLatest { change ->
                if (change == NodeChanges.Remove) {
                    // If current folder is moved to rubbish bin, navigate back
                    _uiState.update {
                        it.copy(navigateBack = triggered)
                    }
                } else {
                    // If nodes are currently loading, ignore updates
                    if (uiState.value.nodesLoadingState == NodesLoadingState.FullyLoaded) {
                        refreshNodes()
                    }
                }
            }
        }
    }

    private suspend fun isHiddenNodeFeatureFlagEnabled(): Boolean = runCatching {
        getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
    }.getOrDefault(false)

    private suspend fun checkIfHiddenNodeIsOnboarded() = runCatching {
        isHiddenNodesOnboardedUseCase()
    }.onSuccess { isHiddenNodesOnboarded ->
        if (!isHiddenNodesOnboarded) {
            _uiState.update {
                it.copy(isHiddenNodesOnboarded = false)
            }
        }
    }.onFailure {
        Timber.e(it, "Failed to check if hidden nodes are onboarded")
    }

    /**
     * // TODO: handle from node options bottom sheet's "Hide" action
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _uiState.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    private suspend fun updateTitle() {
        runCatching {
            getNodeNameByIdUseCase(uiState.value.currentFolderId)
        }.onSuccess { nodeName ->
            val title = LocalizedText.Literal(nodeName ?: "")
            // Only update state if fetched title is different
            if (uiState.value.title != title) {
                _uiState.update { state ->
                    state.copy(title = title)
                }
            }
        }.onFailure {
            Timber.e(it, "Failed to get node name for title update")
        }
    }

    /**
     * Handle item click - navigate to folder if it's a folder
     */
    private fun onItemClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        if (uiState.value.isInSelectionMode) {
            toggleItemSelection(nodeUiItem)
            return
        }
        when (nodeUiItem.node) {
            is TypedFolderNode -> {
                _uiState.update { state ->
                    state.copy(
                        navigateToFolderEvent = triggered(nodeUiItem.node)
                    )
                }
            }

            is TypedFileNode -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = nodeUiItem.node as TypedFileNode
                    )
                }
            }
        }
    }

    /**
     * Consume navigation event
     */
    private fun onNavigateToFolderEventConsumed() {
        _uiState.update { state ->
            state.copy(navigateToFolderEvent = consumed())
        }
    }

    /**
     * Consume navigate back event
     */
    private fun onNavigateBackEventConsumed() {
        _uiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    /**
     * Handle item long click - toggle selection state
     */
    private fun onItemLongClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        toggleItemSelection(nodeUiItem)
    }

    private fun toggleItemSelection(nodeUiItem: NodeUiItem<TypedNode>) {
        val updatedItems = uiState.value.items.map { item ->
            if (item.node.id == nodeUiItem.node.id) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        _uiState.update { state ->
            state.copy(items = updatedItems)
        }

        // Cancel any ongoing multi-selection job if user manually deselects all items
        if (!uiState.value.isInSelectionMode) {
            nodeMultiSelectionJob?.cancel()
        }
    }

    /**
     * Deselect all items and reset selection state
     */
    private fun deselectAllItems() {
        nodeMultiSelectionJob?.cancel()
        val updatedItems = uiState.value.items.map { it.copy(isSelected = false) }
        _uiState.update { state ->
            state.copy(
                items = updatedItems,
                isSelecting = false
            )
        }
    }

    /**
     * Select all items
     */
    private fun selectAllItems() {
        nodeMultiSelectionJob?.cancel()
        nodeMultiSelectionJob = viewModelScope.launch {
            runCatching {
                // Select all items that are already loaded
                performAllItemSelection()
                // If nodes are still loading, wait until fully loaded then select all
                if (uiState.value.nodesLoadingState != NodesLoadingState.FullyLoaded) {
                    _uiState.update { state ->
                        state.copy(isSelecting = true)
                    }
                    uiState.first { it.nodesLoadingState == NodesLoadingState.FullyLoaded }
                    if (isActive) {
                        performAllItemSelection()
                    }
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isSelecting = false)
                }
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
     * This method will toggle node view type between list and grid.
     */
    private fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            runCatching {
                val toggledViewType = when (uiState.value.currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorViewTypeUseCase()
                .catch { Timber.e(it) }
                .collect { viewType ->
                    _uiState.update { it.copy(currentViewType = viewType) }
                }
        }
    }

    /**
     * Handle the event when a file node is opened
     */
    private fun onOpenedFileNodeHandled() {
        _uiState.update { state ->
            state.copy(openedFileNode = null)
        }
    }

    /**
     * Prepares the ML Kit Document Scanner from Google Play Services
     */
    fun prepareDocumentScanner() {
        viewModelScope.launch {
            runCatching {
                scannerHandler.prepareDocumentScanner()
            }.onSuccess { gmsDocumentScanner ->
                _uiState.update { it.copy(gmsDocumentScanner = gmsDocumentScanner) }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        documentScanningError = if (exception is InsufficientRAMToLaunchDocumentScanner) {
                            DocumentScanningError.InsufficientRAM
                        } else {
                            DocumentScanningError.GenericError
                        }
                    )
                }
            }
        }
    }

    /**
     * When the system fails to open the ML Kit Document Scanner, display a generic error message
     */
    fun onDocumentScannerFailedToOpen() {
        _uiState.update { it.copy(documentScanningError = DocumentScanningError.GenericError) }
    }

    /**
     * Resets the value of [CloudDriveUiState.gmsDocumentScanner]
     */
    fun onGmsDocumentScannerConsumed() {
        _uiState.update { it.copy(gmsDocumentScanner = null) }
    }

    /**
     * Resets the value of [CloudDriveUiState.documentScanningError]
     */
    fun onDocumentScanningErrorConsumed() {
        _uiState.update { it.copy(documentScanningError = null) }
    }
}
