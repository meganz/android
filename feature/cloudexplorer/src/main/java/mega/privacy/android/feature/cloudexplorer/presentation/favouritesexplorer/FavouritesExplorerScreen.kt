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
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerGridViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerListViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.FAVOURITES_TAB_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NODES_EXPLORER_EMPTY_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerSharedUiState
import mega.privacy.android.feature.cloudexplorer.presentation.search.FavouritesExplorerSearchContent
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun FavouritesExplorerContent(
    uiStateShared: NodesExplorerSharedUiState,
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
    emptyView: @Composable () -> Unit = { EmptyFolder(isFolderPicker) },
) = with(uiStateShared) {
    EventEffect(
        event = navigateBack,
        onConsumed = consumeNavigateBack,
    ) { onNavigateBack() }

    val visibleItems = remember(showHiddenNodes, items) {
        return@remember if (showHiddenNodes || !isHiddenNodesEnabled) {
            items
        } else {
            items.filterNot { it.isSensitive }
        }
    }
    val onItemClicked: (NodeViewItem<TypedNode>) -> Unit = { item ->
        when {
            // Pre-added nodes are shown checked but disabled; ignore taps.
            item.id in disabledNodeIds -> Unit

            item.isFolderNode -> {
                onFolderClick(item.id)
                selectionState.deselectAll()
            }

            // Only videos can be added to a playlist; ignore taps on other files.
            videosOnly && !item.isVideoNode -> Unit

            isSelectionModeEnabled -> selectionState.toggleSelection(item.id)
        }
    }
    NodeViewWithHeader(
        items = visibleItems,
        nodeSourceType = uiStateShared.nodeSourceType,
        nodesLoadingState = nodesLoadingState,
        emptyView = emptyView,
        itemListView = {
            val isAlreadyAdded = it.id in disabledNodeIds
            val isUnsupportedFile = videosOnly && !it.isFolderNode && !it.isVideoNode
            val isDisabled = isAlreadyAdded || isUnsupportedFile
            CloudExplorerListViewItem(
                title = it.title.text,
                subtitle = it.subtitle.text(),
                icon = it.iconRes,
                description = it.formattedDescription?.text,
                tags = it.tags,
                thumbnailData = it.thumbnailData,
                isTakenDown = it.isTakenDown,
                showIsVerified = it.showIsVerified,
                label = it.nodeLabel,
                isSensitive = it.isSensitive && isHiddenNodesEnabled,
                showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                isHighlighted = it.isHighlighted,
                isSelected = selectionState.selectedNodeIds.contains(it.id) || isAlreadyAdded,
                isInSelectionMode = isSelectionModeEnabled && (it.node is FileNode),
                onItemClicked = { onItemClicked(it) },
                enabled = (it.isFolderNode || isSelectionModeEnabled) && !isDisabled,
            )
        },
        itemGridView = {
            val isAlreadyAdded = it.id in disabledNodeIds
            val isUnsupportedFile = videosOnly && !it.isFolderNode && !it.isVideoNode
            val isDisabled = isAlreadyAdded || isUnsupportedFile
            CloudExplorerGridViewItem(
                name = it.title.text,
                iconRes = it.iconRes,
                thumbnailData = it.thumbnailData,
                isTakenDown = it.isTakenDown,
                duration = it.duration,
                isFolderNode = it.isFolderNode,
                isVideoNode = it.isVideoNode,
                onClick = { onItemClicked(it) },
                isSensitive = it.isSensitive && isHiddenNodesEnabled,
                showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                isHighlighted = it.isHighlighted,
                label = it.nodeLabel,
                isSelected = selectionState.selectedNodeIds.contains(it.id) || isAlreadyAdded,
                isInSelectionMode = isSelectionModeEnabled && (it.node is FileNode),
                enabled = (it.isFolderNode || isSelectionModeEnabled) && !isDisabled,
            )
        },
        onRefreshNodes = onRefreshNodes,
        modifier = modifier,
    )
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
    onHasContentChanged: (Boolean) -> Unit = {},
) {
    val viewModel =
        hiltViewModel<FavouritesExplorerViewModel, FavouritesExplorerViewModel.Factory> { factory ->
            factory.create(
                args = FavouritesExplorerViewModel.Args(showFiles = !explorerMode.isFolderPicker)
            )
        }
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiStateShared.items.isEmpty()) {
        onHasContentChanged(uiStateShared.items.isNotEmpty())
    }
    val onFolderClick: (NodeId) -> Unit = { nodeId ->
        protectedUserTap {
            onNavigate(
                NodesExplorerNavKey(
                    nodeId = nodeId,
                    nodeSourceType = uiStateShared.nodeSourceType,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    disabledNodeIds = disabledNodeIds.toList(),
                )
            )
        }
    }
    addTextTabWithScrollableContent(
        tabItem = TabItems(
            title = stringResource(sharedR.string.video_section_title_favourite_playlist),
            testTag = FAVOURITES_TAB_TAG,
        ),
    ) { _, modifier ->
        if (showSearch) {
            FavouritesExplorerSearchContent(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                isFolderPicker = explorerMode.isFolderPicker,
                nodeSelectionState = selectionState,
                isFileSelectionEnabled = isSelectionModeEnabled,
                videosOnly = videosOnly,
                disabledNodeIds = disabledNodeIds,
                onFolderClick = onFolderClick,
                onCloseSearch = onCloseSearch,
                modifier = modifier,
            )
        } else {
            FavouritesExplorerContent(
                uiStateShared = uiStateShared,
                isFolderPicker = explorerMode.isFolderPicker,
                onNavigateBack = { protectedUserTap { onNavigateBack() } },
                consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
                onFolderClick = onFolderClick,
                onRefreshNodes = viewModel::refreshNodes,
                selectionState = selectionState,
                isSelectionModeEnabled = isSelectionModeEnabled,
                disabledNodeIds = disabledNodeIds,
                videosOnly = videosOnly,
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
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                ),
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
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                    isSelectionModeEnabled = false,
                    items = previewFolders()
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