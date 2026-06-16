package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingSharesExplorerContent
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingSharesExplorerViewModel

/**
 * Incoming shares search results, reusing the browse [IncomingSharesExplorerViewModel].
 */
@Composable
internal fun IncomingSharesExplorerSearchContent(
    query: String?,
    onQueryChanged: (String) -> Unit,
    onNavigateToFolderPath: (List<NodeId>) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) = ExplorerSearchContent(query, onQueryChanged, modifier) { debouncedQuery ->
    val viewModel = hiltViewModel<IncomingSharesExplorerViewModel>()
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()
    LaunchedEffect(debouncedQuery) { viewModel.onSearchQuery(debouncedQuery) }

    IncomingSharesExplorerContent(
        uiStateShared = uiStateShared.asSearchState(),
        onNavigateBack = {},
        consumeNavigateBack = {},
        onFolderClick = rememberSearchResultFolderClick(
            viewModel,
            onNavigateToFolderPath,
            onCloseSearch
        ),
        onRefreshNodes = { viewModel.onSearchQuery(debouncedQuery) },
        emptyView = { SearchResultsEmptyView() },
        modifier = modifier,
    )
}
