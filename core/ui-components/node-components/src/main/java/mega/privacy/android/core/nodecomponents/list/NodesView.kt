package mega.privacy.android.core.nodecomponents.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import mega.privacy.android.core.nodecomponents.dialog.TakeDownDialog
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * NodesView
 *
 * @param items List of [NodeUiItem]
 * @param onMenuClick three dots click
 * @param onItemClicked callback for item click
 * @param onLongClicked callback for long item click
 * @param sortOrder the sort order of the list
 * @param isListView whether the current view is list view
 * @param onSortOrderClick callback for sort order click
 * @param onChangeViewTypeClicked callback for change view type click
 * @param onLinkClicked callback for link click
 * @param onDisputeTakeDownClicked callback for dispute take down click
 * @param modifier
 * @param listState the state of the list
 * @param gridState the state of the grid
 * @param spanCount the span count of the grid
 * @param showHiddenNodes whether to forcefully show hidden nodes
 * @param isHiddenNodesEnabled whether hidden nodes feature is enabled for the user
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
    onChangeViewTypeClicked: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    highlightText: String = "",
    spanCount: Int = 2,
    showHiddenNodes: Boolean = false,
    isHiddenNodesEnabled: Boolean = false,
    showLinkIcon: Boolean = true,
    showChangeViewType: Boolean = true,
    showSortOrder: Boolean = true,
    showMediaDiscoveryButton: Boolean = false,
    isContactVerificationOn: Boolean = false,
    inSelectionMode: Boolean = false,
    onEnterMediaDiscoveryClick: () -> Unit = {},
    listContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var showTakenDownDialog by rememberSaveable { mutableStateOf(false) }
    val orientation = LocalConfiguration.current.orientation
    val span = if (orientation == Configuration.ORIENTATION_PORTRAIT) spanCount else 4
    val visibleItems = rememberNodeItems(
        nodeUIItems = items,
        showHiddenItems = showHiddenNodes,
        isHiddenNodesEnabled = isHiddenNodesEnabled,
    )
    val highlightedIndex = remember(visibleItems) {
        visibleItems.indexOfFirst { it.isHighlighted }
            .takeIf { visibleItems.indices.contains(it) }
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
            nodeUiItemList = visibleItems,
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
            onChangeViewTypeClick = onChangeViewTypeClicked,
            showSortOrder = showSortOrder,
            showChangeViewType = showChangeViewType,
            showLinkIcon = showLinkIcon,
            listState = listState,
            showMediaDiscoveryButton = showMediaDiscoveryButton,
            inSelectionMode = inSelectionMode,
            isContactVerificationOn = isContactVerificationOn,
        )
    } else {
        NodeGridView(
            modifier = modifier,
            listContentPadding = listContentPadding,
            nodeUiItems = visibleItems,
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
            onChangeViewTypeClick = onChangeViewTypeClicked,
            showSortOrder = showSortOrder,
            showChangeViewType = showChangeViewType,
            gridState = gridState,
            showMediaDiscoveryButton = showMediaDiscoveryButton,
            inSelectionMode = inSelectionMode,
        )
    }

    if (showTakenDownDialog) {
        TakeDownDialog(
            isFolder = false,
            onDismiss = {
                showTakenDownDialog = false
            }
        )
    }
}


/**
 * Remember function for node items to handle empty span count and to filter out sensitive nodes
 * @param nodeUIItems list of [NodeUiItem]
 * @param showHiddenItems whether to show hidden items
 * @param isHiddenNodesEnabled whether hidden nodes are enabled
 */
@Composable
internal fun <T : TypedNode> rememberNodeItems(
    nodeUIItems: List<NodeUiItem<T>>,
    showHiddenItems: Boolean,
    isHiddenNodesEnabled: Boolean,
) = remember(showHiddenItems, nodeUIItems.hashCode()) {
    return@remember if (showHiddenItems || !isHiddenNodesEnabled) {
        nodeUIItems
    } else {
        nodeUIItems.filterNot { it.isSensitive }
    }
}

/**
 * Test tag for nodesView visibility
 */
const val NODES_EMPTY_VIEW_VISIBLE = "Nodes empty view not visible"