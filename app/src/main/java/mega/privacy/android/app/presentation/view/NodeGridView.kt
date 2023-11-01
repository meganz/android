package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.fetcher.ThumbnailRequest
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

/**
This method will show [NodeUIItem] in Grid manner based on span and getting thumbnail using [ThumbnailRequest]
 *
 * @param nodeUIItems List of [NodeUIItem]
 * @param onMenuClick three dots click
 * @param onItemClicked on item click
 * @param onLongClick on long item click
 * @param onEnterMediaDiscoveryClick on enter media discovery click
 * @param sortOrder the sort order of the list
 * @param onSortOrderClick on sort order click
 * @param onChangeViewTypeClick on change view type click
 * @param showSortOrder whether to show change sort order button
 * @param gridState the state of the grid
 * @param showMediaDiscoveryButton whether to show media discovery button
 * @param modifier
 * @param spanCount the span count of the grid
 * @param showChangeViewType whether to show change view type button
 */
@Composable
fun <T : TypedNode> NodeGridView(
    nodeUIItems: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
    showMediaDiscoveryButton: Boolean,
    modifier: Modifier = Modifier,
    spanCount: Int = 2,
    showChangeViewType: Boolean = true,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header",
                span = {
                    GridItemSpan(currentLineSpan = spanCount)
                }
            ) {
                HeaderViewItem(
                    modifier = modifier.padding(bottom = 4.dp),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                    sortOrder = sortOrder,
                    isListView = false,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
                    showMediaDiscoveryButton = showMediaDiscoveryButton,
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
            NodeGridViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItems[it],
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
                thumbnailData = ThumbnailRequest(nodeUIItems[it].node.id),
            )
        }
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
 * @param getThumbnail
 */
@Deprecated("Use NodeGridView with ThumbnailRequest instead")
@Composable
fun <T : TypedNode> NodeGridView(
    nodeUIItems: List<NodeUIItem<T>>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
    getThumbnail: ((handle: Long, onFinished: (file: File?) -> Unit) -> Unit),
    showMediaDiscoveryButton: Boolean,
    modifier: Modifier = Modifier,
    spanCount: Int = 2,
    showChangeViewType: Boolean = true,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header",
                span = {
                    GridItemSpan(currentLineSpan = spanCount)
                }
            ) {
                HeaderViewItem(
                    modifier = modifier
                        .padding(bottom = 8.dp),
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                    sortOrder = sortOrder,
                    isListView = false,
                    showSortOrder = showSortOrder,
                    showChangeViewType = showChangeViewType,
                    showMediaDiscoveryButton = showMediaDiscoveryButton,
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
            NodeGridViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItems[it],
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
                imageState = imageState,
            )
        }
    }
}