package mega.privacy.android.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.model.SearchState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.search.GetSearchCategoriesUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to SearchFragment
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param rootNodeExistsUseCase Check if the root node exists
 * @param searchNodesUseCase Perform a search request
 * @param getCloudSortOrder Get the Cloud Sort Order
 * @param getSearchParentNodeHandle Get parent node for current node
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getRootFolder: GetRootFolder,
    private val searchNodesUseCase: SearchNodesUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getSearchParentNodeHandle: GetParentNodeHandle,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getSearchCategoriesUseCase: GetSearchCategoriesUseCase,
    private val searchFilterMapper: SearchFilterMapper,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(initializeState())

    /**
     * public UI State
     */
    val state: StateFlow<SearchState> = _state

    /**
     * Temporary live data that can be observed from the search fragment
     * Should be replace by directly observing uiState flow
     * after migrating SearchFragment to kotlin
     */
    val stateLiveData = _state.map { Event(it) }.asLiveData()

    /**
     * Stack to maintain folder navigation clicks
     */
    private val lastPositionStack: Stack<Int> = Stack()

    private var firstNavigationLevel = false

    init {
        viewModelScope.launch {
            monitorTransferEventsUseCase().collect { event ->
                if (event is TransferEvent.TransferFinishEvent && !event.transfer.isFolderTransfer) {
                    setTextSubmitted(true)
                }
            }
        }
    }

    private fun getSearchFilterCategories() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.SearchWithChipsMVP)
            }.onSuccess { isEnabled ->
                val shouldShow = isEnabled && arrayOf(
                    DrawerItem.HOMEPAGE,
                    DrawerItem.CLOUD_DRIVE
                ).contains(state.value.searchDrawerItem)
                _state.update { it.copy(showChips = shouldShow) }
                if (shouldShow) {
                    runCatching {
                        getSearchCategoriesUseCase().map { searchFilterMapper(it) }
                            .filterNot { it.filter == SearchCategory.ALL }
                    }
                        .onSuccess { filters ->
                            _state.update {
                                it.copy(
                                    filters = filters,
                                    selectedFilter = null
                                )
                            }
                        }.onFailure {
                            Timber.e("Get search categories failed $it")
                        }
                }
            }.onFailure {
                Timber.e("Feature flag check failed $it")
            }
        }
    }

    /**
     * Monitor global node updates
     */
    var updateNodes =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
            .map { Event(it) }
            .asLiveData()


    /**
     * Set the current search parent handle
     */
    fun setSearchParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(searchParentHandle = handle) }
    }

    /**
     * Set the flag textSubmitted
     *
     * @param submitted
     */
    fun setTextSubmitted(submitted: Boolean) = viewModelScope.launch {
        _state.update { it.copy(textSubmitted = submitted) }
    }

    /**
     * Check is the search query is valid
     *
     * @return true if the query is not null and not empty
     */
    fun isSearchQueryValid(): Boolean = !_state.value.searchQuery.isNullOrEmpty()

    /**
     * Set the current search query
     *
     * @param query the query to set
     */
    fun setSearchQuery(query: String) = viewModelScope.launch {
        _state.update { it.copy(searchQuery = query) }
    }

    /**
     * Reset the search query an empty string
     */
    fun resetSearchQuery() = viewModelScope.launch {
        _state.update { it.copy(searchQuery = "") }
    }

    /**
     * Reset the search level to initial value
     */
    fun resetSearchDepth() = viewModelScope.launch {
        _state.update { it.copy(searchDepth = -1) }
    }

    /**
     * Decrease by 1 the search depth
     */
    fun decreaseSearchDepth() = viewModelScope.launch {
        _state.update { it.copy(searchDepth = it.searchDepth - 1) }
    }

    /**
     * Handles Folder item clicked on [SearchFragment]
     */
    fun onFolderClicked(handle: Long, lastFirstVisiblePosition: Int) {
        setSearchParentHandle(handle)
        increaseSearchDepth()
        lastPositionStack.push(lastFirstVisiblePosition)
    }

    /**
     * Increase by 1 the search depth
     */
    private fun increaseSearchDepth() = viewModelScope.launch {
        _state.update { it.copy(searchDepth = it.searchDepth + 1) }
    }

    /**
     * Perform a search request
     *
     * @param browserParentHandle
     * @param rubbishBinParentHandle
     * @param backupsParentHandle
     * @param incomingParentHandle
     * @param outgoingParentHandle
     * @param linksParentHandle
     * @param isFirstNavigationLevel
     */
    fun performSearch(
        browserParentHandle: Long,
        rubbishBinParentHandle: Long,
        backupsParentHandle: Long,
        incomingParentHandle: Long,
        outgoingParentHandle: Long,
        linksParentHandle: Long,
        isFirstNavigationLevel: Boolean,
    ) = viewModelScope.launch {
        if (!rootNodeExistsUseCase()) {
            Timber.e("Root node is null.")
            return@launch
        }

        firstNavigationLevel = isFirstNavigationLevel
        val parentHandle =
            getParentHandleForSearch(
                browserParentHandle,
                rubbishBinParentHandle,
                backupsParentHandle,
                incomingParentHandle,
                outgoingParentHandle,
                linksParentHandle
            )
        _state.update {
            it.copy(searchHandle = parentHandle)
        }

        cancelSearch()
        setIsInSearchProgress(true)
        startSearch()
    }

    /**
     * Start search by calling search api
     */
    private suspend fun startSearch() {
        with(state.value) {
            val nodes = searchNodesUseCase(
                query = searchQuery,
                parentHandleSearch = searchParentHandle,
                parentHandle = searchHandle,
                drawerItem = searchDrawerItem,
                sharesTab = searchSharesTab.position,
                isFirstLevel = firstNavigationLevel,
                searchFilter = selectedFilter
            )
            finishSearch(nodes ?: emptyList())
        }
    }

    /**
     * Get the parent handle from where the search is performed
     *
     * @param browserParentHandle
     * @param rubbishBinParentHandle
     * @param backupsParentHandle
     * @param incomingParentHandle
     * @param outgoingParentHandle
     * @param linksParentHandle
     *
     * @return the parent handle from where the search is performed
     */
    private suspend fun getParentHandleForSearch(
        browserParentHandle: Long,
        rubbishBinParentHandle: Long,
        backupsParentHandle: Long,
        incomingParentHandle: Long,
        outgoingParentHandle: Long,
        linksParentHandle: Long,
    ): Long {
        return when (_state.value.searchDrawerItem) {
            DrawerItem.CLOUD_DRIVE -> browserParentHandle
            DrawerItem.SHARED_ITEMS ->
                when (_state.value.searchSharesTab) {
                    SharesTab.INCOMING_TAB -> incomingParentHandle
                    SharesTab.OUTGOING_TAB -> outgoingParentHandle
                    SharesTab.LINKS_TAB -> linksParentHandle
                    else -> incomingParentHandle
                }

            DrawerItem.RUBBISH_BIN -> rubbishBinParentHandle
            DrawerItem.BACKUPS -> backupsParentHandle
            else -> getRootFolder()?.handle ?: INVALID_HANDLE
        }
    }

    /**
     * Cancel a search request
     */
    fun cancelSearch() {
        setNodes(null)
        viewModelScope.launch {
            cancelCancelTokenUseCase()
        }
    }

    /**
     * Set the search result after a search request has been performed
     *
     * @param nodes the list of nodes to set
     */
    private fun finishSearch(nodes: List<MegaNode>) {
        setIsInSearchProgress(false)
        setNodes(nodes)
    }

    /**
     * Reset the current search drawer item to initial value
     */
    fun resetSearchDrawerItem() = viewModelScope.launch {
        _state.update { it.copy(searchDrawerItem = null) }
    }

    /**
     * Set current search drawer item
     *
     * @param drawerItem
     */
    fun setSearchDrawerItem(drawerItem: DrawerItem) = viewModelScope.launch {
        _state.update { it.copy(searchDrawerItem = drawerItem) }
        getSearchFilterCategories()
    }

    /**
     * Set the current search shares tab
     *
     * @param sharesTab
     */
    fun setSearchSharedTab(sharesTab: SharesTab) = viewModelScope.launch {
        _state.update { it.copy(searchSharesTab = sharesTab) }
    }

    /**
     * Set the nodes in the UI state
     *
     * @param nodes a list of nodes to set
     */
    private fun setNodes(nodes: List<MegaNode>?) = viewModelScope.launch {
        _state.update { it.copy(nodes = nodes) }
    }

    /**
     * Set the flag is in search progress in the UI state
     *
     * @param isInProgress true if a search request is in progress
     */
    private fun setIsInSearchProgress(isInProgress: Boolean) = viewModelScope.launch {
        _state.update { it.copy(isInProgress = isInProgress) }
    }

    /**
     * Initialize the UI State
     */
    private fun initializeState(): SearchState =
        SearchState(
            nodes = null,
            searchParentHandle = -1L,
            textSubmitted = false,
            searchQuery = "",
            searchDepth = -1,
            isInProgress = false
        )

    /**
     * Get Cloud Sort Order
     */
    suspend fun getOrder() = getCloudSortOrder()

    /**
     * Handles back click [SearchFragment]
     */
    fun onBackClicked() {
        cancelSearch()
        val levelSearch = _state.value.searchDepth
        if (levelSearch >= 0) {
            if (levelSearch > 0) {
                viewModelScope.launch {
                    getSearchParentNodeHandle(_state.value.searchParentHandle)?.let {
                        setSearchParentHandle(it)
                    } ?: run {
                        setSearchParentHandle(-1)
                    }
                }
            } else {
                setSearchParentHandle(-1)
            }
        }
    }

    /**
     * Pop scroll position for previous depth
     *
     * @return last position saved
     */
    fun popLastPositionStack(): Int = lastPositionStack.takeIf { it.isNotEmpty() }?.pop() ?: 0

    /**
     * Updates the value of [SearchState.currentViewType]
     *
     * @param newViewType The new [ViewType]
     */
    fun setCurrentViewType(newViewType: ViewType) {
        _state.update { it.copy(currentViewType = newViewType) }
    }

    /**
     * Update search filter on selection
     *
     * @param selectedChip
     */
    fun updateFilter(selectedChip: SearchFilter) {
        val searchFilter = if (selectedChip.filter != state.value.selectedFilter?.filter) {
            selectedChip
        } else {
            null
        }
        _state.update { it.copy(selectedFilter = searchFilter) }
        viewModelScope.launch { startSearch() }
    }
}