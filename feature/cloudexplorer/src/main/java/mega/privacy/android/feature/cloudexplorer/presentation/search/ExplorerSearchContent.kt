package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerSharedUiState
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.search.presentation.component.RecentSearchesView
import mega.privacy.android.shared.search.presentation.component.SearchEmptyStateView
import mega.privacy.android.shared.search.presentation.model.SearchEmptyContent

/**
 * Generic search surface: owns the [ExplorerSearchViewModel] (debounce + recent searches). Before
 * any query it shows recent searches / landing; once a query is entered it renders [content] with
 * the debounced query that drives the per-source search. The raw [query] text is owned by the host.
 *
 * @param recentSearchesEnabled `false` opts out of the global (node) recent searches — chat sets it.
 */
@Composable
internal fun ExplorerSearchContent(
    query: String?,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    recentSearchesEnabled: Boolean = true,
    @StringRes landingDescription: Int = sharedR.string.search_landing_subtitle,
    content: @Composable (debouncedQuery: String?) -> Unit,
) {
    val searchViewModel = hiltViewModel<ExplorerSearchViewModel>()
    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val searchData = searchUiState as? ExplorerSearchUiState.Data
    val debouncedQuery = searchData?.debouncedQuery

    LaunchedEffect(query) { searchViewModel.onQueryChanged(query) }

    // Record a recent search only once the debounced value matches this tab's own input, so the
    // shared ViewModel's leftover query from another tab (e.g. chat) is never saved on attach.
    if (recentSearchesEnabled) {
        LaunchedEffect(debouncedQuery, query) {
            if (debouncedQuery == query) searchViewModel.saveRecentSearch(debouncedQuery)
        }
    }

    if (query.isNullOrBlank()) {
        val recentSearches = searchData?.recentSearches.orEmpty()
        when {
            recentSearchesEnabled && recentSearches.isNotEmpty() -> RecentSearchesView(
                modifier = modifier
                    .fillMaxSize()
                    .testTag(EXPLORER_SEARCH_RECENT_TAG),
                queries = recentSearches,
                onClicked = { recentQuery, _ -> onQueryChanged(recentQuery) },
                onClearAllClicked = searchViewModel::clearRecentSearches,
            )

            !recentSearchesEnabled || searchUiState !is ExplorerSearchUiState.Loading ->
                SearchEmptyStateView(
                    modifier = modifier
                        .fillMaxSize()
                        .testTag(EXPLORER_SEARCH_LANDING_TAG),
                    content = SearchEmptyContent(
                        title = LocalizedText.StringRes(sharedR.string.search_landing_title),
                        description = LocalizedText.StringRes(landingDescription),
                        image = IconPackR.drawable.ic_search_02,
                    ),
                )
        }
        return
    }

    content(debouncedQuery)
}

/**
 * Builds the search-result folder-click callback: resolves the tapped folder's full ancestor chain
 * via [NodeExplorerSharedViewModel.resolveSearchResultStack], navigates to it and closes search.
 */
@Composable
internal fun rememberSearchResultFolderClick(
    viewModel: NodeExplorerSharedViewModel,
    onNavigateToFolderPath: (List<NodeId>) -> Unit,
    onCloseSearch: () -> Unit,
): (NodeId) -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    return { nodeId ->
        coroutineScope.launch {
            if (viewModel.nodeExplorerSharedUiState.value.isConnected) {
                onNavigateToFolderPath(viewModel.resolveSearchResultStack(nodeId))
                onCloseSearch()
            } else {
                snackbarHostState?.showAutoDurationSnackbar(
                    resources.getString(sharedR.string.error_no_internet_title)
                )
            }
        }
    }
}

@Composable
internal fun SearchResultsEmptyView(modifier: Modifier = Modifier) {
    SearchEmptyStateView(
        modifier = modifier.testTag(EXPLORER_SEARCH_EMPTY_TAG),
        content = SearchEmptyContent(
            title = LocalizedText.StringRes(sharedR.string.photos_search_empty_state_title),
            description = LocalizedText.StringRes(sharedR.string.photos_search_empty_state_description),
            image = IconPackR.drawable.ic_search_02,
        ),
    )
}

@Composable
internal fun SearchLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(EXPLORER_SEARCH_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Projects the search slice of the shared browse state onto the [NodesExplorerSharedUiState.items]/
 * [NodesExplorerSharedUiState.nodesLoadingState] the content composables render, so search reuses
 * the browse content without leaking into the browse list.
 *
 * Stays [NodesLoadingState.Loading] until the results match [query] (debounce window + in-flight
 * search), so the empty-results view never flashes before the real results arrive.
 */
internal fun NodesExplorerSharedUiState.asSearchState(query: String?) =
    copy(
        items = searchItems,
        nodesLoadingState = if (query == searchedQuery) searchLoadingState else NodesLoadingState.Loading,
    )


internal const val EXPLORER_SEARCH_RECENT_TAG = "explorer_search:recent"
internal const val EXPLORER_SEARCH_LANDING_TAG = "explorer_search:landing"
internal const val EXPLORER_SEARCH_EMPTY_TAG = "explorer_search:empty"
internal const val EXPLORER_SEARCH_LOADING_TAG = "explorer_search:loading"
