package mega.privacy.android.core.nodecomponents.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * NodeGridView
 *
 * @param nodeUiItems List of [NodeUiItem]
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
 * @param highlightText the text to highlight in the grid items
 * @param spanCount the span count of the grid
 * @param showChangeViewType whether to show change view type button
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : TypedNode> NodeGridView(
    nodeUiItems: List<NodeUiItem<T>>,
    onMenuClicked: (NodeUiItem<T>) -> Unit,
    onItemClicked: (NodeUiItem<T>) -> Unit,
    onLongClicked: (NodeUiItem<T>) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    showSortOrder: Boolean,
    gridState: LazyGridState,
    showMediaDiscoveryButton: Boolean,
    modifier: Modifier = Modifier,
    highlightText: String = "",
    spanCount: Int = 2,
    showChangeViewType: Boolean = true,
    inSelectionMode: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    FastScrollLazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(spanCount),
        totalItems = nodeUiItems.size,
        modifier = modifier
            .padding(horizontal = DSTokens.spacings.s3)
            .semantics { testTagsAsResourceId = true },
        horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
        verticalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
        contentPadding = listContentPadding
    ) {
        if (showSortOrder || showChangeViewType) {
            item(
                key = "header",
                span = {
                    GridItemSpan(currentLineSpan = spanCount)
                }
            ) {
                NodeHeaderItem(
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
        items(
            count = nodeUiItems.size,
            key = {
                nodeUiItems[it].id.longValue
            },
        ) {
            NodeGridViewItem(
                nodeUiItem = nodeUiItems[it],
                isInSelectionMode = inSelectionMode,
                highlightText = highlightText,
                onItemClicked = onItemClicked,
                onLongClicked = onLongClicked,
                onMenuClicked = onMenuClicked,
            )
        }
    }
}
