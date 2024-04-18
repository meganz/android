package mega.privacy.android.app.presentation.shares.outgoing.ui

import androidx.compose.foundation.layout.PaddingValues
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
import mega.privacy.android.app.presentation.search.view.LoadingStateView
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.utils.ListGridStateMap
import mega.privacy.android.core.ui.utils.getState
import mega.privacy.android.core.ui.utils.sync
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

/**
 * Composable view for Outgoing Shares
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
fun OutgoingSharesView(
    uiState: OutgoingSharesState,
    emptyState: Pair<Int, Int>,
    onToggleAppBarElevation: (show: Boolean) -> Unit,
    onItemClick: (NodeUIItem<ShareNode>) -> Unit,
    onLongClick: (NodeUIItem<ShareNode>) -> Unit,
    onMenuClick: (NodeUIItem<ShareNode>) -> Unit,
    sortOrder: String,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onVerifyContactDialogDismissed: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper
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
                fileTypeIconMapper = fileTypeIconMapper,
                inSelectionMode = uiState.isInSelection
            )
        } else {
            LegacyMegaEmptyView(
                modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
                imagePainter = painterResource(id = emptyState.first),
                text = stringResource(id = emptyState.second)
            )
        }
    } else if (uiState.isInRootLevel) {
        LoadingStateView(
            isList = uiState.currentViewType == ViewType.LIST
        )
    }

    if (uiState.verifyContactDialog != null) {
        MegaAlertDialog(
            title = stringResource(id = R.string.shared_items_contact_not_in_contact_list_dialog_title),
            text = stringResource(
                id = R.string.shared_items_contact_not_in_contact_list_dialog_content,
                uiState.verifyContactDialog
            ),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = onVerifyContactDialogDismissed,
            onDismiss = onVerifyContactDialogDismissed
        )
    }
}


