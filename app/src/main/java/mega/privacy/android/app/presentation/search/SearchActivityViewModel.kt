package mega.privacy.android.app.presentation.search

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * SearchActivity View Model
 * @property monitorNodeUpdates [MonitorNodeUpdates]
 * @property searchNodesUseCase [SearchNodesUseCase]
 * @property getParentNodeHandle [GetParentNodeHandle]
 * @property setViewType [SetViewType]
 * @property getSearchCategoriesUseCase [GetSearchCategoriesUseCase]
 * @property searchFilterMapper [SearchFilterMapper]
 * @property emptySearchViewMapper [EmptySearchViewMapper]
 * @property cancelCancelTokenUseCase [CancelCancelTokenUseCase]
 */
@HiltViewModel
class SearchActivityViewModel @Inject constructor(
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val searchNodesUseCase: SearchNodesUseCase,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase,
    private val searchFilterMapper: SearchFilterMapper,
    private val emptySearchViewMapper: EmptySearchViewMapper,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val fileDurationMapper: FileDurationMapper,
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
    private val searchType =
        stateHandle.get<SearchType>(SearchActivity.SEARCH_TYPE) ?: SearchType.OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    init {
        monitorNodeUpdatesForSearch()
        initializeSearch()
        checkViewType()
    }

    private fun initializeSearch() {
        _state.update { it.copy(searchType = searchType) }
        runCatching {
            getSearchCategoriesUseCase().map { searchFilterMapper(it) }
                .filterNot { it.filter == SearchCategory.ALL }
        }.onSuccess { filters ->
            _state.update {
                it.copy(
                    filters = filters,
                    selectedFilter = null,
                )
            }
        }.onFailure {
            Timber.e("Get search categories failed $it")
        }
        performSearch()
    }


    private fun monitorNodeUpdatesForSearch() {
        viewModelScope.launch {
            monitorNodeUpdates().collect { nodeUpdate ->
                if (nodeUpdate.changes.keys.find { state.value.parentHandle == it.parentId.longValue } != null)
                    performSearch()
            }
        }
    }

    /**
     * Perform search by entering query or change in search type
     */
    private fun performSearch() {
        _state.update {
            it.copy(isSearching = true, searchItemList = emptyList())
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runCatching {
                cancelCancelTokenUseCase()
                searchNodesUseCase(
                    query = state.value.searchQuery,
                    parentHandle = parentHandle,
                    searchType = searchType,
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
        _state.update {
            it.copy(
                searchItemList = emptyList(),
                isSearching = false,
                emptyState = getEmptySearchState()
            )
        }
    }

    private suspend fun onSearchSuccess(searchResults: List<TypedNode>?) =
        coroutineScope {
            if (searchResults.isNullOrEmpty()) {
                _state.update {
                    it.copy(isSearching = false, emptyState = getEmptySearchState())
                }
            } else {
                val nodeUIItems = searchResults.map { typedNode ->
                    NodeUIItem(node = typedNode, isSelected = false)
                }
                _state.update { state ->
                    state.copy(
                        searchItemList = nodeUIItems,
                        isSearching = false,
                        sortOrder = getCloudSortOrder()
                    )
                }
                updateNodeUiItemWithOfflineInfo()
            }
        }

    private fun getEmptySearchState() = emptySearchViewMapper(
        isSearchChipEnabled = true,
        category = state.value.selectedFilter?.filter,
        searchQuery = state.value.searchQuery
    )

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private suspend fun updateNodeUiItemWithOfflineInfo() = coroutineScope {
        val nodeUiList = state.value.searchItemList.map { item ->
            ensureActive()
            val fileDuration = if (item.node is FileNode) {
                fileDurationMapper(item.node.type)?.let { TimeUtils.getVideoDuration(it) }
                    ?: run { null }
            } else null
            item.copy(
                fileDuration = fileDuration
            )
        }
        _state.update { it.copy(searchItemList = nodeUiList) }
    }

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
        if (_state.value.isInSelection) {
            updateNodeSelection(nodeUIItem = nodeUIItem, index = index)
        } else {
            if (nodeUIItem.node is FileNode) {
                _state.update {
                    it.copy(
                        itemIndex = index,
                        currentFileNode = nodeUIItem.node
                    )
                }

            } else {
                _state.update {
                    it.copy(
                        currentFolderClickedHandle = nodeUIItem.id.longValue
                    )
                }
            }
        }
    }

    /**
     * This will update [NodeUIItem] list based on and update it on to the UI
     * @param nodeUIItem [NodeUIItem] to be updated
     * @param index Index of [NodeUIItem] in [state]
     */
    private fun updateNodeSelection(nodeUIItem: NodeUIItem<TypedNode>, index: Int) {
        nodeUIItem.isSelected = !nodeUIItem.isSelected
        val selectedNodeHandleList = state.value.selectedNodeHandles.toMutableList()
        selectedNodeHandleList.apply {
            if (nodeUIItem.isSelected) add(nodeUIItem.node.id.longValue) else remove(nodeUIItem.node.id.longValue)
        }
        val pair = selectUnSelectNode(nodeUIItem = nodeUIItem, nodeSelected = nodeUIItem.isSelected)
        selectUnSelectNode(nodeUIItem = nodeUIItem, nodeSelected = nodeUIItem.isSelected)
        val newNodesList =
            _state.value.searchItemList.updateItemAt(index = index, item = nodeUIItem)
        _state.update {
            it.copy(
                selectedFileNodes = pair.first,
                selectedFolderNodes = pair.second,
                searchItemList = newNodesList,
                isInSelection = pair.first > 0 || pair.second > 0,
                selectedNodeHandles = selectedNodeHandleList,
                optionsItemInfo = null
            )
        }
    }

    /**
     * select a node
     * @param nodeUIItem
     * @param nodeSelected if node is selected or removed selection
     * @return Pair of count of Selected File Node and Selected Folder Node
     */
    private fun selectUnSelectNode(
        nodeUIItem: NodeUIItem<TypedNode>,
        nodeSelected: Boolean,
    ): Pair<Int, Int> {
        var totalSelectedFileNode = state.value.selectedFileNodes
        var totalSelectedFolderNode = state.value.selectedFolderNodes
        if (nodeUIItem.node is FolderNode) {
            totalSelectedFolderNode =
                if (nodeSelected) _state.value.selectedFolderNodes + 1 else _state.value.selectedFolderNodes - 1
        } else if (nodeUIItem.node is FileNode) {
            totalSelectedFileNode = if (nodeSelected) _state.value.selectedFileNodes + 1 else
                _state.value.selectedFileNodes - 1
        }
        return Pair(totalSelectedFileNode, totalSelectedFolderNode)
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
            it.copy(
                currentFileNode = null,
                itemIndex = -1,
            )
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
    fun onSortOrderChanged() {
        performSearch()
    }

    /**
     * Show error message on UI
     */
    fun showShowErrorMessage(@StringRes errorMessageResId: Int) {
        _state.update {
            it.copy(
                errorMessageId = errorMessageResId
            )
        }
    }

    /**
     * Remove error message
     */
    fun errorMessageShown() {
        _state.update {
            it.copy(
                errorMessageId = null
            )
        }
    }
}