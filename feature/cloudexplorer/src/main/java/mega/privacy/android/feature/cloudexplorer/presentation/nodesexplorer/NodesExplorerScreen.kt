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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeGridViewItemSkeleton
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.components.NodeListViewItemSkeleton
import mega.privacy.android.shared.nodes.components.NodeSkeletons
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.components.rememberDynamicSpanCount
import mega.privacy.android.shared.nodes.components.rememberNodeItems
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun NodesExplorerScreen(
    viewModel: NodesExplorerViewModel,
    isTabContent: Boolean,
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
        isTabContent = isTabContent,
        onFolderClick = onNavigateToFolder,
        onFileClick = viewModel::fileClicked,
        onViewTypeClick = viewModel::updateViewType,
        onSortNodes = viewModel::updateNodeSortConfiguration,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodesExplorerScreen(
    isTabContent: Boolean,
    uiState: NodesExplorerUiState,
    uiStateShared: NodesExplorerSharedUiState,
    onNavigateBack: () -> Unit,
    consumeNavigateBack: () -> Unit,
    onFolderClick: (NodeId) -> Unit,
    onFileClick: (NodeUiItem<TypedNode>) -> Unit,
    onViewTypeClick: () -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) = with(uiStateShared) {
    val isListView = viewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val topPadding = if (isTabContent) 12.dp else 0.dp

    EventEffect(
        event = navigateBack,
        onConsumed = consumeNavigateBack,
    ) { onNavigateBack() }

    Column(
        modifier = modifier
            .padding(contentPadding().excludingBottomPadding())
    ) {
        when {
            isLoading -> NodesViewSkeleton(
                isListView = isListView,
                spanCount = spanCount,
                contentPadding = PaddingValues(top = topPadding),
                delay = NodeSkeletons.defaultDelay,
            )

            items.isEmpty() -> if (uiState.isRoot) {
                EmptyRoot()
            } else {
                EmptyFolder()
            }

            else -> {
                val isNextPageLoading = nodesLoadingState == NodesLoadingState.PartiallyLoaded
                val visibleItems = rememberNodeItems(
                    nodeUIItems = items,
                    showHiddenItems = showHiddenNodes,
                    isHiddenNodesEnabled = isHiddenNodesEnabled,
                )

                if (isListView) {
                    NodesExplorerListView(
                        items = visibleItems,
                        isNextPageLoading = isNextPageLoading,
                        nodeSortConfiguration = nodeSortConfiguration,
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        onFolderClick = onFolderClick,
                        onFileClick = onFileClick,
                        onViewTypeClick = onViewTypeClick,
                        onSortOrderClick = { showSortBottomSheet = true },
                    )
                } else {
                    NodesExplorerGridView(
                        items = visibleItems,
                        isNextPageLoading = isNextPageLoading,
                        nodeSortConfiguration = nodeSortConfiguration,
                        isSelectionModeEnabled = isSelectionModeEnabled,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        spanCount = spanCount,
                        onFolderClick = onFolderClick,
                        onFileClick = onFileClick,
                        onViewTypeClick = onViewTypeClick,
                        onSortOrderClick = { showSortBottomSheet = true },
                    )
                }
            }
        }

        if (showSortBottomSheet) {
            SortBottomSheet(
                title = stringResource(sharedR.string.action_sort_by_header),
                options = NodeSortOption.getOptionsForSourceType(nodeSourceType),
                sheetState = sortBottomSheetState,
                selectedSort = SortBottomSheetResult(
                    sortOptionItem = nodeSortConfiguration.sortOption,
                    sortDirection = nodeSortConfiguration.sortDirection
                ),
                onSortOptionSelected = { result ->
                    result?.let {
                        onSortNodes(
                            NodeSortConfiguration(
                                sortOption = it.sortOptionItem,
                                sortDirection = it.sortDirection
                            )
                        )
                        showSortBottomSheet = false
                    }
                },
                onDismissRequest = {
                    showSortBottomSheet = false
                }
            )
        }
    }
}

@Composable
internal fun NodesExplorerListView(
    items: List<NodeUiItem<TypedNode>>,
    isNextPageLoading: Boolean,
    nodeSortConfiguration: NodeSortConfiguration,
    isSelectionModeEnabled: Boolean,
    isHiddenNodesEnabled: Boolean,
    onFolderClick: (NodeId) -> Unit,
    onFileClick: (NodeUiItem<TypedNode>) -> Unit,
    onViewTypeClick: () -> Unit,
    onSortOrderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    FastScrollLazyColumn(
        state = listState,
        totalItems = items.size,
        modifier = modifier.semantics { testTagsAsResourceId = true },
    ) {
        item(key = "header") {
            NodeHeaderItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onViewTypeClick,
                onEnterMediaDiscoveryClick = { },
                sortConfiguration = nodeSortConfiguration,
                isListView = true,
                showSortOrder = true,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
            )
        }

        items(
            count = items.size,
            key = { items[it].id.longValue }
        ) {
            NodeExplorerListItem(
                item = items[it],
                isInSelectionMode = isSelectionModeEnabled,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                onItemClicked = { item ->
                    when {
                        item.isFolderNode -> onFolderClick(item.id)
                        isSelectionModeEnabled -> onFileClick(item)
                    }
                },
            )
        }

        if (isNextPageLoading) {
            items(count = 5, key = { "loading_$it" }) {
                NodeListViewItemSkeleton()
            }
        }
    }
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
internal fun NodesExplorerGridView(
    items: List<NodeUiItem<TypedNode>>,
    isNextPageLoading: Boolean,
    nodeSortConfiguration: NodeSortConfiguration,
    isSelectionModeEnabled: Boolean,
    isHiddenNodesEnabled: Boolean,
    spanCount: Int,
    onFolderClick: (NodeId) -> Unit,
    onFileClick: (NodeUiItem<TypedNode>) -> Unit,
    onViewTypeClick: () -> Unit,
    onSortOrderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    FastScrollLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        totalItems = items.size,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .semantics { testTagsAsResourceId = true },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        item(
            key = "header",
            span = { GridItemSpan(currentLineSpan = spanCount) }
        ) {
            NodeHeaderItem(
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onViewTypeClick,
                onEnterMediaDiscoveryClick = { },
                sortConfiguration = nodeSortConfiguration,
                isListView = false,
                showSortOrder = true,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }
        items(
            count = items.size,
            key = {
                items[it].id.longValue
            },
        ) {
            NodeExplorerGridItem(
                item = items[it],
                isInSelectionMode = isSelectionModeEnabled,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                onItemClicked = { item ->
                    when {
                        item.isFolderNode -> onFolderClick(item.id)
                        isSelectionModeEnabled -> onFileClick(item)
                    }
                },
            )
        }
        if (isNextPageLoading) {
            items(count = 4, key = { "loading_$it" }) {
                NodeGridViewItemSkeleton()
            }
        }
    }
}

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
            isTabContent = false,
            uiState = NodesExplorerUiState(),
            uiStateShared = NodesExplorerSharedUiState(),
            onNavigateBack = {},
            consumeNavigateBack = {},
            onFolderClick = {},
            onFileClick = {},
            onViewTypeClick = {},
            onSortNodes = {},
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