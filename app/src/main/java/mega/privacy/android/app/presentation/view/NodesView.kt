package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.ThumbnailViewModel
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.FolderNode
import java.io.File

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
 * This method will show [NodeUIItem] in Grid manner based on span
 * @param modifier
 * @param nodeUIItems
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param spanCount
 * @param thumbnailViewModel
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
    thumbnailViewModel: ThumbnailViewModel?
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
            val imageState = produceState<File?>(initialValue = null) {
                thumbnailViewModel?.getThumbnail(handle = nodeUIItems[it].node.id.longValue) { file ->
                    value = file
                }
            }
            NodeGridViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItems[it],
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
                imageState = imageState
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
 * @param thumbnailViewModel
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
    thumbnailViewModel: ThumbnailViewModel?
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
            val imageState = produceState<File?>(initialValue = null) {
                thumbnailViewModel?.getThumbnail(handle = nodeUIItemList[it].node.id.longValue) { file ->
                    value = file
                }
            }
            NodeListViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItemList[it],
                stringUtilWrapper = stringUtilWrapper,
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
                imageState = imageState
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
 * @param onLinkClicked
 * @param onDisputeTakeDownClicked
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
    thumbnailViewModel: ThumbnailViewModel?,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit
) {
    val takenDownDialog = remember { mutableStateOf(Pair(false, false)) }
    if (isListView) {
        NodeListView(
            modifier = modifier,
            nodeUIItemList = nodeUIItems,
            stringUtilWrapper = stringUtilWrapper,
            onMenuClick = onMenuClick,
            onItemClicked = {
                if (it.isTakenDown) {
                    takenDownDialog.value = Pair(true, it.node is FolderNode)
                } else {
                    onItemClicked(it)
                }
            },
            onLongClick = onLongClick,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            listState = listState,
            thumbnailViewModel = thumbnailViewModel,
        )
    } else {
        val newList = rememberNodeListForGrid(nodeUIItems = nodeUIItems, spanCount = spanCount)
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
            spanCount = spanCount,
            sortOrder = sortOrder,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            showSortOrder = showSortOrder,
            gridState = gridState,
            thumbnailViewModel = thumbnailViewModel
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

/**
 * Thumbnail View for NodesView
 * @param modifier [Modifier]
 * @param imageFile File
 * @param contentDescription Content Description for image,
 * @param node [NodeUIItem]
 * @param contentScale [ContentScale]
 */
@Composable
fun ThumbnailView(
    modifier: Modifier,
    contentDescription: String?,
    imageFile: File?,
    node: NodeUIItem,
    contentScale: ContentScale = ContentScale.Fit,
) {
    imageFile?.let {
        Image(
            modifier = modifier
                .aspectRatio(1f),
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(it)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(id = R.drawable.ic_image_thumbnail),
                error = painterResource(id = R.drawable.ic_image_thumbnail),
            ),
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    } ?: run {
        Image(
            modifier = modifier,
            painter = painterResource(id = MimeTypeList.typeForName(node.name).iconResourceId),
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    }
}