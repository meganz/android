package mega.privacy.android.app.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
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
     * Monitor global node updates
     */
    var updateNodes: LiveData<Event<List<MegaNode>>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
            .map { Event(it) }
            .asLiveData()


    /**
     * Flag to control if a search has been performed
     * It is also used to prevent a search to be performed if the user is not manually triggering it
     */
    var textSubmitted: Boolean = false

    /**
     * Current search query
     */
    var searchQuery: String? = ""

    /**
     * Current search depth count
     */
    var searchDepth: Int = -1

    /**
     * Check is the search query is valid
     *
     * @return true if the query is not null and not empty
     */
    fun isSearchQueryValid(): Boolean = !searchQuery.isNullOrEmpty()

    /**
     * Reset the search query an empty string
     */
    fun resetSearchQuery() {
        searchQuery = ""
    }

    /**
     * Reset the search level to initial value
     */
    fun resetSearchDepth() {
        searchDepth = -1
    }

    /**
     * Decrease by 1 the search depth
     */
    fun decreaseSearchDepth() {
        searchDepth--
    }

    /**
     * Increase by 1 the search depth
     */
    fun increaseSearchDepth() {
        searchDepth++
    }

}