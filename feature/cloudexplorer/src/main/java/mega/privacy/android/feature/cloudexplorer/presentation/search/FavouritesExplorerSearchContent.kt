package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerContent
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerViewModel
import mega.privacy.android.shared.nodes.selection.NodeSelectionState

/**
 * Favourites search results, reusing the browse [FavouritesExplorerViewModel].
 */
@Composable
internal fun FavouritesExplorerSearchContent(
    query: String?,
    onQueryChanged: (String) -> Unit,
    isFolderPicker: Boolean,
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
    val viewModel =
        hiltViewModel<FavouritesExplorerViewModel, FavouritesExplorerViewModel.Factory> { factory ->
            factory.create(FavouritesExplorerViewModel.Args(showFiles = !isFolderPicker))
        }
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()

    LaunchedEffect(debouncedQuery, query) {
        if (debouncedQuery == query) viewModel.onSearchQuery(debouncedQuery)
    }

    FavouritesExplorerContent(
        uiStateShared = uiStateShared.asSearchState(query),
        isFolderPicker = isFolderPicker,
        onNavigateBack = {},
        consumeNavigateBack = {},
        onFolderClick = rememberSearchResultFolderClick(
            viewModel,
            onNavigateToFolderPath,
            onCloseSearch
        ),
        onRefreshNodes = {},
        selectionState = nodeSelectionState,
        isSelectionModeEnabled = isFileSelectionEnabled,
        disabledNodeIds = disabledNodeIds,
        videosOnly = videosOnly,
        emptyView = { SearchResultsEmptyView() },
        modifier = modifier,
    )
}
