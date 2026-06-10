package mega.privacy.android.shared.search.presentation.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

/**
 * Generic UI state consumed by [mega.privacy.android.shared.search.presentation.SearchShellScaffold].
 *
 * It is intentionally free of any node/chat/contact specific type. Each consumer maps its own
 * UI state into this model so the shared search shell can render the search bar, filter chips,
 * recent searches and the landing/loading/empty/results state machine.
 *
 * @property searchText Current text in the search field.
 * @property searchedQuery The last query that produced the current results.
 * @property placeholder Placeholder text shown in the search field.
 * @property isLoading True while results are being loaded.
 * @property isPreSearch True when no search has been performed yet (shows recent searches or landing).
 * @property isEmpty True when a search produced no results.
 * @property recentSearches Recent search queries, most recent first.
 * @property isRecentSearchesLoading True while recent searches are still loading.
 * @property filters Filter chips to display; empty hides the chips row.
 */
@Immutable
data class SearchShellState(
    val searchText: String = "",
    val searchedQuery: String = "",
    val placeholder: LocalizedText = LocalizedText.Literal(""),
    val isLoading: Boolean = false,
    val isPreSearch: Boolean = true,
    val isEmpty: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val isRecentSearchesLoading: Boolean = true,
    val filters: List<SearchFilterChipState> = emptyList(),
)
