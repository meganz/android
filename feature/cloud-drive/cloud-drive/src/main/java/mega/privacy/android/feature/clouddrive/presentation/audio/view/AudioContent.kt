package mega.privacy.android.feature.clouddrive.presentation.audio.view

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
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioAction
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioAction.ChangeViewTypeClicked
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioAction.ItemClicked
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioAction.OpenedFileNodeHandled
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.nodes.components.NodeSkeletons
import mega.privacy.android.shared.nodes.components.NodesView
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.rememberDynamicSpanCount
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ViewModeButtonPressedEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioContent(
    navigationHandler: NavigationHandler,
    uiState: AudioUiState,
    onAction: (AudioAction) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    showNodeOptionsBottomSheet: (NodeOptionsBottomSheetNavKey) -> Unit,
    selectionState: NodeSelectionState,
    isInSelectionMode: Boolean,
    selectedNodes: List<TypedNode>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
        ),
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val isListView = when (uiState) {
        is AudioUiState.Loading -> true
        is AudioUiState.Data -> uiState.currentViewType == ViewType.LIST
    }
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedNodes) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            selectedNodes.toSet(),
            NodeSourceType.CLOUD_DRIVE
        )
    }

    Column(
        modifier = modifier
            .padding(contentPadding.excludingBottomPadding())
    ) {
        when (uiState) {
            is AudioUiState.Loading -> {
                NodesViewSkeleton(
                    isListView = isListView,
                    spanCount = spanCount,
                    contentPadding = PaddingValues(top = 12.dp),
                    delay = NodeSkeletons.defaultDelay,
                )
            }

            is AudioUiState.Data -> when {
                uiState.isEmpty -> {
                    MegaEmptyView(
                        imagePainter = painterResource(id = iconPackR.drawable.ic_audio_glass),
                        text = stringResource(id = sharedR.string.homepage_empty_hint_audio)
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
                    isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                    showHiddenNodes = uiState.showHiddenNodes,
                    onMenuClicked = {
                        showNodeOptionsBottomSheet(
                            NodeOptionsBottomSheetNavKey(
                                nodeHandle = it.id.longValue,
                                nodeSourceType = NodeSourceType.CLOUD_DRIVE
                            )
                        )
                    },
                    onItemClicked = {
                        if (isInSelectionMode) {
                            selectionState.toggleSelection(it.id)
                        } else {
                            onAction(ItemClicked(it))
                        }
                    },
                    onLongClicked = { selectionState.toggleSelection(it.id) },
                    sortConfiguration = uiState.selectedSortConfiguration,
                    isListView = isListView,
                    onSortOrderClick = { showSortBottomSheet = true },
                    onChangeViewTypeClicked = {
                        Analytics.tracker.trackEvent(ViewModeButtonPressedEvent)
                        onAction(ChangeViewTypeClicked)
                    },
                    showMediaDiscoveryButton = false,
                    onEnterMediaDiscoveryClick = {},
                    inSelectionMode = isInSelectionMode,
                    isContactVerificationOn = false,
                    nodeSelectionState = selectionState,
                )
            }
        }

        if (uiState is AudioUiState.Data) {
            uiState.openedFileNode?.let { openedFileNode ->
                HandleNodeAction3(
                    typedFileNode = openedFileNode,
                    snackBarHostState = snackbarHostState,
                    onNavigate = navigationHandler::navigate,
                    coroutineScope = coroutineScope,
                    onActionHandled = { onAction(OpenedFileNodeHandled) },
                    nodeSourceData = NodeSourceData.Default(NodeSourceType.AUDIO),
                    onDownloadEvent = onTransfer,
                    sortOrder = uiState.selectedSortOrder
                )
            }
        }

        if (showSortBottomSheet && uiState is AudioUiState.Data) {
            SortBottomSheet(
                title = stringResource(sharedR.string.action_sort_by_header),
                options = NodeSortOption.getOptionsForSourceType(NodeSourceType.AUDIO),
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
