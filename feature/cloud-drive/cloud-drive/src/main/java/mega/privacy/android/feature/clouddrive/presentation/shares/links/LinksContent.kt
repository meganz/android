package mega.privacy.android.feature.clouddrive.presentation.shares.links

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksAction
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksContent(
    uiState: LinksUiState,
    navigationHandler: NavigationHandler,
    onAction: (LinksAction) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onShowNodeOptions: (NodeId) -> Unit,
    onSortOrderClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val snackbarHostState = LocalSnackBarHostState.current
    var shouldShowSkeleton by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            delay(200L)
            if (this.isActive) {
                shouldShowSkeleton = true
            }
        } else {
            shouldShowSkeleton = false
        }
    }
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)

    Column(
        modifier = modifier
            .padding(contentPadding.excludingBottomPadding()),
    ) {
        when {
            uiState.isLoading -> {
                if (shouldShowSkeleton) {
                    NodesViewSkeleton(
                        isListView = isListView,
                        spanCount = spanCount,
                        contentPadding = PaddingValues(top = 12.dp),
                    )
                }
            }

            uiState.isEmpty -> {
                MegaEmptyView(
                    imagePainter = painterResource(iconPackR.drawable.ic_link_glass),
                    text = stringResource(R.string.context_empty_links)
                )
            }

            else -> NodesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listContentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                ),
                listState = listState,
                gridState = gridState,
                spanCount = spanCount,
                items = uiState.items,
                isNextPageLoading = false,
                isHiddenNodesEnabled = false,
                showHiddenNodes = true,
                onMenuClicked = { onShowNodeOptions(it.id) },
                onItemClicked = { onAction(LinksAction.ItemClicked(it)) },
                onLongClicked = { onAction(LinksAction.ItemLongClicked(it)) },
                sortConfiguration = uiState.selectedSortConfiguration,
                isListView = isListView,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClicked = { onAction(LinksAction.ChangeViewTypeClicked) },
                inSelectionMode = uiState.isInSelectionMode,
            )
        }
    }

    EventEffect(
        event = uiState.navigateToFolderEvent,
        onConsumed = { onAction(LinksAction.NavigateToFolderEventConsumed) }
    ) { node ->
        navigationHandler.navigate(
            CloudDriveNavKey(
                nodeHandle = node.id.longValue,
                nodeName = node.name,
                nodeSourceType = NodeSourceType.LINKS
            )
        )
    }

    uiState.openedFileNode?.let { fileNode ->
        HandleNodeAction3(
            typedFileNode = fileNode,
            nodeSourceType = NodeSourceType.LINKS,
            sortOrder = uiState.selectedSortOrder,
            snackBarHostState = snackbarHostState,
            onActionHandled = {
                onAction(LinksAction.OpenedFileNodeHandled)
            },
            onDownloadEvent = onTransfer,
            coroutineScope = coroutineScope
        )
    }
}