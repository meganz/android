package mega.privacy.android.app.presentation.clouddrive.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.app.presentation.view.OverQuotaView
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

/**
 * Composable view for FileBrowser
 * @param uiState
 * @param emptyState
 * @param onItemClick
 * @param onLongClick
 * @param onMenuClick
 * @param sortOrder
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param onDisputeTakeDownClicked
 * @param onLinkClicked
 */
@Composable
fun FileBrowserComposeView(
    uiState: FileBrowserState,
    emptyState: Pair<Int, Int>,
    onItemClick: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    onUpgradeClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    if (uiState.nodesList.isNotEmpty()) {
        Column {
            OverQuotaView(
                bannerTime = uiState.bannerTime,
                shouldShowBannerVisibility = uiState.shouldShowBannerVisibility,
                onUpgradeClicked = onUpgradeClicked,
                onDismissClicked = onDismissClicked
            )

            NodesView(
                nodeUIItems = uiState.nodesList,
                onMenuClick = onMenuClick,
                onItemClicked = onItemClick,
                onLongClick = onLongClick,
                sortOrder = sortOrder,
                isListView = uiState.currentViewType == ViewType.LIST,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                listState = listState,
                gridState = gridState,
                onLinkClicked = onLinkClicked,
                onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                showMediaDiscoveryButton = uiState.showMediaDiscoveryIcon,
                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
            )
        }
    } else {
        LegacyMegaEmptyView(
            modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
            imagePainter = painterResource(id = emptyState.first),
            text = stringResource(id = emptyState.second)
        )
    }
}