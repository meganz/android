package mega.privacy.android.app.presentation.shares.links.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

/**
 * Composable view for Links screen
 * @param uiState
 * @param emptyState
 * @param onListTopReached
 * @param onItemClick
 * @param onLongClick
 * @param onMenuClick
 * @param sortOrder
 * @param onSortOrderClick
 */
@Composable
fun LinksView(
    uiState: LinksUiState,
    emptyState: Pair<Int, Int>,
    onToggleAppBarElevation: (show: Boolean) -> Unit,
    onItemClick: (NodeUIItem<PublicLinkNode>) -> Unit,
    onLongClick: (NodeUIItem<PublicLinkNode>) -> Unit,
    onMenuClick: (NodeUIItem<PublicLinkNode>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
) {
    val listState = rememberLazyListState()

    val isListAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    LaunchedEffect(isListAtTop, uiState.isInSelection) {
        onToggleAppBarElevation(!uiState.isInSelection && !isListAtTop)
    }

    // reset scroll position when parent node changes (a folder is opened)
    LaunchedEffect(uiState.parentNode) {
        listState.scrollToItem(0)
    }

    val gridState = rememberLazyGridState()

    if (!uiState.isLoading)
        if (uiState.nodesList.isNotEmpty()) {
            NodesView(
                listContentPadding = PaddingValues(top = 17.dp),
                nodeUIItems = uiState.nodesList,
                onMenuClick = onMenuClick,
                onItemClicked = onItemClick,
                onLongClick = onLongClick,
                sortOrder = sortOrder,
                isListView = true,
                showChangeViewType = false,
                showLinkIcon = false,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = { },
                listState = listState,
                gridState = gridState,
                onLinkClicked = { },
                onDisputeTakeDownClicked = { },
                showMediaDiscoveryButton = false,
                onEnterMediaDiscoveryClick = { },
                showPublicLinkCreationTime = uiState.isFirstPage
            )
        } else {
            LegacyMegaEmptyView(
                modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
                imagePainter = painterResource(id = emptyState.first),
                text = stringResource(id = emptyState.second)
            )
        }
}