package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.TabsScope
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.components.ExplorerNodeGridItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.ExplorerNodeListItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.explorerNodeClick
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.FAVOURITES_TAB_INDEX
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.FAVOURITES_TAB_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.navigateToFolder
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.navigateToFolderPath
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.rememberVisibleItems
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NODES_EXPLORER_EMPTY_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.previewNodeExplorerData
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.toTabSignal
import mega.privacy.android.feature.cloudexplorer.presentation.search.FavouritesExplorerSearchContent
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun FavouritesExplorerContent(
    uiState: NodeExplorerUiState,
    isFolderPicker: Boolean,
    onNavigateBack: () -> Unit,
    consumeNavigateBack: () -> Unit,
    onFolderClick: (NodeId) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier = Modifier,
    selectionState: NodeSelectionState = rememberNodeSelectionState(),
    isSelectionModeEnabled: Boolean = false,
    disabledNodeIds: Set<NodeId> = emptySet(),
    videosOnly: Boolean = false,
    allowsFolderSelection: Boolean = false,
    emptyView: @Composable () -> Unit = { EmptyFolder(isFolderPicker) },
) {
    when (uiState) {
        NodeExplorerUiState.Loading -> NodesViewSkeleton()
        is NodeExplorerUiState.Data -> {
            EventEffect(
                event = uiState.navigateBack,
                onConsumed = consumeNavigateBack,
            ) { onNavigateBack() }

            val visibleItems = rememberVisibleItems(
                items = uiState.items,
                showHiddenNodes = uiState.showHiddenNodes,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
            )
            val onItemClicked = explorerNodeClick(
                selectionState = selectionState,
                disabledNodeIds = disabledNodeIds,
                videosOnly = videosOnly,
                isSelectionModeEnabled = isSelectionModeEnabled,
                onFolderClick = onFolderClick,
                allowsFolderSelection = allowsFolderSelection,
            )
            NodeViewWithHeader(
                items = visibleItems,
                nodeSourceType = uiState.nodeSourceType,
                nodesLoadingState = uiState.nodesLoadingState,
                emptyView = emptyView,
                itemListView = { nodeItem ->
                    ExplorerNodeListItem(
                        item = nodeItem,
                        isSelected = selectionState.selectedNodeIds.contains(nodeItem.id),
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                        videosOnly = videosOnly,
                        disabledNodeIds = disabledNodeIds,
                        allowsFolderSelection = allowsFolderSelection,
                        onSelectionCheckedChange = { selectionState.toggleSelection(nodeItem.id) },
                        onItemClicked = { onItemClicked(nodeItem) },
                    )
                },
                itemGridView = {
                    ExplorerNodeGridItem(
                        item = it,
                        isSelected = selectionState.selectedNodeIds.contains(it.id),
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                        videosOnly = videosOnly,
                        disabledNodeIds = disabledNodeIds,
                        onItemClicked = { onItemClicked(it) },
                    )
                },
                onRefreshNodes = onRefreshNodes,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun EmptyFolder(isFolderPicker: Boolean) {
    EmptyStateView(
        title = stringResource(sharedR.string.homepage_favourites_empty_hint),
        imagePainter = painterResource(iconPackR.drawable.ic_hearts_glass),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
        description = SpannableText(
            stringResource(
                if (isFolderPicker) {
                    sharedR.string.favourite_folders_empty_screen_description
                } else {
                    sharedR.string.favourites_empty_screen_description
                }
            )
        ),
    )
}

@Composable
internal fun TabsScope.FavouritesExplorerTab(
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    shareUris: List<UriPath>?,
    showSearch: Boolean,
    searchQuery: String?,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    protectedUserTap: (() -> Unit) -> Unit,
    onNavigate: (NavKey) -> Unit,
    onNavigateBack: () -> Unit,
    selectionState: NodeSelectionState = rememberNodeSelectionState(),
    isSelectionModeEnabled: Boolean = false,
    disabledNodeIds: Set<NodeId> = emptySet(),
    videosOnly: Boolean = false,
) {
    val viewModel =
        hiltViewModel<FavouritesExplorerViewModel, FavouritesExplorerViewModel.Factory> { factory ->
            factory.create(
                args = FavouritesExplorerViewModel.Args(showFiles = !explorerMode.isFolderPicker)
            )
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signal = remember(
        uiState, isSelectionModeEnabled, disabledNodeIds, videosOnly, explorerMode,
    ) {
        uiState.toTabSignal(
            disabledNodeIds = disabledNodeIds,
            videosOnly = videosOnly,
            isFileSelectionEnabled = isSelectionModeEnabled,
            allowsFolderSelection = explorerMode.allowsFolderSelection,
        )
    }
    val explorerViewModel = hiltViewModel<ExplorerViewModel>()
    val explorerUiState by explorerViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(signal) { explorerViewModel.onTabSignal(FAVOURITES_TAB_INDEX, signal) }

    addTextTabWithScrollableContent(
        tabItem = TabItems(
            title = stringResource(sharedR.string.video_section_title_favourite_playlist),
            testTag = FAVOURITES_TAB_TAG,
        ),
    ) { isActive, modifier ->
        if (showSearch) {
            FavouritesExplorerSearchContent(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                isFolderPicker = explorerMode.isFolderPicker,
                nodeSelectionState = selectionState,
                isFileSelectionEnabled = isSelectionModeEnabled,
                videosOnly = videosOnly,
                disabledNodeIds = disabledNodeIds,
                isConnected = explorerUiState.isConnected,
                onNavigateToFolderPath = navigateToFolderPath(
                    nodeSourceType = NodeSourceType.FAVOURITES,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    disabledNodeIds = disabledNodeIds.toList(),
                    protectedUserTap = protectedUserTap,
                    onNavigate = onNavigate,
                ),
                onCloseSearch = onCloseSearch,
                recentSearchesEnabled = isActive,
                modifier = modifier,
            )
        } else {
            FavouritesExplorerContent(
                uiState = uiState,
                isFolderPicker = explorerMode.isFolderPicker,
                onNavigateBack = { protectedUserTap { onNavigateBack() } },
                consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
                onFolderClick = navigateToFolder(
                    nodeSourceType = NodeSourceType.FAVOURITES,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    disabledNodeIds = disabledNodeIds.toList(),
                    protectedUserTap = protectedUserTap,
                    onNavigate = onNavigate,
                ),
                onRefreshNodes = viewModel::refreshNodes,
                selectionState = selectionState,
                isSelectionModeEnabled = isSelectionModeEnabled,
                disabledNodeIds = disabledNodeIds,
                videosOnly = videosOnly,
                allowsFolderSelection = explorerMode.allowsFolderSelection,
                modifier = modifier,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun EmptyFolderPreview(
    @PreviewParameter(BooleanProvider::class) isFolderPicker: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            FavouritesExplorerContent(
                uiState = previewNodeExplorerData(nodeSourceType = NodeSourceType.FAVOURITES),
                isFolderPicker = isFolderPicker,
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun FavouritesExplorerFolderDestinationScreenPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            FavouritesExplorerContent(
                uiState = previewNodeExplorerData(
                    items = previewFolders(),
                    nodeSourceType = NodeSourceType.FAVOURITES,
                ),
                isFolderPicker = true,
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

private fun previewFolders() = (1..7L).map { id ->
    previewFolderNodeUiItem(id)
}