package mega.privacy.android.feature.clouddrive.presentation.clouddrive

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import timber.log.Timber

@HiltViewModel(assistedFactory = CloudDriveViewModel.Factory::class)
class CloudDriveViewModel @AssistedInject constructor(
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @Assisted private val navKey: CloudDriveNavKey,
) : ViewModel() {

    private val highlightedNodeId = navKey.highlightedNodeHandle?.let { NodeId(it) }
    private val highlightedNodeNames = navKey.highlightedNodeNames
    internal val nodeSourceType = navKey.nodeSourceType
    private val _uiState = MutableStateFlow(
        CloudDriveUiState(
            title = LocalizedText.Literal(navKey.nodeName ?: ""),
            currentFolderId = NodeId(navKey.nodeHandle),
            isCloudDriveRoot = navKey.nodeHandle == -1L,
            nodeSourceType = navKey.nodeSourceType,
        )
    )
    internal val uiState = _uiState.asStateFlow()
    private var nodeMultiSelectionJob: Job? = null

    init {
        monitorViewType()
        viewModelScope.launch { updateTitle() }
        setupNodesLoading()
        monitorNodeUpdates()
        monitorCloudSortOrder()
        monitorStorageOverQuota()
        monitorTransferOverQuota()
        checkWritePermission()
        checkSearchRevampEnabled()
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
            is CloudDriveAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is CloudDriveAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
            is CloudDriveAction.OverQuotaConsumptionWarning -> onConsumeOverQuotaWarning()
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
                .catch { Timber.e(it) }
                .collect { (nodes, hasMore) ->
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
        checkCurrentFolderContactVerification()
        viewModelScope.launch {
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
        }
    }

    private fun monitorCloudSortOrder() {
        monitorSortCloudOrderUseCase()
            .catch { Timber.e(it) }
            .filterNotNull()
            .onEach {
                updateSortOrder(it)
                refreshNodes()
            }
            .launchIn(viewModelScope)
    }

    private fun updateSortOrder(sortOrder: SortOrder) {
        val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
        _uiState.update {
            it.copy(
                selectedSortConfiguration = sortOrderPair,
                selectedSortOrder = sortOrder
            )
        }
    }

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }

    private suspend fun refreshNodes() {
        val folderId = uiState.value.currentFolderId
        runCatching {
            checkCurrentFolderContactVerification()
            val nodes = getFileBrowserNodeChildrenUseCase(folderId.longValue)
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
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(
                nodeId = NodeId(navKey.nodeHandle),
                nodeSourceType = nodeSourceType
            )
                .catch { Timber.e(it) }
                .collectLatest { change ->
                    if (change == NodeChanges.Remove) {
                        // If current folder is moved to rubbish bin, navigate back
                        _uiState.update {
                            it.copy(navigateBack = triggered)
                        }
                    } else {
                        updateTitle()
                        // If nodes are currently loading, ignore updates
                        if (uiState.value.nodesLoadingState == NodesLoadingState.FullyLoaded) {
                            refreshNodes()
                            checkWritePermission()
                        }
                    }
                }
        }
    }

    private suspend fun updateTitle() {
        runCatching {
            getNodeInfoByIdUseCase(uiState.value.currentFolderId)
        }.onSuccess { nodeInfo ->
            val title = if (nodeInfo?.isNodeKeyDecrypted == false) {
                LocalizedText.StringRes(resId = R.string.shared_items_verify_credentials_undecrypted_folder)
            } else {
                LocalizedText.Literal(nodeInfo?.name ?: "")
            }
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
     * Monitor storage over quota state
     */
    private fun monitorStorageOverQuota() {
        viewModelScope.launch {
            monitorStorageStateEventUseCase()
                .collectLatest { storageState ->
                    _uiState.update { state ->
                        val isStorageOverQuota = storageState.storageState == StorageState.Red
                                || storageState.storageState == StorageState.PayWall

                        state.copy(
                            isStorageOverQuota = isStorageOverQuota,
                        )
                    }
                }
        }
    }

    /**
     * Monitor transfer over quota state
     */
    private fun monitorTransferOverQuota() {
        viewModelScope.launch {
            monitorTransferOverQuotaUseCase()
                .collectLatest { isTransferOverQuota ->
                    _uiState.update { state ->
                        state.copy(
                            isTransferOverQuota = isTransferOverQuota,
                        )
                    }
                }
        }
    }

    /**
     * Consume transfer over quota warning.
     */
    private fun onConsumeOverQuotaWarning() {
        _uiState.update { state ->
            state.copy(
                isTransferOverQuota = false,
                isStorageOverQuota = false
            )
        }
    }

    /**
     * Check if the contact verification banner should be shown if the current folder is an incoming share
     */
    private fun checkCurrentFolderContactVerification() {
        if (!isSharedSourceType) return
        viewModelScope.launch {
            runCatching {
                val isContactVerificationOn = getContactVerificationWarningUseCase()
                if (!isContactVerificationOn) return@runCatching

                val showBanner = if (nodeSourceType == NodeSourceType.INCOMING_SHARES) {
                    val email =
                        getIncomingShareParentUserEmailUseCase(uiState.value.currentFolderId)
                    email?.let { !areCredentialsVerifiedUseCase(it) } ?: false
                } else {
                    false
                }

                _uiState.update {
                    it.copy(
                        isContactVerificationOn = true,
                        showContactNotVerifiedBanner = showBanner
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun checkWritePermission() {
        viewModelScope.launch {
            runCatching {
                val accessPermission = getNodeAccessPermission(uiState.value.currentFolderId)
                val hasWritePermission = accessPermission == AccessPermission.OWNER ||
                        accessPermission == AccessPermission.READWRITE ||
                        accessPermission == AccessPermission.FULL
                _uiState.update { state ->
                    state.copy(hasWritePermission = hasWritePermission)
                }
            }.onFailure {
                Timber.e(it, "Failed to check write permission")
                _uiState.update { state ->
                    state.copy(hasWritePermission = false)
                }
            }
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

    private val isSharedSourceType: Boolean
        get() = nodeSourceType == NodeSourceType.INCOMING_SHARES ||
                nodeSourceType == NodeSourceType.OUTGOING_SHARES

    @AssistedFactory
    interface Factory {
        fun create(navKey: CloudDriveNavKey): CloudDriveViewModel
    }
}
