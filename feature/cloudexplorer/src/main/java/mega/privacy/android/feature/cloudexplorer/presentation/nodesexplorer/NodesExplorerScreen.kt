package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerGridViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerListViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.model.ExplorerModeData
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.SelectableNodeItem
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun NodesExplorerScreen(
    explorerModeData: ExplorerModeData,
    nodeExplorerId: NodeId,
    nodeSourceType: NodeSourceType,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (NavKey) -> Unit,
) {
    ExplorerScreen(
        explorerModeData = explorerModeData,
        isInnerNavigation = true,
        nodeExplorerId = nodeExplorerId,
        nodeSourceType = nodeSourceType,
        onNavigateBack = onNavigateBack,
        onNavigateToFolder = onNavigateToFolder,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodesExplorerScreenContent(
    uiState: NodesExplorerUiState,
    uiStateShared: NodesExplorerSharedUiState,
    onNavigateBack: () -> Unit,
    consumeNavigateBack: () -> Unit,
    onFolderClick: (NodeId) -> Unit,
    onRefreshNodes: () -> Unit,
    modifier: Modifier = Modifier,
) = with(uiStateShared) {
    EventEffect(
        event = navigateBack,
        onConsumed = consumeNavigateBack,
    ) { onNavigateBack() }

    val visibleItems = remember(showHiddenNodes, items.hashCode()) {
        return@remember if (showHiddenNodes || !isHiddenNodesEnabled) {
            items
        } else {
            items.filterNot { it.isSensitive }
        }
    }

    val selectionState = rememberNodeSelectionState()
    val nodeUiItems = remember(visibleItems, selectionState.selectedNodeIds) {
        visibleItems.map {
            SelectableNodeItem(it, selectionState.selectedNodeIds.contains(it.id))
        }
    }

    val onItemClicked: (SelectableNodeItem<TypedNode>) -> Unit = { item ->
        when {
            item.isFolderNode -> onFolderClick(item.id)
            isSelectionModeEnabled -> selectionState.toggleSelection(item.id)
        }
    }
    NodeViewWithHeader(
        items = nodeUiItems,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        nodesLoadingState = nodesLoadingState,
        emptyView = {
            if (uiState.isRoot) EmptyRoot()
            else EmptyFolder()
        },
        itemListView = {
            CloudExplorerListViewItem(
                title = it.title.text,
                subtitle = it.subtitle.text(),
                icon = it.iconRes,
                description = it.formattedDescription?.text,
                tags = it.tags,
                thumbnailData = it.thumbnailData,
                isSelected = it.isSelected,
                isInSelectionMode = isSelectionModeEnabled && (it.node is FileNode),
                showIsVerified = it.showIsVerified,
                isTakenDown = it.isTakenDown,
                label = it.nodeLabel,
                isSensitive = it.isSensitive && isHiddenNodesEnabled,
                showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                isHighlighted = it.isHighlighted,
                onItemClicked = { onItemClicked(it) },
                enabled = it.isFolderNode || isSelectionModeEnabled,
            )
        },
        itemGridView = {
            CloudExplorerGridViewItem(
                name = it.title.text,
                iconRes = it.iconRes,
                thumbnailData = it.thumbnailData,
                isTakenDown = it.isTakenDown,
                duration = it.duration,
                isSelected = it.isSelected,
                isInSelectionMode = isSelectionModeEnabled && (it.node is FileNode),
                isFolderNode = it.isFolderNode,
                isVideoNode = it.isVideoNode,
                onClick = { onItemClicked(it) },
                isSensitive = it.isSensitive && isHiddenNodesEnabled,
                showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                isHighlighted = it.isHighlighted,
                label = it.nodeLabel,
                enabled = it.isFolderNode || isSelectionModeEnabled,
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
fun NodesExplorerScreenContentEmptyPreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            NodesExplorerScreenContent(
                uiState = NodesExplorerUiState(),
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                ),
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
fun NodesExplorerScreenContentPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            NodesExplorerScreenContent(
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
                onRefreshNodes = {},
            )
        }
    }
}

internal const val NODES_EXPLORER_VIEW_TAG = "nodes_explorer_view"
internal const val NODES_EXPLORER_EMPTY_VIEW_TAG = "$NODES_EXPLORER_VIEW_TAG:empty_view"