package mega.privacy.android.app.presentation.clouddrive.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.controls.MegaEmptyView
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Composable view for FileBrowser
 * @param uiState
 * @param stringUtilWrapper
 * @param emptyState
 * @param onItemClick
 * @param onLongClick
 */
@Composable
fun FileBrowserComposeView(
    uiState: FileBrowserState,
    stringUtilWrapper: StringUtilWrapper,
    emptyState: Pair<Int, Int>,
    onItemClick: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
) {
    if (uiState.nodesList.isNotEmpty()) {
        NodesView(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            nodeUIItems = uiState.nodesList,
            stringUtilWrapper = stringUtilWrapper,
            onMenuClick = {},
            onItemClicked = onItemClick,
            onLongClick = onLongClick,
            sortOrder = "",
            isListView = uiState.currentViewType == ViewType.LIST,
            onSortOrderClick = {},
            onChangeViewTypeClick = {},
        )
    } else {
        MegaEmptyView(
            modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
            imagePainter = painterResource(id = emptyState.first),
            text = stringResource(id = emptyState.second)
        )
    }
}