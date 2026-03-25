package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.hasWritePermission
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerGridViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.components.CloudExplorerListViewItem
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NODES_EXPLORER_EMPTY_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerSharedUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewIncomingShareFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun IncomingSharesExplorerScreen(
    viewModel: IncomingSharesExplorerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
) {
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()

    IncomingSharesExplorerContent(
        uiStateShared = uiStateShared,
        onNavigateBack = onNavigateBack,
        consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
        onFolderClick = onNavigateToFolder,
        onFileClick = viewModel::fileClicked,
        onRefreshNodes = viewModel::refreshNodes,
    )
}

@Composable
internal fun IncomingSharesExplorerContent(
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

    val visibleItems = remember(showHiddenNodes, items) {
        return@remember if (showHiddenNodes || !isHiddenNodesEnabled) {
            items
        } else {
            items.filterNot { it.isSensitive }
        }
    }
    val onItemClicked: (NodeUiItem<TypedNode>) -> Unit = { item ->
        when {
            item.isFolderNode -> onFolderClick(item.id)
            isSelectionModeEnabled -> onFileClick(item)
        }
    }
    NodeViewWithHeader(
        items = visibleItems,
        nodeSourceType = uiStateShared.nodeSourceType,
        nodesLoadingState = nodesLoadingState,
        emptyView = {
            EmptyFolder()
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
                isInSelectionMode = isSelectionModeEnabled && it.node is FileNode,
                isTakenDown = it.isTakenDown,
                showIsVerified = it.showIsVerified,
                label = it.nodeLabel,
                isSensitive = it.isSensitive && isHiddenNodesEnabled,
                showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                isHighlighted = it.isHighlighted,
                onItemClicked = { onItemClicked(it) },
                enabled = isSelectionModeEnabled || (it.node as? ShareFolderNode)?.shareData?.access?.hasWritePermission() == true,
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
                isInSelectionMode = isSelectionModeEnabled && it.node is FileNode,
                isFolderNode = it.isFolderNode,
                isVideoNode = it.isVideoNode,
                onClick = { onItemClicked(it) },
                isSensitive = it.isSensitive && isHiddenNodesEnabled,
                showBlurEffect = it.showBlurEffect && isHiddenNodesEnabled,
                isHighlighted = it.isHighlighted,
                label = it.nodeLabel,
                enabled = isSelectionModeEnabled || (it.node as? ShareFolderNode)?.shareData?.access?.hasWritePermission() == true,
            )
        },
        onRefreshNodes = onRefreshNodes,
        modifier = modifier,
    )
}

@Composable
private fun EmptyFolder() {
    MegaEmptyView(
        text = stringResource(sharedR.string.shares_screen_incoming_empty),
        imagePainter = painterResource(iconPackR.drawable.ic_folder_arrow_up_glass),
        modifier = Modifier.testTag(NODES_EXPLORER_EMPTY_VIEW_TAG),
    )
}

@CombinedThemePreviews
@Composable
private fun EmptyFolderPreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            IncomingSharesExplorerContent(
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
fun IncomingSharesExplorerFilePickerScreenPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            IncomingSharesExplorerContent(
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                    isSelectionModeEnabled = true,
                    items = previewFolders()
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
fun IncomingSharesExplorerFolderDestinationScreenPreview(
    @PreviewParameter(BooleanProvider::class) isList: Boolean,
) {
    AndroidThemeForPreviews {
        CompositionLocalProvider(
            LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                viewType = if (isList) ViewType.LIST else ViewType.GRID,
                nodeSortConfiguration = NodeSortConfiguration.default,
            ),
        ) {
            IncomingSharesExplorerContent(
                uiStateShared = NodesExplorerSharedUiState(
                    nodesLoadingState = NodesLoadingState.FullyLoaded,
                    isSelectionModeEnabled = false,
                    items = previewFolders()
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

private fun previewFolders() = (1..15L).map { id ->
    previewIncomingShareFolderNodeUiItem(
        id = id,
        access = if (id % 3 == 0L) AccessPermission.READ else AccessPermission.READWRITE,
        user = "User $id",
        userFullName = "Full name",
    )
}
