package mega.privacy.android.app.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.RootNodeExists
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.search.model.SearchState
import mega.privacy.android.app.search.usecase.SearchNodesUseCase
import mega.privacy.android.app.search.usecase.SearchNodesUseCase.Companion.TYPE_GENERAL
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to SearchFragment
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param rootNodeExists Check if the root node exists
 * @param searchNodesUseCase Perform a search request
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    private val rootNodeExists: RootNodeExists,
    private val getRootFolder: GetRootFolder,
    private val searchNodesUseCase: SearchNodesUseCase,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _uiState = MutableStateFlow(initializeState())

    /**
     * public UI State
     */
    val uiState: StateFlow<SearchState> = _uiState

    /**
     * Temporary live data that can be observed from the search fragment
     * Should be replace by directly observing uiState flow
     * after migrating SearchFragment to kotlin
     */
    val uiStateLiveData = _uiState.map { Event(it) }.asLiveData()

    /**
     * Monitor global node updates
     */
    var updateNodes: LiveData<Event<List<MegaNode>>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
            .map { Event(it) }
            .asLiveData()


    /**
     * Current search cancel token after a search request has been performed
     */
    private var searchCancelToken: MegaCancelToken? = null

    /**
     * Set the current search parent handle
     */
    fun setSearchParentHandle(handle: Long) = viewModelScope.launch {
        _uiState.update { it.copy(searchParentHandle = handle) }
    }

    /**
     * Set the flag textSubmitted
     *
     * @param submitted
     */
    fun setTextSubmitted(submitted: Boolean) = viewModelScope.launch {
        _uiState.update { it.copy(textSubmitted = submitted) }
    }

    /**
     * Check is the search query is valid
     *
     * @return true if the query is not null and not empty
     */
    fun isSearchQueryValid(): Boolean = !_uiState.value.searchQuery.isNullOrEmpty()

    /**
     * Set the current search query
     *
     * @param query the query to set
     */
    fun setSearchQuery(query: String) = viewModelScope.launch {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Reset the search query an empty string
     */
    fun resetSearchQuery() = viewModelScope.launch {
        _uiState.update { it.copy(searchQuery = "") }
    }

    /**
     * Reset the search level to initial value
     */
    fun resetSearchDepth() = viewModelScope.launch {
        _uiState.update { it.copy(searchDepth = -1) }
    }

    /**
     * Decrease by 1 the search depth
     */
    fun decreaseSearchDepth() = viewModelScope.launch {
        _uiState.update { it.copy(searchDepth = it.searchDepth - 1) }
    }

    /**
     * Increase by 1 the search depth
     */
    fun increaseSearchDepth() = viewModelScope.launch {
        _uiState.update { it.copy(searchDepth = it.searchDepth + 1) }
    }

    /**
     * Perform a search request
     *
     * @param managerState the manager UI state of the ManagerViewModel
     */
    fun performSearch(
        managerState: ManagerState,
    ) = viewModelScope.launch {
        if (!rootNodeExists()) {
            Timber.e("Root node is null.")
            return@launch
        }

        //stop from query for empty string.
        setTextSubmitted(true)

        val query = uiState.value.searchQuery
        val parentHandleSearch = uiState.value.searchParentHandle
        val drawerItem = managerState.searchDrawerItem
        val parentHandle = getParentHandleForSearch(managerState)
        val sharesTab = managerState.searchSharedTab
        val isFirstNavigationLevel = managerState.isFirstNavigationLevel

        cancelSearch()
        searchCancelToken = initNewSearch()
        searchCancelToken?.let { token ->
            searchNodesUseCase.get(
                query,
                parentHandleSearch,
                parentHandle,
                TYPE_GENERAL,
                token,
                drawerItem,
                sharesTab,
                isFirstNavigationLevel
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { nodes, _ ->
                    finishSearch(nodes ?: emptyList())
                }
        }
    }

    /**
     * Get the parent handle from where the search is performed
     *
     * @param managerState the UI manager state of the ManagerViewModel
     */
    private suspend fun getParentHandleForSearch(managerState: ManagerState): Long {
        return when (managerState.searchDrawerItem) {
            DrawerItem.CLOUD_DRIVE -> managerState.browserParentHandle
            DrawerItem.SHARED_ITEMS ->
                when (managerState.searchSharedTab) {
                    ManagerActivity.OUTGOING_TAB -> managerState.outgoingParentHandle
                    ManagerActivity.LINKS_TAB -> managerState.linksParentHandle
                    ManagerActivity.INCOMING_TAB -> managerState.incomingParentHandle
                    else -> managerState.incomingParentHandle
                }
            DrawerItem.RUBBISH_BIN -> managerState.rubbishBinParentHandle
            DrawerItem.INBOX -> managerState.inboxParentHandle
            else -> getRootFolder()?.handle ?: INVALID_HANDLE
        }
    }

    /**
     * Cancel a search request
     */
    fun cancelSearch() {
        setNodes(null)
        searchCancelToken?.cancel()
    }

    /**
     * Initialize a new search request
     *
     * @return a mega cancel token
     */
    private fun initNewSearch(): MegaCancelToken {
        setIsInSearchProgress(true)
        return MegaCancelToken.createInstance()
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
     * Set the nodes in the UI state
     *
     * @param nodes a list of nodes to set
     */
    private fun setNodes(nodes: List<MegaNode>?) = viewModelScope.launch {
        _uiState.update { it.copy(nodes = nodes) }
    }

    /**
     * Set the flag is in search progress in the UI state
     *
     * @param isInProgress true if a search request is in progress
     */
    private fun setIsInSearchProgress(isInProgress: Boolean) = viewModelScope.launch {
        _uiState.update { it.copy(isInProgress = isInProgress) }
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

}