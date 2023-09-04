package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * View for Search compose
 * @param state [SearchActivityState]
 * @param sortOrder String
 * @param onItemClick item click listener
 * @param onLongClick item long click listener
 * @param onMenuClick item menu click listener
 * @param onSortOrderClick Change sort order click listener
 * @param onChangeViewTypeClick change view type click listener
 * @param onLinkClicked link click listener for item
 * @param onDisputeTakeDownClicked dispute take-down click listener
 */
@Composable
fun SearchComposeView(
    state: SearchActivityState,
    sortOrder: String,
    onItemClick: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    if (state.searchItemList.isNotEmpty()) {
        NodesView(
            nodeUIItems = state.searchItemList,
            onMenuClick = onMenuClick,
            onItemClicked = onItemClick,
            onLongClick = onLongClick,
            sortOrder = sortOrder,
            isListView = state.currentViewType == ViewType.LIST,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            onLinkClicked = onLinkClicked,
            onDisputeTakeDownClicked = onDisputeTakeDownClicked,
            listState = listState,
            gridState = gridState,
        )
    }
}
