package mega.privacy.android.app.presentation.shares.incoming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_yellow_700
import mega.privacy.android.core.ui.theme.extensions.yellow_100_yellow_700_alpha_015
import mega.privacy.android.core.ui.utils.ListGridStateMap
import mega.privacy.android.core.ui.utils.getState
import mega.privacy.android.core.ui.utils.sync
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

/**
 * Composable view for Incoming Shares
 * @param uiState
 * @param emptyState
 * @param onToggleAppBarElevation
 * @param onItemClick
 * @param onLongClick
 * @param onMenuClick
 * @param sortOrder
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param onLinkClicked
 */
@Composable
fun IncomingSharesView(
    uiState: IncomingSharesState,
    emptyState: Pair<Int, Int>,
    onToggleAppBarElevation: (show: Boolean) -> Unit,
    onItemClick: (NodeUIItem<ShareNode>) -> Unit,
    onLongClick: (NodeUIItem<ShareNode>) -> Unit,
    onMenuClick: (NodeUIItem<ShareNode>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
) {

    var listStateMap by rememberSaveable(saver = ListGridStateMap.Saver) {
        mutableStateOf(emptyMap())
    }

    /**
     * When back navigation performed from a folder, remove the listState/gridState of that node handle
     */
    LaunchedEffect(uiState.openedFolderNodeHandles, uiState.nodesList, uiState.currentHandle) {
        listStateMap = listStateMap.sync(
            uiState.openedFolderNodeHandles,
            uiState.currentHandle
        )
    }

    val currentListState = listStateMap.getState(uiState.currentHandle)

    val isListAtTop by remember(currentListState) {
        derivedStateOf {
            if (uiState.currentViewType == ViewType.LIST) {
                currentListState.lazyListState.firstVisibleItemIndex == 0
            } else {
                currentListState.lazyGridState.firstVisibleItemIndex == 0
            }
        }
    }

    LaunchedEffect(isListAtTop, uiState.isInSelection, uiState.currentHandle) {
        onToggleAppBarElevation(!uiState.isInSelection && !isListAtTop)
    }

    if (!uiState.isLoading) {
        Column {
            if (uiState.showContactNotVerifiedBanner && !uiState.currentNodeName.isNullOrEmpty()) {
                Text(
                    text = stringResource(
                        id = R.string.contact_incoming_shared_folder_contact_not_approved_alert_text,
                        uiState.currentNodeName
                    ),
                    style = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.grey_alpha_087_yellow_700
                    ),
                    modifier = Modifier
                        .testTag(VERIFICATION_BANNER_TAG)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colors.yellow_100_yellow_700_alpha_015
                        )
                        .padding(16.dp),
                )
            }

            if (uiState.nodesList.isNotEmpty()) {
                NodesView(
                    nodeUIItems = uiState.nodesList,
                    onMenuClick = onMenuClick,
                    onItemClicked = onItemClick,
                    onLongClick = onLongClick,
                    sortOrder = sortOrder,
                    isListView = uiState.currentViewType == ViewType.LIST,
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    listState = currentListState.lazyListState,
                    gridState = currentListState.lazyGridState,
                    onLinkClicked = onLinkClicked,
                    onDisputeTakeDownClicked = { },
                    showMediaDiscoveryButton = false,
                    onEnterMediaDiscoveryClick = { },
                    listContentPadding = PaddingValues(top = 18.dp),
                )
            } else {
                LegacyMegaEmptyView(
                    modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
                    imagePainter = painterResource(id = emptyState.first),
                    text = stringResource(id = emptyState.second)
                )
            }
        }
    }
}

/**
 * Test tag for verification banner view visible
 */
const val VERIFICATION_BANNER_TAG = "incoming_shares_view:text_verification_banner"


