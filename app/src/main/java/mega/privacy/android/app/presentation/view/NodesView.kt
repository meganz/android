package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId

/**
 * This method will return different type of folder icons based on their type
 * @param nodeUIItem [FolderNode]
 */
@Composable
internal fun getPainter(nodeUIItem: FolderNode): Painter {
    return if (nodeUIItem.isIncomingShare) {
        painterResource(id = R.drawable.ic_folder_incoming)
    } else if (nodeUIItem.isExported) {
        painterResource(id = R.drawable.ic_folder_outgoing)
    } else {
        painterResource(id = R.drawable.ic_folder_list)
    }
}

/**
 * This method will create menu item image
 * @param onMenuClick click listenr of menu item
 */
@Composable
internal fun MenuItem(onMenuClick: () -> Unit) {
    Image(
        painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
        contentDescription = "3 dots",
        modifier = Modifier.clickable { onMenuClick.invoke() }
    )
}

/**
 * This method will show [NodeUIItem] in Grid manner based on span
 * @param modifier
 * @param nodeUIItems
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param spanCount
 */
@Composable
private fun NodeGridView(
    modifier: Modifier,
    nodeUIItems: List<NodeUIItem>,
    onMenuClick: () -> Unit,
    onItemClicked: (NodeId) -> Unit,
    onLongClick: (NodeId) -> Unit,
    spanCount: Int = 2,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(
            key = "header",
            span = {
                GridItemSpan(currentLineSpan = spanCount)
            }
        ) {
            HeaderViewItem(
                modifier = modifier,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                sortOrder = sortOrder,
                isListView = false
            )
        }
        items(count = nodeUIItems.size,
            key = {
                nodeUIItems[it].node.id.longValue
            }) {
            NodeGridViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItems[it],
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
            )
        }
    }
}

/**
 * This method will show [NodeUIItem] in vertical list
 * @param modifier
 * @param nodeUIItemList
 * @param stringUtilWrapper
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param sortOrder
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 */
@Composable
private fun NodeListView(
    modifier: Modifier,
    nodeUIItemList: List<NodeUIItem>,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: () -> Unit,
    onItemClicked: (NodeId) -> Unit,
    onLongClick: (NodeId) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
) {
    LazyColumn {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                modifier = modifier.padding(16.dp),
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                sortOrder = sortOrder,
                isListView = true
            )
        }
        items(count = nodeUIItemList.size,
            key = {
                nodeUIItemList[it].node.id.longValue
            }) {
            NodeListViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItemList[it],
                stringUtilWrapper = stringUtilWrapper,
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick
            )
        }
    }
}

/**
 * /**
 * List/Grid view for file/folder list
 * @param modifier [Modifier]
 * @param nodeUIItems List of [NodeUIItem]
 * @param stringUtilWrapper [StringUtilWrapper] to format Info
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 * @param isListView current view type
 * @param onChangeViewTypeClick changeViewType Click
 * @param onSortOrderClick change sort order click
 * @param sortOrder current sort name
*/
 */
@Composable
fun NodesView(
    modifier: Modifier,
    nodeUIItems: List<NodeUIItem>,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: () -> Unit,
    onItemClicked: (NodeId) -> Unit,
    onLongClick: (NodeId) -> Unit,
    sortOrder: String,
    isListView: Boolean,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    spanCount: Int = 2,
) {
    if (!isListView) {
        NodeListView(
            modifier = modifier,
            nodeUIItemList = nodeUIItems,
            stringUtilWrapper = stringUtilWrapper,
            onMenuClick = onMenuClick,
            onItemClicked = onItemClicked,
            onLongClick = onLongClick,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick
        )
    } else {
        val newList = rememberNodeListForGrid(nodeUIItems = nodeUIItems, spanCount = spanCount)
        NodeGridView(
            modifier = modifier,
            nodeUIItems = newList,
            onMenuClick = onMenuClick,
            onItemClicked = onItemClicked,
            onLongClick = onLongClick,
            spanCount = spanCount,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick
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
private fun rememberNodeListForGrid(nodeUIItems: List<NodeUIItem>, spanCount: Int) =
    remember(key1 = nodeUIItems.size) {
        val folderCount = nodeUIItems.count {
            it.node is FolderNode
        }
        val placeholderCount =
            (folderCount % spanCount).takeIf { it != 0 }?.let { spanCount - it } ?: 0
        if (folderCount > 0 && placeholderCount > 0 && folderCount < nodeUIItems.size) {
            val gridItemList = nodeUIItems.toMutableList()
            repeat(placeholderCount) {
                val node = nodeUIItems[folderCount - 1].copy(
                    isInvisible = true
                )
                gridItemList.add(folderCount + 1, node)
            }
            return@remember gridItemList
        }
        nodeUIItems
    }

