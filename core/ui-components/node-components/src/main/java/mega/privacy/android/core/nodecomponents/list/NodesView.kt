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
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.privacy.android.core.nodecomponents.dialog.TakeDownDialog
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * NodesView
 *
 * @param items List of [NodeUiItem]
 * @param onMenuClicked three dots click
 * @param onItemClicked callback for item click
 * @param onLongClicked callback for long item click
 * @param sortConfiguration the sort order of the list
 * @param isListView whether the current view is list view
 * @param onSortOrderClick callback for sort order click
 * @param onChangeViewTypeClicked callback for change view type click
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
    onMenuClicked: (NodeUiItem<T>) -> Unit,
    onItemClicked: (NodeUiItem<T>) -> Unit,
    onLongClicked: (NodeUiItem<T>) -> Unit,
    sortConfiguration: NodeSortConfiguration,
    isListView: Boolean,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClicked: () -> Unit,
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
            onMenuClick = onMenuClicked,
            onItemClicked = {
                if (it.isTakenDown && it.node !is FolderNode) {
                    showTakenDownDialog = true
                } else {
                    onItemClicked(it)
                }
            },
            onLongClick = onLongClicked,
            onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            sortConfiguration = sortConfiguration,
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
            onMenuClicked = onMenuClicked,
            onItemClicked = {
                if (it.isTakenDown && it.node !is FolderNode) {
                    showTakenDownDialog = true
                } else {
                    onItemClicked(it)
                }
            },
            onLongClicked = onLongClicked,
            onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            spanCount = spanCount,
            sortConfiguration = sortConfiguration,
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
 * Responsive grid span count for nodes view based on device type and orientation.
 *
 * @param defaultSpanCount fallback span count if no custom logic applies
 * @param isListView if true, skips complex calculations and returns default span count
 * @return responsive span count based on device type and orientation
 */
@Composable
fun rememberDynamicSpanCount(
    defaultSpanCount: Int = 2,
    isListView: Boolean = false,
): Int {
    // Early return for list view, calculation is not required
    if (isListView) {
        return defaultSpanCount
    }

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val screenWidthDp = configuration.screenWidthDp

    return remember(orientation, isTablet, screenWidthDp) {
        when {
            // For phone, 2 span in portrait and 4 in landscape
            !isTablet && orientation == Configuration.ORIENTATION_PORTRAIT -> 2
            !isTablet && orientation == Configuration.ORIENTATION_LANDSCAPE -> 4
            // Span count based on tablet screen size
            isTablet -> {
                when {
                    // Large tablets (10+ inch)
                    screenWidthDp >= 840 -> {
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) 5 else 8
                    }
                    // Medium tablets (7-9 inch)
                    screenWidthDp >= 600 -> {
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) 4 else 6
                    }
                    // Small tablets
                    else -> {
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5
                    }
                }
            }

            // Fallback
            else -> defaultSpanCount
        }
    }
}