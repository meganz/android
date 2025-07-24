package mega.privacy.android.core.nodecomponents.list.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * NEW Nodes view to load thumbnails using [ThumbnailRequest]
 *
 * @param items List of [NodeUiItem]
 * @param onMenuClick three dots click
 * @param onItemClicked callback for item click
 * @param onLongClicked callback for long item click
 * @param sortOrder the sort order of the list
 * @param isListView whether the current view is list view
 * @param onSortOrderClick callback for sort order click
 * @param onChangeViewTypeClick callback for change view type click
 * @param onLinkClicked callback for link click
 * @param onDisputeTakeDownClicked callback for dispute take down click
 * @param modifier
 * @param listState the state of the list
 * @param gridState the state of the grid
 * @param spanCount the span count of the grid
 * @param showSortOrder whether to show change sort order button
 * @param showLinkIcon whether to show public share link icon
 * @param showChangeViewType whether to show change view type button
 * @param showMediaDiscoveryButton whether to show media discovery button
 * @param onEnterMediaDiscoveryClick callback for enter media discovery click
 * @param listContentPadding the content padding of the list/lazyColumn
 * @param isContactVerificationOn whether contact verification is enabled
 */
@Composable
fun <T : TypedNode> NodesView(
    items: List<NodeUiItem<T>>,
    onMenuClick: (NodeUiItem<T>) -> Unit,
    onItemClicked: (NodeUiItem<T>) -> Unit,
    onLongClicked: (NodeUiItem<T>) -> Unit,
    sortOrder: String,
    isListView: Boolean,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
    listState: LazyListState = LazyListState(),
    gridState: LazyGridState = LazyGridState(),
    highlightText: String = "",
    spanCount: Int = 2,
    showLinkIcon: Boolean = true,
    showChangeViewType: Boolean = true,
    shouldApplySensitiveMode: Boolean = false,
    showSortOrder: Boolean = true,
    showMediaDiscoveryButton: Boolean = false,
    showPublicLinkCreationTime: Boolean = false,
    isPublicNode: Boolean = false,
    isContactVerificationOn: Boolean = false,
    inSelectionMode: Boolean = false,
    onEnterMediaDiscoveryClick: () -> Unit = {},
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) {
    var showTakenDownDialog by rememberSaveable { mutableStateOf(false) }
    val orientation = LocalConfiguration.current.orientation
    val span = if (orientation == Configuration.ORIENTATION_PORTRAIT) spanCount else 4
    val highlightedIndex = remember(items) {
        items.indexOfFirst { it.isHighlighted }
            .takeIf { items.indices.contains(it) }
    }
    LaunchedEffect(highlightedIndex) {
        highlightedIndex?.let {
            listState.animateScrollToItem(
                index = highlightedIndex.plus(2).coerceAtMost(items.lastIndex),
                scrollOffset = -(listState.layoutInfo.viewportSize.height / 2)
            )
        }
    }
    if (isListView) {
        NodeListView(
            modifier = modifier,
            listContentPadding = listContentPadding,
            nodeUiItemList = items,
            onMenuClick = onMenuClick,
            onItemClicked = {
                if (it.isTakenDown && it.node !is FolderNode) {
                    showTakenDownDialog = true
                } else {
                    onItemClicked(it)
                }
            },
            onLongClick = onLongClicked,
            onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            sortOrder = sortOrder,
            highlightText = highlightText,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            showChangeViewType = showChangeViewType,
            showLinkIcon = showLinkIcon,
            listState = listState,
            showMediaDiscoveryButton = showMediaDiscoveryButton,
            isPublicNode = isPublicNode,
            showPublicLinkCreationTime = showPublicLinkCreationTime,
            fileTypeIconMapper = fileTypeIconMapper,
            inSelectionMode = inSelectionMode,
            shouldApplySensitiveMode = shouldApplySensitiveMode,
            isContactVerificationOn = isContactVerificationOn,
            nodeSourceType = nodeSourceType,
        )
    } else {
        val newList = rememberNodeListForGrid(nodeUIItems = items, spanCount = span)
        NodeGridView(
            modifier = modifier,
            listContentPadding = listContentPadding,
            nodeUiItems = newList,
            onMenuClick = onMenuClick,
            onItemClicked = {
                if (it.isTakenDown && it.node !is FolderNode) {
                    showTakenDownDialog = true
                } else {
                    onItemClicked(it)
                }
            },
            onLongClick = onLongClicked,
            onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            spanCount = span,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            showChangeViewType = showChangeViewType,
            gridState = gridState,
            showMediaDiscoveryButton = showMediaDiscoveryButton,
            isPublicNode = isPublicNode,
            fileTypeIconMapper = fileTypeIconMapper,
            inSelectionMode = inSelectionMode,
            shouldApplySensitiveMode = shouldApplySensitiveMode,
            nodeSourceType = nodeSourceType,
        )
    }
    if (showTakenDownDialog) {
        // TODO
//        TakeDownDialog(
//            isFolder = false,
//            onConfirm = {
//                showTakenDownDialog = false
//            },
//            onDeny = {
//                showTakenDownDialog = false
//                onDisputeTakeDownClicked.invoke(Constants.DISPUTE_URL)
//            },
//            onLinkClick = {
//                onLinkClicked(it)
//            }
//        )
    }
}

/**
 * Remember function for [NodeGridView] to form empty items in case of folders count are not as per
 * span count
 * @param nodeUIItems list of [NodeUiItem]
 * @param spanCount span count of [NodeGridView]
 */
@Composable
private fun <T : TypedNode> rememberNodeListForGrid(
    nodeUIItems: List<NodeUiItem<T>>,
    spanCount: Int,
) = remember(spanCount + nodeUIItems.hashCode()) {
    val folderCount = nodeUIItems.count {
        it.node is FolderNode
    }
    val placeholderCount =
        (folderCount % spanCount).takeIf { it != 0 }?.let { spanCount - it } ?: 0
    if (folderCount > 0 && placeholderCount > 0 && folderCount < nodeUIItems.size) {
        val gridItemList = nodeUIItems.toMutableList()
        repeat(placeholderCount) {
            val node = nodeUIItems[folderCount - 1].copy(
                isInvisible = true,
            )
            gridItemList.add(folderCount, node)
        }
        return@remember gridItemList
    }
    nodeUIItems
}

/**
 * Test tag for nodesView visibility
 */
const val NODES_EMPTY_VIEW_VISIBLE = "Nodes empty view not visible"