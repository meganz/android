package mega.privacy.android.app.presentation.search

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.mapper.DateFilterOptionStringResMapper
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterOptionStringResMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.app.presentation.search.model.DateFilterWithName
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.model.TypeFilterWithName
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeSourceType.OTHER
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * SearchActivity View Model
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @property monitorNodeUpdatesUseCase [MonitorNodeUpdatesUseCase]
 * @property getSearchCategoriesUseCase [GetSearchCategoriesUseCase]
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
class SearchActivityViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val searchUseCase: SearchUseCase,
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase,
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
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
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
    private var searchJob: Job? = null

    private val nodeSourceType =
        stateHandle.get<NodeSourceType>(SearchActivity.SEARCH_TYPE) ?: OTHER
    private val parentHandle =
        stateHandle.get<Long>(SearchActivity.PARENT_HANDLE) ?: MegaApiJava.INVALID_HANDLE

    private var showHiddenItems: Boolean = true

    init {
        checkDropdownChipsFlag()
        checkSearchDescriptionFlag()
        monitorNodeUpdatesForSearch()
        initializeSearch()
        checkViewType()
        monitorAccountDetail()
        monitorShowHiddenItems()
    }

    private fun checkDropdownChipsFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.DropdownChips)
            }.onSuccess { flag ->
                _state.update {
                    it.copy(dropdownChipsEnabled = flag)
                }
            }.onFailure {
                Timber.e("Get feature flag failed $it")
            }
        }
    }

    private fun checkSearchDescriptionFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.SearchWithDescription)
            }.onSuccess { flag ->
                _state.update {
                    it.copy(searchDescriptionEnabled = flag)
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
                    nodeSourceType = nodeSourceType,
                    typeSelectedFilterOption = null,
                    dateModifiedSelectedFilterOption = null,
                    dateAddedSelectedFilterOption = null,
                )
            }
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
        }
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
        searchJob?.cancel()
        _state.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            runCatching {
                cancelCancelTokenUseCase()
                searchUseCase(
                    parentHandle = NodeId(getCurrentParentHandle()),
                    nodeSourceType = nodeSourceType,
                    searchParameters = SearchParameters(
                        query = getCurrentSearchQuery(),
                        searchTarget = nodeSourceTypeToSearchTargetMapper(nodeSourceType),
                        searchCategory = state.value.typeSelectedFilterOption?.let {
                            typeFilterToSearchMapper(it.type)
                        } ?: state.value.selectedFilter?.filter ?: SearchCategory.ALL,
                        modificationDate = state.value.dateModifiedSelectedFilterOption?.date,
                        creationDate = state.value.dateAddedSelectedFilterOption?.date,
                        description = if (state.value.searchDescriptionEnabled == true) getCurrentSearchQuery() else null,
                    )
                )
            }.onSuccess {
                onSearchSuccess(it)
            }.onFailure { ex ->
                onSearchFailure(ex)
            }
        }
    }

    //If folder is opened from search screen we are setting query as empty
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

    private fun filterNonSensitiveNodes(nodes: List<TypedNode>?): List<TypedNode>? {
        val accountType = _state.value.accountType ?: return nodes
        return if (showHiddenItems || !accountType.isPaid) {
            nodes
        } else {
            nodes?.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
    }

    private fun getEmptySearchState() =
        if (state.value.dropdownChipsEnabled == true) {
            emptySearchViewMapper(
                isSearchChipEnabled = true,
                category = typeFilterToSearchMapper(state.value.typeSelectedFilterOption?.type),
                searchQuery = state.value.searchQuery,
                isDateFilterApplied = state.value.dateAddedSelectedFilterOption != null || state.value.dateModifiedSelectedFilterOption != null
            )
        } else {
            emptySearchViewMapper(
                isSearchChipEnabled = true,
                category = state.value.selectedFilter?.filter,
                searchQuery = state.value.searchQuery
            )
        }

    /**
     * Update search filter on selection
     *
     * @param selectedChip
     */
    fun updateFilter(selectedChip: SearchFilter?) {
        _state.update {
            it.copy(
                selectedFilter = selectedChip.takeIf { selectedChip?.filter != state.value.selectedFilter?.filter },
                resetScroll = state.value.resetScroll.not()
            )
        }
        viewModelScope.launch { performSearch() }
    }

    /**
     * Update search query on typing
     *
     * @param query search text
     */
    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query, resetScroll = state.value.resetScroll.not()) }
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
                resetScroll = state.value.resetScroll.not()
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
                resetScroll = state.value.resetScroll.not()
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
                resetScroll = state.value.resetScroll.not()
            )
        }
        viewModelScope.launch { performSearch() }
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
                dateAddedSelectedFilterOption = null
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
                _state.update {
                    it.copy(accountType = accountDetail.levelDetail?.accountType)
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
