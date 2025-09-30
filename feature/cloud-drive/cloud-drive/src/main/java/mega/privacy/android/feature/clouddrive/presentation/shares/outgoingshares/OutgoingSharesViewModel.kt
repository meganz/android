package mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares

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
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeNameByIdUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.shares.GetOutgoingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model.OutgoingSharesAction
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model.OutgoingSharesUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OutgoingSharesViewModel @Inject constructor(
    private val getNodeNameByIdUseCase: GetNodeNameByIdUseCase,
    private val getOutgoingSharesChildrenNodeUseCase: GetOutgoingSharesChildrenNodeUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val getCloudSortOrderUseCase: GetCloudSortOrder,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val setOthersSortOrder: SetOthersSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutgoingSharesUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        monitorViewType()
        viewModelScope.launch { updateTitle() }
        viewModelScope.launch { loadNodes() }
        monitorNodeUpdates()
    }

    /**
     * Process OutgoingSharesAction and call relevant methods
     */
    fun processAction(action: OutgoingSharesAction) {
        when (action) {
            is OutgoingSharesAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is OutgoingSharesAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is OutgoingSharesAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is OutgoingSharesAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is OutgoingSharesAction.SelectAllItems -> selectAllItems()
            is OutgoingSharesAction.DeselectAllItems -> deselectAllItems()
            is OutgoingSharesAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is OutgoingSharesAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
        }
    }

    private suspend fun loadNodes() {
        val folderId = uiState.value.currentFolderId
        runCatching {
            val nodes = getOutgoingSharesChildrenNodeUseCase(folderId.longValue)
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                existingItems = uiState.value.items,
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
            // TODO handle root node of outgoing shares
            monitorNodeUpdatesByIdUseCase(NodeId(-1L)).collectLatest { change ->
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
    }

    /**
     * Deselect all items and reset selection state
     */
    private fun deselectAllItems() {
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
}
