package mega.privacy.android.feature.clouddrive.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val getAllFavoritesUseCase: GetAllFavoritesUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouritesUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        monitorViewType()
        monitorFavourites()
        monitorCloudSortOrder()
    }

    /**
     * Process FavouritesAction and call relevant methods
     */
    fun processAction(action: FavouritesAction) {
        when (action) {
            is FavouritesAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is FavouritesAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is FavouritesAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is FavouritesAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is FavouritesAction.SelectAllItems -> selectAllItems()
            is FavouritesAction.DeselectAllItems -> deselectAllItems()
            is FavouritesAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
        }
    }

    private suspend fun loadNodes() {
        runCatching {
            val nodes = getAllFavoritesUseCase().first()
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.FAVOURITES,
                highlightedNodeId = null,
                highlightedNames = null,
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

    private fun monitorFavourites() {
        viewModelScope.launch {
            getAllFavoritesUseCase()
                .catch { Timber.e(it) }
                .collectLatest { nodes ->
                    val nodeUiItems = nodeUiItemMapper(
                        nodeList = nodes,
                        nodeSourceType = NodeSourceType.FAVOURITES,
                        highlightedNodeId = null,
                        highlightedNames = null,
                        existingItems = uiState.value.items,
                    )
                    _uiState.update { state ->
                        state.copy(
                            items = nodeUiItems,
                            isLoading = false
                        )
                    }
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
            }
        }
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

    private fun monitorCloudSortOrder() {
        monitorSortCloudOrderUseCase()
            .catch { Timber.e(it) }
            .filterNotNull()
            .onEach {
                updateSortOrder(it)
                loadNodes()
            }
            .launchIn(viewModelScope)
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

    /**
     * Handle the event when a file node is opened
     */
    private fun onOpenedFileNodeHandled() {
        _uiState.update { state ->
            state.copy(openedFileNode = null)
        }
    }
}
