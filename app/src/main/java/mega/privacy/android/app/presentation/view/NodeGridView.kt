package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

/**
 * This method will show [NodeUIItem] in Grid manner based on span
 * @param modifier
 * @param nodeUIItems
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param spanCount
 * @param getThumbnail
 */
@Composable
fun <T : TypedNode> NodeGridView(
    nodeUIItems: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
    getThumbnail: ((handle: Long, onFinished: (file: File?) -> Unit) -> Unit),
    modifier: Modifier = Modifier,
    spanCount: Int = 2,
    showChangeViewType: Boolean = true,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showSortOrder || showChangeViewType) {
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
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
                )
            }
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
                getThumbnail(nodeUIItems[it].node.id.longValue) { file ->
                    value = file
                }
            }
            NodeGridViewItem<T>(
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