package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterResult
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiState
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val searchUseCase: SearchUseCase,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val typeFilterToSearchMapper: TypeFilterToSearchMapper,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var nodeMultiSelectionJob: Job? = null

    init {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { query -> performSearch(query) }
        }
        monitorViewType()
        // TODO Handle others, links sort types
        monitorSortOrder()
        monitorHiddenNodeSettings()
    }

    fun processAction(action: SearchUiAction) {
        when (action) {
            is SearchUiAction.UpdateSearchText -> updateSearchText(action.text)
            is SearchUiAction.SelectFilter -> updateFilter(action.result)
            is SearchUiAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is SearchUiAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is SearchUiAction.SetSortOrder -> setSortOrder(action.sortConfiguration)
            is SearchUiAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is SearchUiAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is SearchUiAction.SelectAllItems -> selectAllItems()
            is SearchUiAction.DeselectAllItems -> deselectAllItems()
            is SearchUiAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is SearchUiAction.NavigateBackEventConsumed -> {} // TODO
            is SearchUiAction.OverQuotaConsumptionWarning -> {} // TODO
        }
    }

    private fun updateFilter(result: SearchFilterResult) {
        _uiState.update { state ->
            when (result) {
                is SearchFilterResult.Type -> state.copy(typeFilterOption = result.option)
                is SearchFilterResult.DateModified -> state.copy(dateModifiedFilterOption = result.option)
                is SearchFilterResult.DateAdded -> state.copy(dateAddedFilterOption = result.option)
            }
        }
        viewModelScope.launch { performSearch(_uiState.value.searchText) }
    }

    private fun updateSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
        searchQueryFlow.value = text
    }

    private suspend fun performSearch(
        query: String = uiState.value.searchedQuery,
    ) {
        if (query.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    items = emptyList(),
                    searchedQuery = query,
                    nodesLoadingState = NodesLoadingState.Idle,
                )
            }
            return
        }

        _uiState.update { it.copy(nodesLoadingState = NodesLoadingState.Loading) }

        deselectAllItems()
        runCatching {
            cancelCancelTokenUseCase()
            val nodes = searchUseCase(
                parentHandle = NodeId(args.parentHandle),
                nodeSourceType = args.nodeSourceType,
                searchParameters = SearchParameters(
                    query = query,
                    searchCategory = typeFilterToSearchMapper(
                        typeFilterOption = uiState.value.typeFilterOption,
                        nodeSourceType = args.nodeSourceType
                    ),
                    modificationDate = uiState.value.dateModifiedFilterOption,
                    creationDate = uiState.value.dateAddedFilterOption,
                ),
                isSingleActivityEnabled = true
            )
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = args.nodeSourceType,
                existingItems = _uiState.value.items,
            )
            _uiState.update { state ->
                state.copy(
                    items = nodeUiItems,
                    searchedQuery = query,
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                )
            }
        }.onFailure { throwable ->
            if (throwable !is CancellationException) {
                _uiState.update { state ->
                    state.copy(
                        items = emptyList(),
                        nodesLoadingState = NodesLoadingState.FullyLoaded,
                    )
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

        when (val node = nodeUiItem.node) {
            is TypedFolderNode -> {
                _uiState.update { state ->
                    state.copy(
                        navigateToFolderEvent = triggered(node)
                    )
                }
            }

            is PublicLinkFile -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = node
                    )
                }
            }

            is TypedFileNode -> {
                _uiState.update { state ->
                    state.copy(
                        openedFileNode = node
                    )
                }
            }
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

    private fun monitorSortOrder() {
        monitorSortCloudOrderUseCase()
            .catch { Timber.e(it) }
            .filterNotNull()
            .onEach {
                updateSortOrder(it)
                performSearch()
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

    private fun setSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
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
     * Handle the event when a file node is opened
     */
    private fun onOpenedFileNodeHandled() {
        _uiState.update { state ->
            state.copy(openedFileNode = null)
        }
    }

    private fun monitorHiddenNodeSettings() {
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

    @AssistedFactory
    interface Factory {
        fun create(args: Args): SearchViewModel
    }

    data class Args(
        val parentHandle: Long,
        val nodeSourceType: NodeSourceType,
    )

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

