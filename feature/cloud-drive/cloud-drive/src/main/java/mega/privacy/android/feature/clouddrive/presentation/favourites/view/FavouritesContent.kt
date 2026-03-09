package mega.privacy.android.feature.clouddrive.presentation.favourites.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.list.NodeSkeletons
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.ChangeViewTypeClicked
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.ItemClicked
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.ItemLongClicked
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.NavigateToFolderEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.OpenedFileNodeHandled
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ViewModeButtonPressedEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavouritesContent(
    navigationHandler: NavigationHandler,
    uiState: FavouritesUiState,
    onAction: (FavouritesAction) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    showNodeOptionsBottomSheet: (NodeOptionsBottomSheetNavKey) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(NodeSourceType.FAVOURITES) }
        ),
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            uiState.selectedNodes.toSet(),
            NodeSourceType.FAVOURITES
        )
    }

    Column(
        modifier = modifier
            .padding(contentPadding.excludingBottomPadding())
    ) {
        when {
            uiState.isLoading -> {
                NodesViewSkeleton(
                    isListView = isListView,
                    spanCount = spanCount,
                    contentPadding = PaddingValues(top = 12.dp),
                    delay = NodeSkeletons.defaultDelay,
                )
            }

            uiState.isEmpty -> {
                MegaEmptyView(
                    imagePainter = painterResource(id = iconPackR.drawable.ic_hearts_glass),
                    text = stringResource(id = sharedR.string.homepage_favourites_empty_hint)
                )
            }

            else -> NodesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listContentPadding = PaddingValues(
                    bottom = contentPadding.calculateSafeBottomPadding(),
                ),
                listState = listState,
                gridState = gridState,
                spanCount = spanCount,
                items = uiState.items,
                isNextPageLoading = false,
                isHiddenNodesEnabled = false,
                showHiddenNodes = false,
                onMenuClicked = {
                    showNodeOptionsBottomSheet(
                        NodeOptionsBottomSheetNavKey(
                            nodeHandle = it.id.longValue,
                            nodeSourceType = NodeSourceType.FAVOURITES
                        )
                    )
                },
                onItemClicked = { onAction(ItemClicked(it)) },
                onLongClicked = { onAction(ItemLongClicked(it)) },
                sortConfiguration = uiState.selectedSortConfiguration,
                isListView = isListView,
                onSortOrderClick = { showSortBottomSheet = true },
                onChangeViewTypeClicked = {
                    Analytics.tracker.trackEvent(ViewModeButtonPressedEvent)
                    onAction(ChangeViewTypeClicked)
                },
                showMediaDiscoveryButton = false,
                onEnterMediaDiscoveryClick = {},
                inSelectionMode = uiState.isInSelectionMode,
                isContactVerificationOn = false
            )
        }

        EventEffect(
            event = uiState.navigateToFolderEvent,
            onConsumed = { onAction(NavigateToFolderEventConsumed) }
        ) { node ->
            navigationHandler.navigate(
                CloudDriveNavKey(
                    nodeHandle = node.id.longValue,
                    nodeName = node.name
                )
            )
        }

        uiState.openedFileNode?.let { openedFileNode ->
            HandleNodeAction3(
                typedFileNode = openedFileNode,
                snackBarHostState = snackbarHostState,
                onNavigate = navigationHandler::navigate,
                coroutineScope = coroutineScope,
                onActionHandled = { onAction(OpenedFileNodeHandled) },
                nodeSourceData = NodeSourceData.Default(NodeSourceType.FAVOURITES),
                onDownloadEvent = onTransfer,
                sortOrder = uiState.selectedSortOrder
            )
        }

        if (showSortBottomSheet) {
            SortBottomSheet(
                title = stringResource(sharedR.string.action_sort_by_header),
                options = NodeSortOption.getOptionsForSourceType(NodeSourceType.FAVOURITES),
                sheetState = sortBottomSheetState,
                selectedSort = SortBottomSheetResult(
                    sortOptionItem = uiState.selectedSortConfiguration.sortOption,
                    sortDirection = uiState.selectedSortConfiguration.sortDirection
                ),
                onSortOptionSelected = { result ->
                    result?.let {
                        onSortNodes(
                            NodeSortConfiguration(
                                sortOption = it.sortOptionItem,
                                sortDirection = it.sortDirection
                            )
                        )
                        showSortBottomSheet = false
                    }
                },
                onDismissRequest = {
                    showSortBottomSheet = false
                }
            )
        }
    }
}
