package mega.privacy.android.app.presentation.search.model

import nz.mega.sdk.MegaNode

/**
 * Search UI state
 *
 * @param nodes nodes retrieved from a search request, null if not set yet
 * @param searchParentHandle current search parent handle
 * @param textSubmitted flag to control if a search has been performed
 *                      It is also used to prevent a search to be performed
 *                      if the user is not manually triggering it
 * @param searchQuery current search query
 * @param searchDepth current search depth count
 * @param isInProgress current progress state of the search request
 */
data class SearchState(
    val nodes: List<MegaNode>?,
    val searchParentHandle: Long,
    val textSubmitted: Boolean,
    val searchQuery: String?,
    val searchDepth: Int,
    val isInProgress: Boolean,
)