package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreenContent
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.shared.nodes.selection.NodeSelectionState

/**
 * Cloud drive search results. Reuses the browse [NodesExplorerViewModel] so search results live in
 * its searchItems, separate from the browse list.
 */
@Composable
internal fun NodesExplorerSearchContent(
    query: String?,
    onQueryChanged: (String) -> Unit,
    nodeSelectionState: NodeSelectionState,
    isFileSelectionEnabled: Boolean,
    videosOnly: Boolean,
    disabledNodeIds: Set<NodeId>,
    onNavigateToFolderPath: (List<NodeId>) -> Unit,
    onCloseSearch: () -> Unit,
    recentSearchesEnabled: Boolean,
    modifier: Modifier = Modifier,
) = ExplorerSearchContent(
    query,
    onQueryChanged,
    modifier,
    recentSearchesEnabled
) { debouncedQuery ->
    val viewModel = hiltViewModel<NodesExplorerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(debouncedQuery, query) {
        if (debouncedQuery == query) viewModel.onSearchQuery(debouncedQuery)
    }

    NodesExplorerScreenContent(
        uiState = uiState.asSearchState(query),
        onNavigateBack = {},
        consumeNavigateBack = {},
        onFolderClick = rememberSearchResultFolderClick(
            viewModel,
            onNavigateToFolderPath,
            onCloseSearch
        ),
        onRefreshNodes = { viewModel.onSearchQuery(debouncedQuery) },
        selectionState = nodeSelectionState,
        isSelectionModeEnabled = isFileSelectionEnabled,
        disabledNodeIds = disabledNodeIds,
        videosOnly = videosOnly,
        emptyView = { SearchResultsEmptyView() },
        modifier = modifier,
    )
}
