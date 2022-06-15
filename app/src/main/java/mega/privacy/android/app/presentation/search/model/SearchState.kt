package mega.privacy.android.app.presentation.search.model

/**
 * Search UI state
 *
 * @param searchParentHandle current search parent handle
 * @param textSubmitted Flag to control if a search has been performed
 *                      It is also used to prevent a search to be performed
 *                      if the user is not manually triggering it
 * @param searchQuery Current search query
 * @param searchDepth Current search depth count
 */
data class SearchState(
    val searchParentHandle: Long,
    val textSubmitted: Boolean,
    val searchQuery: String?,
    val searchDepth: Int
)