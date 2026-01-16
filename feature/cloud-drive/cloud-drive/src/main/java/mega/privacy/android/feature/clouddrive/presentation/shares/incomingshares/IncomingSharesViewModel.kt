package mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares

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
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesUiState
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class IncomingSharesViewModel @Inject constructor(
    private val getIncomingSharesChildrenNodeUseCase: GetIncomingSharesChildrenNodeUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val setOthersSortOrder: SetOthersSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomingSharesUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        monitorViewType()
        getSortOrder()
        viewModelScope.launch { loadNodes() }
        monitorNodeUpdates()
        checkSearchRevampEnabled()
    }

    /**
     * Process incoming shares actions and call relevant methods
     */
    fun processAction(action: IncomingSharesAction) {
        when (action) {
            is IncomingSharesAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is IncomingSharesAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is IncomingSharesAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is IncomingSharesAction.SelectAllItems -> selectAllItems()
            is IncomingSharesAction.DeselectAllItems -> deselectAllItems()
            is IncomingSharesAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is IncomingSharesAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
        }
    }

    private suspend fun loadNodes() {
        val folderId = uiState.value.currentFolderId
        runCatching {
            val isContactVerificationOn = runCatching {
                getContactVerificationWarningUseCase()
            }.getOrDefault(false)
            val nodes = getIncomingSharesChildrenNodeUseCase(folderId.longValue)
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                existingItems = uiState.value.items,
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = isContactVerificationOn
            )
            _uiState.update { state ->
                state.copy(
                    items = nodeUiItems,
                    isLoading = false,
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(
                nodeId = NodeId(-1L),
                nodeSourceType = NodeSourceType.INCOMING_SHARES
            ).collectLatest { change ->
                if (change == NodeChanges.Remove) {
                    // If current folder is moved to rubbish bin, navigate back
                    _uiState.update {
                        it.copy(navigateBack = triggered)
                    }
                } else {
                    loadNodes()
                }
            }
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
        (nodeUiItem.node as? TypedFolderNode)?.let { node ->
            _uiState.update { state ->
                state.copy(
                    navigateToFolderEvent = triggered(node)
                )
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
    }

    /**
     * Deselect all items and reset selection state
     */
    private fun deselectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = false) }
        _uiState.update { state ->
            state.copy(
                items = updatedItems,
            )
        }
    }

    /**
     * Select all items
     */
    private fun selectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = true) }
        _uiState.update { state ->
            state.copy(
                items = updatedItems,
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

    private fun getSortOrder(
        refresh: Boolean = false,
    ) {
        viewModelScope.launch {
            runCatching {
                getOthersSortOrder()
            }.onSuccess { sortOrder ->
                val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
                _uiState.update {
                    it.copy(
                        selectedSortConfiguration = sortOrderPair,
                        selectedSortOrder = sortOrder
                    )
                }
                if (refresh) {
                    loadNodes()
                }
            }.onFailure {
                Timber.e(it, "Failed to get sort order")
            }
        }
    }

    /**
     * Set sort order
     */
    fun setSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setOthersSortOrder(order)
            }.onSuccess {
                getSortOrder(refresh = true)
            }.onFailure {
                Timber.e(it, "Failed to set sort order")
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
}
