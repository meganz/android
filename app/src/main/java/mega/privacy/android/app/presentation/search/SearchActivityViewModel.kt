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
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.CloudDrive
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.IncomingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Links
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.OutgoingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.RubbishBin
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.model.mapper.NodeToolbarActionMapper
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.entity.search.SearchType.INCOMING_SHARES
import mega.privacy.android.domain.entity.search.SearchType.LINKS
import mega.privacy.android.domain.entity.search.SearchType.OTHER
import mega.privacy.android.domain.entity.search.SearchType.OUTGOING_SHARES
import mega.privacy.android.domain.entity.search.SearchType.RUBBISH_BIN
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
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
    @CloudDrive private val cloudDriveToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @IncomingShares private val incomingSharesToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @OutgoingShares private val outgoingSharesToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @Links private val linksToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @RubbishBin private val rubbishBinToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    private val nodeToolbarActionMapper: NodeToolbarActionMapper,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
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

    private var rubbishBinNode: UnTypedNode? = null

    private val isFirstLevel = stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL) ?: false
    private val searchType =
        stateHandle.get<SearchType>(SearchActivity.SEARCH_TYPE) ?: OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    init {
        monitorNodeUpdatesForSearch()
        initializeSearch()
        checkViewType()
        getRubbishBinNode()
    }

    private fun getRubbishBinNode() {
        viewModelScope.launch {
            runCatching {
                getRubbishNodeUseCase()
            }.onSuccess { rubbishBin ->
                rubbishBinNode = rubbishBin
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun initializeSearch() {
        _state.update { it.copy(searchType = searchType) }
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
            merge(monitorNodeUpdates(), monitorOfflineNodeUpdatesUseCase()).conflate()
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
            val menuActions = updateToolbarState(selectedNod, newNodesList.size)
            _state.update {
                it.copy(
                    searchItemList = newNodesList,
                    optionsItemInfo = null,
                    selectedNodes = selectedNod,
                    menuActions = menuActions,
                )
            }
        }

    private suspend fun updateToolbarState(
        selectedNodes: Set<TypedNode>,
        searchResultCount: Int,
    ): List<MenuAction> {
        val toolbarOptions = getToolbarOptions()
        val canBeMovedToTarget = if (state.value.searchType != RUBBISH_BIN) {
            nodeCanBeMovedToRubbishBin(selectedNodes)
        } else false
        val anyNodeInBackups = if (state.value.searchType != RUBBISH_BIN) {
            selectedNodes.any { isNodeInBackupsUseCase(handle = it.id.longValue) }
        } else false
        val hasAccessPermission = if (state.value.searchType == INCOMING_SHARES) {
            checkIfNodeHasFullAccessPermission(selectedNodes)
        } else true
        return nodeToolbarActionMapper(
            toolbarOptions = toolbarOptions,
            hasNodeAccessPermission = hasAccessPermission,
            selectedNodes = selectedNodes,
            resultCount = searchResultCount,
            allNodeCanBeMovedToTarget = canBeMovedToTarget,
            noNodeInBackups = anyNodeInBackups.not()
        )
    }

    private fun getToolbarOptions() = when (state.value.searchType) {
        INCOMING_SHARES -> incomingSharesToolbarOptions
        OUTGOING_SHARES -> outgoingSharesToolbarOptions
        LINKS -> linksToolbarOptions
        RUBBISH_BIN -> rubbishBinToolbarOptions
        else -> cloudDriveToolbarOptions
    }

    private suspend fun checkIfNodeHasFullAccessPermission(selectedNodes: Set<TypedNode>): Boolean {
        var hasFullAccess = true
        runCatching {
            selectedNodes.all { getNodeAccessPermission(it.id) == AccessPermission.FULL }
        }.onSuccess {
            hasFullAccess = it
        }.onFailure {
            Timber.e(it)
            hasFullAccess = false
        }
        return hasFullAccess
    }

    private suspend fun nodeCanBeMovedToRubbishBin(
        selectedNodes: Set<TypedNode>,
    ) = rubbishBinNode?.let { rubbishBinNode ->
        selectedNodes.any { node ->
            checkNodeCanBeMovedToTargetNode(nodeId = node.id, targetNodeId = rubbishBinNode.id)
        }
    } ?: true

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