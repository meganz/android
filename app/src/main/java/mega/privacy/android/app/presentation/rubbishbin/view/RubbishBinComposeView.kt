package mega.privacy.android.app.presentation.rubbishbin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.app.presentation.search.view.LoadingStateView
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.shared.original.core.ui.model.rememberListGridNavigationState

/**
 * View for RubbishBinComposeFragment
 * @param uiState [RubbishBinState]
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param sortOrder
 * @param emptyState
 */
@Composable
fun RubbishBinComposeView(
    uiState: RubbishBinState,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    onItemClicked: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    sortOrder: String,
    emptyState: Pair<Int, Int>,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    onResetScrollPositionEventConsumed: () -> Unit,
) {
    val listGridNavigationState = rememberListGridNavigationState(
        currentHandle = uiState.currentHandle,
        navigationHandles = uiState.openedFolderNodeHandles
    )

    EventEffect(
        event = uiState.resetScrollPositionEvent,
        onConsumed = onResetScrollPositionEventConsumed,
    ) {
        if (uiState.currentViewType == ViewType.LIST) {
            listGridNavigationState.lazyListState.scrollToItem(0)
        } else {
            listGridNavigationState.lazyGridState.scrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        if (!uiState.isLoading) {
            if (uiState.nodeList.isNotEmpty()) {
                NodesView(
                    nodeUIItems = uiState.nodeList,
                    onMenuClick = onMenuClick,
                    onItemClicked = onItemClicked,
                    onLongClick = onLongClick,
                    sortOrder = sortOrder,
                    isListView = uiState.currentViewType == ViewType.LIST,
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    listState = listGridNavigationState.lazyListState,
                    gridState = listGridNavigationState.lazyGridState,
                    onLinkClicked = onLinkClicked,
                    onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                    fileTypeIconMapper = fileTypeIconMapper,
                    inSelectionMode = uiState.isInSelection,
                    shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                            && uiState.accountType?.isPaid == true
                            && !uiState.isBusinessAccountExpired,
                    listContentPadding = PaddingValues(top = 18.dp, bottom = 86.dp),
                )
            } else {
                LegacyMegaEmptyView(
                    modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
                    imagePainter = painterResource(id = emptyState.first),
                    text = stringResource(id = emptyState.second)
                )
            }
        } else if (uiState.isRootDirectory) {
            LoadingStateView(
                isList = uiState.currentViewType == ViewType.LIST,
            )
        }
    }
}