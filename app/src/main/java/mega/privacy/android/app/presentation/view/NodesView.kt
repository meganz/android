package mega.privacy.android.app.presentation.view

import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * NEW Nodes view to load thumbnails using [ThumbnailRequest]
 *
 * @param nodeUIItems List of [NodeUIItem]
 * @param onMenuClick three dots click
 * @param onItemClicked callback for item click
 * @param onLongClick callback for long item click
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
 * @param showMediaDiscoveryButton whether to show media discovery button
 * @param onEnterMediaDiscoveryClick callback for enter media discovery click
 */
@Composable
fun <T : TypedNode> NodesView(
    nodeUIItems: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    isListView: Boolean,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = LazyListState(),
    gridState: LazyGridState = LazyGridState(),
    spanCount: Int = 2,
    showSortOrder: Boolean = true,
    showMediaDiscoveryButton: Boolean = false,
    isPublicNode: Boolean = false,
    onEnterMediaDiscoveryClick: () -> Unit = {},
) {
    val takenDownDialog = remember { mutableStateOf(Pair(false, false)) }
    val orientation = LocalConfiguration.current.orientation
    val span = if (orientation == Configuration.ORIENTATION_PORTRAIT) spanCount else 4
    if (isListView) {
        NodeListView(
            modifier = modifier,
            nodeUIItemList = nodeUIItems,
            onMenuClick = onMenuClick,
            onItemClicked = {
                if (it.isTakenDown) {
                    takenDownDialog.value = Pair(true, it.node is FolderNode)
                } else {
                    onItemClicked(it)
                }
            },
            onLongClick = onLongClick,
            onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            listState = listState,
            showMediaDiscoveryButton = showMediaDiscoveryButton,
            isPublicNode = isPublicNode
        )
    } else {
        val newList = rememberNodeListForGrid(nodeUIItems = nodeUIItems, spanCount = span)
        NodeGridView(
            modifier = modifier,
            nodeUIItems = newList,
            onMenuClick = onMenuClick,
            onItemClicked = {
                if (it.isTakenDown) {
                    takenDownDialog.value = Pair(true, it.node is FolderNode)
                } else {
                    onItemClicked(it)
                }
            },
            onLongClick = onLongClick,
            onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            spanCount = span,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            gridState = gridState,
            showMediaDiscoveryButton = showMediaDiscoveryButton,
            isPublicNode = isPublicNode
        )
    }
    if (takenDownDialog.value.first) {
        TakeDownDialog(
            isFolder = takenDownDialog.value.second, onConfirm = {
                takenDownDialog.value = Pair(false, false)
            }, onDeny = {
                takenDownDialog.value = Pair(false, false)
                onDisputeTakeDownClicked.invoke(Constants.DISPUTE_URL)
            }, onLinkClick = {
                onLinkClicked(it)
            }
        )
    }
}

/**
 * Remember function for [NodeGridView] to form empty items in case of folders count are not as per
 * span count
 * @param nodeUIItems list of [NodeUIItem]
 * @param spanCount span count of [NodeGridView]
 */
@Composable
private fun <T : TypedNode> rememberNodeListForGrid(
    nodeUIItems: List<NodeUIItem<T>>,
    spanCount: Int,
) =
    remember(key1 = nodeUIItems.count { it.isSelected } + nodeUIItems.size + spanCount) {
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