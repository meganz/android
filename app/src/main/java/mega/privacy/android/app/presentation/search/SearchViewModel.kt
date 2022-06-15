package mega.privacy.android.app.presentation.search

import androidx.lifecycle.LiveData
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
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.presentation.search.model.SearchState
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to SearchFragment
 *
 * @param monitorNodeUpdates Monitor global node updates
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
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
     * Monitor global node updates
     */
    var updateNodes: LiveData<Event<List<MegaNode>>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
            .map { Event(it) }
            .asLiveData()


    /**
     * Set the current search parent handle
     */
    fun setSearchParentHandle(handle: Long) = viewModelScope.launch {
        Timber.d("setSearchParentHandle")
        _uiState.update {
            it.copy(searchParentHandle = handle)
        }
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
     * Initialize the UI State
     */
    private fun initializeState(): SearchState =
        SearchState(
            searchParentHandle = -1L,
            textSubmitted = false,
            searchQuery = "",
            searchDepth = -1,
        )

}