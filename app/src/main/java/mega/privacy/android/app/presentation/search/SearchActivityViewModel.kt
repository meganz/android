package mega.privacy.android.app.presentation.search

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeSourceType.OTHER
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * SearchActivity View Model
 * @property monitorNodeUpdatesUseCase [MonitorNodeUpdatesUseCase]
 * @property searchNodesUseCase [SearchNodesUseCase]
 * @property getSearchCategoriesUseCase [GetSearchCategoriesUseCase]
 * @property searchFilterMapper [SearchFilterMapper]
 * @property emptySearchViewMapper [EmptySearchViewMapper]
 * @property cancelCancelTokenUseCase [CancelCancelTokenUseCase]
 * @property setViewType [SetViewType]
 * @property monitorViewType [MonitorViewType]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property monitorOfflineNodeUpdatesUseCase [MonitorOfflineNodeUpdatesUseCase]
 */
@HiltViewModel
class SearchActivityViewModel @Inject constructor(
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val searchNodesUseCase: SearchNodesUseCase,
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase,
    private val searchFilterMapper: SearchFilterMapper,
    private val emptySearchViewMapper: EmptySearchViewMapper,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * private UI state
     */
    private val _state = MutableStateFlow(SearchActivityState())

    /**
     * public UI State
     */
    val state: StateFlow<SearchActivityState> = _state

    /**
     * Search job
     */
    private var searchJob: Job? = null

    private val isFirstLevel = stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL) ?: false
    private val nodeSourceType =
        stateHandle.get<NodeSourceType>(SearchActivity.SEARCH_TYPE) ?: OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    init {
        monitorNodeUpdatesForSearch()
        initializeSearch()
        checkViewType()
    }

    private fun initializeSearch() {
        _state.update { it.copy(nodeSourceType = nodeSourceType) }
        runCatching {
            getSearchCategoriesUseCase().map { searchFilterMapper(it) }
                .filterNot { it.filter == SearchCategory.ALL }
        }.onSuccess { filters ->
            _state.update {
                it.copy(filters = filters, selectedFilter = null)
            }
        }.onFailure {
            Timber.e("Get search categories failed $it")
        }
        performSearch()
    }


    private fun monitorNodeUpdatesForSearch() {
        viewModelScope.launch {
            merge(monitorNodeUpdatesUseCase(), monitorOfflineNodeUpdatesUseCase()).conflate()
                .collectLatest {
                    performSearch()
                }
        }
    }

    /**
     * Perform search by entering query or change in search type
     */
    private fun performSearch() {
        _state.update { it.copy(isSearching = true) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runCatching {
                cancelCancelTokenUseCase()
                searchNodesUseCase(
                    query = state.value.searchQuery,
                    parentHandle = parentHandle,
                    nodeSourceType = nodeSourceType,
                    isFirstLevel = isFirstLevel,
                    searchCategory = state.value.selectedFilter?.filter ?: SearchCategory.ALL
                )
            }.onSuccess {
                onSearchSuccess(it)
            }.onFailure { ex ->
                onSearchFailure(ex)
            }
        }
    }

    private fun onSearchFailure(ex: Throwable) {
        Timber.e(ex)
        val emptyState = getEmptySearchState()
        _state.update {
            it.copy(
                searchItemList = emptyList(),
                isSearching = false,
                emptyState = emptyState
            )
        }
    }

    private suspend fun onSearchSuccess(searchResults: List<TypedNode>?) =
        coroutineScope {
            if (searchResults.isNullOrEmpty()) {
                val emptyState = getEmptySearchState()
                _state.update {
                    it.copy(
                        searchItemList = emptyList(),
                        isSearching = false,
                        emptyState = emptyState
                    )
                }
            } else {
                val nodeUIItems = searchResults.map { typedNode ->
                    NodeUIItem(node = typedNode, isSelected = false)
                }
                _state.update { state ->
                    val cloudSortOrder = getCloudSortOrder()
                    state.copy(
                        searchItemList = nodeUIItems,
                        isSearching = false,
                        sortOrder = cloudSortOrder
                    )
                }
            }
        }

    private fun getEmptySearchState() = emptySearchViewMapper(
        isSearchChipEnabled = true,
        category = state.value.selectedFilter?.filter,
        searchQuery = state.value.searchQuery
    )

    /**
     * Update search filter on selection
     *
     * @param selectedChip
     */
    fun updateFilter(selectedChip: SearchFilter?) {
        _state.update { it.copy(selectedFilter = selectedChip.takeIf { selectedChip?.filter != state.value.selectedFilter?.filter }) }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Update search query on typing
     *
     * @param query search text
     */
    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        viewModelScope.launch { performSearch() }
    }

    /**
     * This method will handle Item click event from NodesView and will update
     * [state] accordingly if items already selected/unselected, update check count else get MegaNode
     * and navigate to appropriate activity
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.searchItemList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        if (_state.value.selectedNodes.isNotEmpty()) {
            updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
        } else {
            _state.update { it.copy(lastSelectedNode = nodeUIItem.node) }
        }
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        val searchResultsUpdated = _state.value.searchItemList.asSequence().map {
            it.copy(isSelected = false)
        }
        _state.update {
            it.copy(
                searchItemList = searchResultsUpdated.toList(),
                selectedNodes = emptySet()
            )
        }
    }

    /**
     * Select ALl
     */
    fun selectAll() = viewModelScope.launch {
        val searchResultsUpdated = _state.value.searchItemList.asSequence().map {
            it.copy(isSelected = true)
        }
        val selectedNodes = _state.value.searchItemList.asSequence().map {
            it.node
        }.toSet()
        _state.update {
            it.copy(
                searchItemList = searchResultsUpdated.toList(),
                selectedNodes = selectedNodes,
            )
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeSelection(nodeUIItem: NodeUIItem<TypedNode>, index: Int) =
        viewModelScope.launch {
            nodeUIItem.isSelected = !nodeUIItem.isSelected
            val selectedNod = state.value.selectedNodes.toMutableSet()
            if (state.value.selectedNodes.contains(nodeUIItem.node)) {
                selectedNod.remove(nodeUIItem.node)
            } else {
                selectedNod.add(nodeUIItem.node)
            }
            val newNodesList =
                _state.value.searchItemList.updateItemAt(index = index, item = nodeUIItem)
            _state.update {
                it.copy(
                    searchItemList = newNodesList,
                    optionsItemInfo = null,
                    selectedNodes = selectedNod,
                )
            }
        }

    /**
     * This method will handle Long click on a NodesView and check the selected item
     *
     * @param nodeUIItem [NodeUIItem]
     */
    fun onLongItemClicked(nodeUIItem: NodeUIItem<TypedNode>) {
        val index =
            _state.value.searchItemList.indexOfFirst { it.node.id.longValue == nodeUIItem.id.longValue }
        updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
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
     * When item is clicked on activity
     */
    fun onItemPerformedClicked() {
        _state.update {
            it.copy(lastSelectedNode = null)
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
     * When we change sort order from UI
     */
    fun onSortOrderChanged() = performSearch()

    /**
     * Show error message on UI
     */
    fun showShowErrorMessage(@StringRes errorMessageResId: Int) {
        _state.update { it.copy(errorMessageId = errorMessageResId) }
    }

    /**
     * Remove error message
     */
    fun errorMessageShown() {
        _state.update { it.copy(errorMessageId = null) }
    }
}
