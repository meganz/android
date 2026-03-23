package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeGridViewItem
import mega.privacy.android.shared.nodes.components.NodeListViewItem
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.components.rememberNodeItems
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun NodesExplorerScreen(
    viewModel: NodesExplorerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
) {
    val uiState by viewModel.nodesExplorerUiState.collectAsStateWithLifecycle()
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()

    NodesExplorerScreen(
        uiState = uiState,
        uiStateShared = uiStateShared,
        onNavigateBack = onNavigateBack,
        consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
        onFolderClick = onNavigateToFolder,
        onFileClick = viewModel::fileClicked,
        onRefreshNodes = viewModel::refreshNodes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodesExplorerScreen(
    uiState: NodesExplorerUiState,
    uiStateShared: NodesExplorerSharedUiState,
    onNavigateBack: () -> Unit,
    consumeNavigateBack: () -> Unit,
    onFolderClick: (NodeId) -> Unit,
    onFileClick: (NodeUiItem<TypedNode>) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier = Modifier,
) = with(uiStateShared) {
    EventEffect(
        event = navigateBack,
        onConsumed = consumeNavigateBack,
    ) { onNavigateBack() }

    val visibleItems = rememberNodeItems(
        nodeUIItems = items,
        showHiddenItems = showHiddenNodes,
        isHiddenNodesEnabled = isHiddenNodesEnabled,
    )
    val onItemClicked: (NodeUiItem<TypedNode>) -> Unit = { item ->
        when {
            item.isFolderNode -> onFolderClick(item.id)
            isSelectionModeEnabled -> onFileClick(item)
        }
    }
    NodeViewWithHeader(
        items = visibleItems,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        nodesLoadingState = nodesLoadingState,
        emptyView = {
            if (uiState.isRoot) EmptyRoot()
            else EmptyFolder()
        },
        itemListView = {
            NodeListViewItem(
                nodeUiItem = it,
                isInSelectionMode = isSelectionModeEnabled,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                onItemClicked = onItemClicked
            )
        },
        itemGridView = {
            NodeGridViewItem(
                nodeUiItem = it,
                isInSelectionMode = isSelectionModeEnabled,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                onItemClicked = onItemClicked
            )
        },
        onRefreshNodes = onRefreshNodes,
        modifier = modifier,
    )
}

@Composable
private fun EmptyFolder() {
    MegaEmptyView(
        text = stringResource(sharedR.string.context_empty_folder_title),
        imagePainter = painterResource(iconPackR.drawable.ic_empty_folder),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
    )
}

@Composable
private fun EmptyRoot() {
    MegaEmptyView(
        text = stringResource(sharedR.string.context_empty_cloud_drive_title),
        imagePainter = painterResource(iconPackR.drawable.ic_usp_2),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
    )
}

@Composable
@CombinedThemePreviews
fun NodesExplorerScreenEmptyPreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            NodesExplorerScreen(
                uiState = NodesExplorerUiState(),
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                ),
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onFileClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

@Composable
@CombinedThemePreviews
fun NodesExplorerScreenPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            NodesExplorerScreen(
                uiState = NodesExplorerUiState(),
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                    isSelectionModeEnabled = true,
                    items = (1..4L).map { previewFolderNodeUiItem(it) }
                            + (10..15L).map { previewFileNodeUiItem(it) }
                ),
                onNavigateBack = {},
                consumeNavigateBack = {},
                onFolderClick = {},
                onFileClick = {},
                onRefreshNodes = {},
            )
        }
    }
}

internal const val NODES_EXPLORER_EMPTY_VIEW_TAG = "nodes_explorer_view:empty_view"