package mega.privacy.android.feature.cloudexplorer.presentation.search

/**
 * UI state for the explorer search surface.
 */
internal sealed interface ExplorerSearchUiState {

    /** Recent searches are still loading and no query has been entered yet. */
    data object Loading : ExplorerSearchUiState

    /**
     * @property debouncedQuery Debounced query that drives the per-tab searches; `null`/blank while
     *   nothing is being searched.
     * @property recentSearches Recent search queries, most recent first.
     */
    data class Data(
        val debouncedQuery: String?,
        val recentSearches: List<String>,
    ) : ExplorerSearchUiState
}
