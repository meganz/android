package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

/**
 * This method will show [NodeUIItem] in vertical list
 * @param modifier
 * @param nodeUIItemList
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param sortOrder
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param getThumbnail
 */
@Composable
fun <T : TypedNode> NodeListView(
    nodeUIItemList: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    listState: LazyListState,
    getThumbnail: ((handle: Long, onFinished: (file: File?) -> Unit) -> Unit),
    modifier: Modifier = Modifier,
    showChangeViewType: Boolean = true,
) {
    LazyColumn(state = listState, modifier = modifier) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header"
            ) {
                HeaderViewItem(
                    modifier = modifier,
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    sortOrder = sortOrder,
                    isListView = true,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType
                )
            }
        }
        items(count = nodeUIItemList.size,
            key = {
                nodeUIItemList[it].node.id.longValue
            }) {
            val imageState = produceState<File?>(initialValue = null) {
                getThumbnail(nodeUIItemList[it].node.id.longValue) { file ->
                    value = file
                }
            }
            NodeListViewItem<T>(
                modifier = modifier,
                nodeUIItem = nodeUIItemList[it],
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
                imageState = imageState
            )
        }
    }
}