package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.NodeViewWithHeader
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.components.rememberNodeItems
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.nodes.model.text
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
            NodeExplorerListItem(
                item = it,
                isInSelectionMode = isSelectionModeEnabled,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                onItemClicked = onItemClicked
            )
        },
        itemGridView = {
            NodeExplorerGridItem(
                item = it,
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
private fun NodeExplorerListItem(
    item: NodeUiItem<TypedNode>,
    isInSelectionMode: Boolean,
    isHiddenNodesEnabled: Boolean,
    onItemClicked: (NodeUiItem<TypedNode>) -> Unit,
    modifier: Modifier = Modifier,
) = GenericListItem(
    modifier = modifier
        .alpha(if (item.isSensitive && isHiddenNodesEnabled) 0.5f else 1f),
//        .conditional(item.isSelected) {
//            background(BackgroundColor.Surface1)
//        },
    contentPadding = PaddingValues(
        horizontal = 12.dp,
        vertical = 8.dp,
    ),
    leadingElement = {
        NodeThumbnailView(
            modifier = Modifier
                .size(32.dp)
                .testTag(NODES_EXPLORER_VIEW_ITEM_THUMBNAIL_TAG),
            layoutType = ThumbnailLayoutType.List,
            data = item.thumbnailData,
            defaultImage = item.iconRes,
            contentDescription = "Thumbnail",
            blurImage = item.showBlurEffect && isHiddenNodesEnabled,
        )
    },
    title = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            MegaText(
                text = item.title.text,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag(NODES_EXPLORER_VIEW_ITEM_TITLE_TAG),
            )
        }
    },
    subtitle = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MegaText(
                text = item.subtitle.text(),
                textColor = TextColor.Secondary,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag(NODES_EXPLORER_VIEW_ITEM_SUBTITLE_TAG),
            )
        }
    },
    trailingElement = {
        if (isInSelectionMode) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckStateChanged = { },
                    tapTargetArea = false,
                    clickable = false,
                    modifier = Modifier.testTag(NODES_EXPLORER_VIEW_ITEM_CHECKBOX_TAG),
                )
            }
        }
    },
    onClickListener = { onItemClicked(item) },
    enableClick = true,
)

@Composable
private fun NodeExplorerGridItem(
    item: NodeUiItem<TypedNode>,
    isInSelectionMode: Boolean,
    isHiddenNodesEnabled: Boolean,
    onItemClicked: (NodeUiItem<TypedNode>) -> Unit,
    modifier: Modifier = Modifier,
) = with(item) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isSensitive && isHiddenNodesEnabled) 0.5f else 1f)
            .clip(RoundedCornerShape(4.dp))
//            .background(
//                when {
//                    isSelected -> DSTokens.colors.background.surface1
//                    else -> DSTokens.colors.background.pageBackground
//                }
//            )
            .clickable { onItemClicked(item) }
    ) {
        BoxSurface(
            surfaceColor = SurfaceColor.Surface2,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            NodeThumbnailView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag(NODES_EXPLORER_VIEW_ITEM_THUMBNAIL_TAG),
                layoutType = ThumbnailLayoutType.Grid,
                data = thumbnailData,
                defaultImage = iconRes,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                blurImage = showBlurEffect && isSensitive
            )

            if (isVideoNode) {
                BoxSurface(
                    surfaceColor = SurfaceColor.Blur,
                    modifier = Modifier.fillMaxSize()
                ) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Solid.PlayCircle,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .testTag(NODES_EXPLORER_VIEW_ITEM_VIDEO_ICON_TAG),
                        contentDescription = "Play Icon",
                        tint = IconColor.OnColor,
                    )
                }
            }

            duration?.let { duration ->
                BoxSurface(
                    surfaceColor = SurfaceColor.SurfaceTransparent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(shape = RoundedCornerShape(size = 3.dp))
                        .padding(
                            horizontal = 4.dp,
                            vertical = 2.dp,
                        ),
                ) {
                    MegaText(
                        text = duration,
                        style = AppTheme.typography.bodySmall,
                        textColor = TextColor.OnColor,
                        modifier = Modifier
                            .testTag(NODES_EXPLORER_VIEW_ITEM_VIDEO_DURATION_TAG),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        ) {
            Spacer(modifier = Modifier.width(4.dp))

            MegaText(
                text = name,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 2,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .testTag(NODES_EXPLORER_VIEW_ITEM_TITLE_TAG),
            )

            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckStateChanged = {},
                    tapTargetArea = false,
                    clickable = false,
                    modifier = Modifier.testTag(NODES_EXPLORER_VIEW_ITEM_CHECKBOX_TAG)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
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
fun NodesExplorerScreenPreview() {
    AndroidThemeForPreviews {
        NodesExplorerScreen(
            uiState = NodesExplorerUiState(),
            uiStateShared = NodesExplorerSharedUiState(),
            onNavigateBack = {},
            consumeNavigateBack = {},
            onFolderClick = {},
            onFileClick = {},
            onRefreshNodes = {},
        )
    }
}

internal const val NODES_EXPLORER_VIEW_TAG = "nodes_explorer_view"
internal const val NODES_EXPLORER_EMPTY_VIEW_TAG = "${NODES_EXPLORER_VIEW_TAG}:empty_view"
internal const val NODES_EXPLORER_VIEW_ITEM_TAG = "${NODES_EXPLORER_VIEW_TAG}:item"
internal const val NODES_EXPLORER_VIEW_ITEM_THUMBNAIL_TAG =
    "${NODES_EXPLORER_VIEW_ITEM_TAG}:thumbnail"
internal const val NODES_EXPLORER_VIEW_ITEM_TITLE_TAG =
    "${NODES_EXPLORER_VIEW_ITEM_TAG}:title"
internal const val NODES_EXPLORER_VIEW_ITEM_SUBTITLE_TAG =
    "${NODES_EXPLORER_VIEW_ITEM_TAG}:subtitle"
internal const val NODES_EXPLORER_VIEW_ITEM_CHECKBOX_TAG =
    "${NODES_EXPLORER_VIEW_ITEM_TAG}:checkbox"
internal const val NODES_EXPLORER_VIEW_ITEM_VIDEO_ICON_TAG =
    "${NODES_EXPLORER_VIEW_ITEM_TAG}:video_icon"
internal const val NODES_EXPLORER_VIEW_ITEM_VIDEO_DURATION_TAG =
    "${NODES_EXPLORER_VIEW_ITEM_TAG}:video_duration"