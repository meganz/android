package mega.privacy.android.feature.clouddrive.presentation.shares.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.publiclink.MonitorLinksUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksAction
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LinksViewModel @Inject constructor(
    private val monitorLinksUseCase: MonitorLinksUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val getLinksSortOrderUseCase: GetLinksSortOrderUseCase,
    private val setCloudSortOrder: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinksUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        monitorViewType()
        getSortOrder()
        viewModelScope.launch { loadLinks() }
        monitorLinks()
    }

    /**
     * Process links actions and call relevant methods
     */
    fun processAction(action: LinksAction) {
        when (action) {
            is LinksAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is LinksAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is LinksAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is LinksAction.SelectAllItems -> selectAllItems()
            is LinksAction.DeselectAllItems -> deselectAllItems()
            is LinksAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is LinksAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
            is LinksAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
        }
    }

    private fun monitorLinks() {
        viewModelScope.launch {
            monitorLinksUseCase(true)
                .catch { Timber.e(it) }
                .collectLatest { nodes ->
                    val nodeUiItems = nodeUiItemMapper(
                        nodeList = nodes,
                        existingItems = uiState.value.items,
                        nodeSourceType = NodeSourceType.LINKS
                    )
                    _uiState.update { state ->
                        state.copy(
                            items = nodeUiItems
                        )
                    }
                }
        }
    }

    private suspend fun loadLinks() {
        runCatching {
            val nodes = monitorLinksUseCase(true).first()
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                existingItems = uiState.value.items,
                nodeSourceType = NodeSourceType.LINKS
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

    /**
     * Handle item click - open file or navigate to folder
     */
    private fun onItemClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        if (uiState.value.isInSelectionMode) {
            toggleItemSelection(nodeUiItem)
            return
        }
        when (val item = nodeUiItem.node) {
            is PublicLinkFile -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = item.node
                    )
                }
            }

            is TypedFileNode -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = item
                    )
                }
            }

            is TypedFolderNode -> {
                _uiState.update { state ->
                    state.copy(
                        navigateToFolderEvent = triggered(item)
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
            state.copy(items = updatedItems)
        }
    }

    /**
     * Select all items
     */
    private fun selectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = true) }
        _uiState.update { state ->
            state.copy(items = updatedItems)
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
                getLinksSortOrderUseCase(true)
            }.onSuccess { sortOrder ->
                val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
                _uiState.update {
                    it.copy(
                        selectedSortConfiguration = sortOrderPair,
                        selectedSortOrder = sortOrder
                    )
                }
                if (refresh) {
                    loadLinks()
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
                setCloudSortOrder(order)
            }.onSuccess {
                getSortOrder(refresh = true)
            }.onFailure {
                Timber.e(it, "Failed to set sort order")
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
