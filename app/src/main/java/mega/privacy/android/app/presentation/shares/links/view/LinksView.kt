package mega.privacy.android.app.presentation.shares.links.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.utils.ListStateMap
import mega.privacy.android.core.ui.utils.getState
import mega.privacy.android.core.ui.utils.sync
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
    var listStateMap by rememberSaveable(saver = ListStateMap.Saver) {
        mutableStateOf(emptyMap())
    }

    /**
     * When back navigation performed from a folder, remove the listState of that node handle
     */
    LaunchedEffect(
        uiState.openedFolderNodeHandles,
        uiState.nodesList,
    ) {
        listStateMap = listStateMap.sync(
            uiState.openedFolderNodeHandles,
            uiState.currentFolderNodeHandle
        )
    }

    val currentListState = listStateMap.getState(uiState.currentFolderNodeHandle)

    val isListAtTop by remember(currentListState) {
        derivedStateOf {
            currentListState.firstVisibleItemIndex == 0
        }
    }

    LaunchedEffect(isListAtTop, uiState.isInSelection, uiState.currentFolderNodeHandle) {
        onToggleAppBarElevation(!uiState.isInSelection && !isListAtTop)
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
                listState = currentListState,
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