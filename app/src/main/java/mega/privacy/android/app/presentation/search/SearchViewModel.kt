package mega.privacy.android.app.presentation.search

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.mapper.DateFilterOptionStringResMapper
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterOptionStringResMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.app.presentation.search.model.DateFilterWithName
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.app.presentation.search.model.SearchViewState
import mega.privacy.android.app.presentation.search.model.TypeFilterWithName
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeSourceType.OTHER
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.sortorder.GetSortOrderByNodeSourceTypeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature_flags.AppFeatures
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * SearchActivity View Model
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @property monitorNodeUpdatesUseCase [MonitorNodeUpdatesUseCase]
 * @property searchFilterMapper [SearchFilterMapper]
 * @property nodeSourceTypeToSearchTargetMapper [NodeSourceTypeToSearchTargetMapper]
 * @property typeFilterToSearchMapper [TypeFilterToSearchMapper]
 * @property emptySearchViewMapper [EmptySearchViewMapper]
 * @property cancelCancelTokenUseCase [CancelCancelTokenUseCase]
 * @property setViewType [SetViewType]
 * @property monitorViewType [MonitorViewType]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property monitorOfflineNodeUpdatesUseCase [MonitorOfflineNodeUpdatesUseCase]
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val searchUseCase: SearchUseCase,
    private val searchFilterMapper: SearchFilterMapper,
    private val nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper,
    private val typeFilterToSearchMapper: TypeFilterToSearchMapper,
    private val typeFilterOptionStringResMapper: TypeFilterOptionStringResMapper,
    private val dateFilterOptionStringResMapper: DateFilterOptionStringResMapper,
    private val emptySearchViewMapper: EmptySearchViewMapper,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getSortOrderByNodeSourceTypeUseCase: GetSortOrderByNodeSourceTypeUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * private UI state
     */
    private val _state = MutableStateFlow(SearchViewState())

    /**
     * public UI State
     */
    val state: StateFlow<SearchViewState> = _state
    private var searchJob: Job? = null

    private val nodeSourceType =
        stateHandle.get<NodeSourceType>(SearchActivity.SEARCH_TYPE) ?: OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    private var showHiddenItems: Boolean = true

    init {
        checkSearchFlags()
        initializeSearch()
        checkViewType()
        viewModelScope.launch {
            if (isHiddenNodesActive()) {
                monitorAccountDetail()
                monitorShowHiddenItems()
            }
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    private fun checkSearchFlags() {
        viewModelScope.launch {
            runCatching {
                val description = getFeatureFlagValueUseCase(AppFeatures.SearchWithDescription)
                val tags = getFeatureFlagValueUseCase(AppFeatures.SearchWithTags)
                description to tags
            }.onSuccess { (description, tags) ->
                _state.update {
                    it.copy(searchDescriptionEnabled = description, searchTagsEnabled = tags)
                }
            }.onFailure {
                Timber.e("Get feature flag failed $it")
            }
        }
    }

    private fun initializeSearch() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    rootParentHandle = parentHandle,
                    nodeSourceType = nodeSourceType,
                    typeSelectedFilterOption = null,
                    dateModifiedSelectedFilterOption = null,
                    dateAddedSelectedFilterOption = null,
                )
            }
            performSearch()
        }
    }

    /**
     * Perform search by entering query or change in search type
     *
     * Animate search view when there is a user interaction like typing or changing filter
     * when there is a change in search results due to node updates, no need to animate
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun performSearch() {
        searchJob?.cancel()
        _state.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            runCatching {
                val search = executeSearchQuery()
                channelFlow {
                    send(search())
                    monitorNodeChanges().flatMapLatest {
                        flow { emit(search()) }
                    }.collectLatest {
                        send(it)
                    }
                }.collectLatest(::onSearchSuccess)
            }.onFailure { ex ->
                onSearchFailure(ex)
            }
        }
    }

    /**
     * Monitor node change due to change in node update or offline node update
     * @return flow of search results
     */
    private fun monitorNodeChanges() = merge(
        monitorNodeUpdatesUseCase(),
        monitorOfflineNodeUpdatesUseCase().drop(1)
    ).conflate()

    /**
     * Execute search query
     */
    private fun executeSearchQuery() = suspend {
        cancelCancelTokenUseCase()
        searchUseCase(
            parentHandle = NodeId(getCurrentParentHandle()),
            nodeSourceType = nodeSourceType,
            searchParameters = getSearchParameters()
        )
    }

    private fun getSearchParameters() = SearchParameters(
        query = getCurrentSearchQuery(),
        searchTarget = nodeSourceTypeToSearchTargetMapper(nodeSourceType),
        searchCategory = typeFilterToSearchMapper(
            state.value.typeSelectedFilterOption?.type,
            nodeSourceType
        ),
        modificationDate = state.value.dateModifiedSelectedFilterOption?.date,
        creationDate = state.value.dateAddedSelectedFilterOption?.date,
        description = if (state.value.searchDescriptionEnabled == true) getCurrentSearchQuery() else null,
        tag = if (state.value.searchTagsEnabled == true) getCurrentSearchQuery()
            .removePrefix("#")
            .takeIf {
                nodeSourceType != NodeSourceType.RUBBISH_BIN
            }
        else null
    )

    // If folder is opened from search screen we are setting query as empty
    private fun getCurrentSearchQuery() =
        state.value.searchQuery.takeIf { state.value.navigationLevel.isEmpty() }.orEmpty()

    private fun getCurrentParentHandle() =
        state.value.navigationLevel.lastOrNull()?.first ?: parentHandle

    private fun onSearchFailure(ex: Throwable) {
        if (ex is CancellationException) {
            return
        }
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

    private suspend fun onSearchSuccess(sourceSearchResults: List<TypedNode>?) {
        val searchResults = filterNonSensitiveNodes(sourceSearchResults)
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
            val nodeUIItems = searchResults.distinctBy { it.id.longValue }.map { typedNode ->
                NodeUIItem(
                    node = typedNode,
                    isSelected = false,
                )
            }
            val sortOrder = runCatching {
                if (state.value.isAtRootWithEmptyQuery) {
                    getSortOrderByNodeSourceTypeUseCase(nodeSourceType)
                } else {
                    getCloudSortOrder()
                }
            }.getOrDefault(SortOrder.ORDER_NONE)
            _state.update { state ->
                state.copy(
                    searchItemList = nodeUIItems,
                    isSearching = false,
                    sortOrder = sortOrder
                )
            }
        }
    }

    private fun filterNonSensitiveNodes(nodes: List<TypedNode>?): List<TypedNode>? {
        val accountType = _state.value.accountType ?: return nodes
        return if (showHiddenItems || !accountType.isPaid || _state.value.isBusinessAccountExpired) {
            nodes
        } else {
            nodes?.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
    }

    private fun getEmptySearchState() =
        emptySearchViewMapper(
            isSearchChipEnabled = true,
            category = typeFilterToSearchMapper(
                state.value.typeSelectedFilterOption?.type,
                nodeSourceType
            ),
            searchQuery = state.value.searchQuery,
            isDateFilterApplied = state.value.dateAddedSelectedFilterOption != null || state.value.dateModifiedSelectedFilterOption != null
        )

    /**
     * Update search query on typing
     *
     * @param query search text
     */
    fun updateSearchQuery(query: String) {
        if (state.value.searchQuery == query) return
        _state.update {
            it.copy(
                searchQuery = query,
                resetScrollEvent = triggered
            )
        }
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
            val selectedNode = state.value.selectedNodes.toMutableSet()
            if (state.value.selectedNodes.contains(nodeUIItem.node)) {
                selectedNode.remove(nodeUIItem.node)
            } else {
                selectedNode.add(nodeUIItem.node)
            }
            val newNodesList =
                _state.value.searchItemList.updateItemAt(index = index, item = nodeUIItem)
            _state.update {
                it.copy(
                    searchItemList = newNodesList,
                    optionsItemInfo = null,
                    selectedNodes = selectedNode,
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
     * Updates the type filter with the selected option
     */
    fun setTypeSelectedFilterOption(typeFilterOption: TypeFilterWithName?) {
        _state.update {
            it.copy(
                typeSelectedFilterOption = typeFilterOption,
                resetScrollEvent = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Updates the date modified filter with the selected option
     */
    fun setDateModifiedSelectedFilterOption(dateFilterOption: DateFilterWithName?) {
        _state.update {
            it.copy(
                dateModifiedSelectedFilterOption = dateFilterOption,
                resetScrollEvent = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Updates the date added filter with the selected option
     */
    fun setDateAddedSelectedFilterOption(dateFilterOption: DateFilterWithName?) {
        _state.update {
            it.copy(
                dateAddedSelectedFilterOption = dateFilterOption,
                resetScrollEvent = triggered
            )
        }
        viewModelScope.launch { performSearch() }
    }

    fun onResetScrollEventConsumed() {
        _state.update { it.copy(resetScrollEvent = consumed) }
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

    /**
     * Get filter options based on filter
     *
     * @param filter filter
     */
    fun getFilterOptions(filter: String): List<FilterOptionEntity> = when (filter) {
        TYPE ->
            TypeFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    typeFilterOptionStringResMapper(option),
                    option == state.value.typeSelectedFilterOption?.type
                )
            }

        DATE_MODIFIED ->
            DateFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    dateFilterOptionStringResMapper(option),
                    option == state.value.dateModifiedSelectedFilterOption?.date
                )
            }

        DATE_ADDED -> DateFilterOption.entries.map { option ->
            FilterOptionEntity(
                option.ordinal,
                dateFilterOptionStringResMapper(option),
                option == state.value.dateAddedSelectedFilterOption?.date
            )
        }

        else -> emptyList()
    }

    /**
     * Update filter entity
     */
    fun updateFilterEntity(filter: String, filterOption: FilterOptionEntity) {
        when (filter) {
            TYPE -> {
                val typeOption = TypeFilterOption.entries.getOrNull(filterOption.id)
                    ?.takeIf { it.ordinal != state.value.typeSelectedFilterOption?.type?.ordinal }
                setTypeSelectedFilterOption(
                    typeOption?.let {
                        TypeFilterWithName(
                            it,
                            typeFilterOptionStringResMapper(it)
                        )
                    }
                )
                typeOption?.trackAsAnalyticsEvent()
            }

            DATE_MODIFIED -> {
                val dateModifiedOption = DateFilterOption.entries.getOrNull(filterOption.id)
                    ?.takeIf { it.ordinal != state.value.dateModifiedSelectedFilterOption?.date?.ordinal }

                setDateModifiedSelectedFilterOption(
                    dateModifiedOption?.let {
                        DateFilterWithName(
                            it,
                            dateFilterOptionStringResMapper(it)
                        )
                    }
                )
                dateModifiedOption?.trackAsLastModifiedAnalyticsEvent()
            }

            DATE_ADDED -> {
                val dateAddedOption = DateFilterOption.entries.getOrNull(filterOption.id)
                    ?.takeIf { it.ordinal != state.value.dateAddedSelectedFilterOption?.date?.ordinal }

                setDateAddedSelectedFilterOption(
                    dateAddedOption?.let {
                        DateFilterWithName(
                            it,
                            dateFilterOptionStringResMapper(it)
                        )
                    }
                )
                dateAddedOption?.trackAsDateAddedAnalyticsEvent()
            }
        }
    }

    /**
     * Open folder from search screen
     *
     * @param folderHandle folder handle
     * @param name folder name
     */
    fun openFolder(folderHandle: Long, name: String) {
        val list = _state.value.navigationLevel.toMutableList()
        list.add(Pair(folderHandle, name))
        _state.update { state ->
            state.copy(
                navigationLevel = list,
                typeSelectedFilterOption = null,
                dateModifiedSelectedFilterOption = null,
                dateAddedSelectedFilterOption = null,
                resetScrollEvent = triggered
            )
        }
        performSearch()
    }

    /**
     * Handle back press
     *
     * navigates back to previous folder
     */
    fun navigateBack() {
        val list = _state.value.navigationLevel.toMutableList()
        list.remove(list.last())
        _state.update { state ->
            state.copy(navigationLevel = list)
        }
        performSearch()
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                val accountType = accountDetail.levelDetail?.accountType
                val businessStatus =
                    if (accountType?.isBusinessAccount == true) {
                        getBusinessStatusUseCase()
                    } else null

                _state.update {
                    it.copy(
                        accountType = accountType,
                        hiddenNodeEnabled = true,
                        isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired
                    )
                }
                if (_state.value.isSearching) return@onEach

                performSearch()
            }
            .launchIn(viewModelScope)
    }

    private fun monitorShowHiddenItems() {
        monitorShowHiddenItemsUseCase()
            .conflate()
            .onEach { show ->
                showHiddenItems = show
                if (_state.value.isSearching) return@onEach

                performSearch()
            }
            .launchIn(viewModelScope)
    }
}
