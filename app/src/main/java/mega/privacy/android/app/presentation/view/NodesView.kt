package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
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

/**
 * Test tag for info text
 */
const val INFO_TEXT_TEST_TAG = "Info Text"

/**
 * Text tag for selected item
 */
const val SELECTED_TEST_TAG = "Selected Tag"

/**
 * Test tag for folder item
 */
const val FOLDER_TEST_TAG = "Folder Tag"

/**
 * Test tag for file item
 */
const val FILE_TEST_TAG = "File Tag"

/**
 * Test tag for favorite item
 */
const val FAVORITE_TEST_TAG = "favorite Tag"

/**
 * Test tag for exported item
 */
const val EXPORTED_TEST_TAG = "exported Tag"

/**
 * Test tag for taken item
 */
const val TAKEN_TEST_TAG = "taken Tag"

/**
 * Test tag for nodesView visibility
 */
const val NODES_EMPTY_VIEW_VISIBLE = "Nodes empty view not visible"

/**
 * This method will return different type of folder icons based on their type
 * @param nodeUIItem [FolderNode]
 */
@Composable
internal fun getPainter(nodeUIItem: FolderNode): Painter {
    return if (nodeUIItem.isIncomingShare) {
        painterResource(id = R.drawable.ic_folder_incoming)
    } else if (nodeUIItem.isShared || nodeUIItem.isPendingShare) {
        painterResource(id = R.drawable.ic_folder_outgoing)
    } else {
        painterResource(id = R.drawable.ic_folder_list)
    }
}

/**
 * This method will create menu item image
 * @param onMenuClick click listener of menu item
 * @param nodeUIItem Node on which click has been performed
 */
@Composable
internal fun MenuItem(onMenuClick: (NodeUIItem) -> Unit, nodeUIItem: NodeUIItem) {
    Image(
        painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
        contentDescription = "3 dots",
        modifier = Modifier.clickable { onMenuClick.invoke(nodeUIItem) }
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
    onMenuClick: (NodeUIItem) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
    spanCount: Int = 2,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                isListView = false,
                showSortOrder
            )
        }
        items(count = nodeUIItems.size,
            key = {
                if (nodeUIItems[it].isInvisible) {
                    it
                } else {
                    nodeUIItems[it].node.id.longValue
                }
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
    onMenuClick: (NodeUIItem) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
) {
    LazyColumn(state = listState, modifier = modifier) {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                modifier = modifier,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = showSortOrder
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
    onMenuClick: (NodeUIItem) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
    sortOrder: String,
    isListView: Boolean,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    spanCount: Int = 2,
    showSortOrder: Boolean = true,
    listState: LazyListState = LazyListState(),
    gridState: LazyGridState = LazyGridState(),
) {
    if (isListView) {
        NodeListView(
            modifier = modifier,
            nodeUIItemList = nodeUIItems,
            stringUtilWrapper = stringUtilWrapper,
            onMenuClick = onMenuClick,
            onItemClicked = onItemClicked,
            onLongClick = onLongClick,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            listState = listState
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
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            gridState = gridState
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
    remember(key1 = nodeUIItems.count { it.isSelected } + nodeUIItems.size) {
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