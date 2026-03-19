package mega.privacy.android.feature.clouddrive.presentation.offline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.components.offline.HandleOfflineNodeAction3
import mega.privacy.android.core.nodecomponents.components.offline.OfflineNodeActionsViewModel
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.shared.nodes.components.NodeGridViewItem
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.components.NodeListViewItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineSelectionAction
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.BackButtonPressedEvent
import mega.privacy.mobile.analytics.event.OfflineScreenEvent
import mega.privacy.mobile.analytics.event.ViewModeButtonPressedEvent

/**
 * OfflineScreen - A purely composable screen for displaying offline files
 *
 * @param onBack Callback for back navigation
 * @param viewModel The OfflineViewModel to manage state
 * @param modifier Modifier for the composable
 * @param onNavigate Callback to navigate to a NavKey destination
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onNavigateToTransfers: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    openFileInformation: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfflineViewModel = hiltViewModel(),
    actionViewModel: OfflineNodeActionsViewModel = hiltViewModel(),
    onNavigate: (NavKey) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionUiState by actionViewModel.uiState.collectAsStateWithLifecycle()

    HandleOfflineNodeAction3(
        uiState = actionUiState,
        applyShareContentUris = actionViewModel::applyShareContentUris,
        consumeShareFilesEvent = actionViewModel::onShareFilesEventConsumed,
        consumeShareNodeLinksEvent = actionViewModel::onShareNodeLinksEventConsumed,
        consumeOpenFileEvent = actionViewModel::onOpenFileEventConsumed,
        onNavigate = onNavigate,
    )

    OfflineScreen(
        uiState = uiState,
        snackbarEventQueue = rememberSnackBarQueue(),
        onBack = onBack,
        selectAll = viewModel::selectAll,
        deselectAll = viewModel::clearSelection,
        onItemClicked = viewModel::onItemClicked,
        onItemLongClicked = viewModel::onLongItemClicked,
        onOpenFile = actionViewModel::handleOpenOfflineFile,
        onOpenWithFile = actionViewModel::handleOpenWithIntent,
        onDismissOfflineWarning = viewModel::dismissOfflineWarning,
        onNavigateToFolder = onNavigateToFolder,
        onNavigateToTransfers = onNavigateToTransfers,
        onSearch = viewModel::setSearchQuery,
        consumeOpenFolderEvent = viewModel::onOpenFolderInPageEventConsumed,
        consumeOpenFileEvent = viewModel::onOpenOfflineNodeEventConsumed,
        consumeRemoveEvent = viewModel::consumeRemoveNodesEvent,
        shareOfflineFiles = { files ->
            actionViewModel.handleShareOfflineNodes(
                nodes = files,
                isOnline = uiState.isOnline
            )
        },
        saveOfflineFilesToDevice = { handles ->
            val nodes = handles.map { NodeId(it) }
            onTransfer(TransferTriggerEvent.CopyOfflineNode(nodes))
        },
        onChangeViewType = viewModel::updateViewType,
        removeOfflineNodes = viewModel::removeOfflineNodes,
        openFileInformation = openFileInformation,
        onSortNodes = viewModel::setSortOrder,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfflineScreen(
    uiState: OfflineUiState,
    onBack: () -> Unit,
    selectAll: () -> Unit,
    deselectAll: () -> Unit,
    onItemClicked: (OfflineNodeUiItem) -> Unit,
    onItemLongClicked: (OfflineNodeUiItem) -> Unit,
    onNavigateToFolder: (nodeId: Int, name: String) -> Unit,
    onNavigateToTransfers: () -> Unit,
    onOpenFile: (OfflineFileInformation) -> Unit,
    onOpenWithFile: (OfflineFileInformation) -> Unit,
    onDismissOfflineWarning: () -> Unit,
    onSearch: (String) -> Unit,
    shareOfflineFiles: (List<OfflineFileInformation>) -> Unit,
    saveOfflineFilesToDevice: (List<Long>) -> Unit,
    removeOfflineNodes: (List<Long>) -> Unit,
    openFileInformation: (String) -> Unit,
    onChangeViewType: () -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
    snackbarEventQueue: SnackbarEventQueue = rememberSnackBarQueue(),
    consumeOpenFolderEvent: () -> Unit = {},
    consumeOpenFileEvent: () -> Unit = {},
    consumeRemoveEvent: () -> Unit = {},
) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var selectedHandlesToRemove by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val offlineBottomSheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        Analytics.tracker.trackEvent(OfflineScreenEvent)
        onPauseOrDispose {}
    }

    EventEffect(
        event = uiState.openFolderInPageEvent,
        onConsumed = consumeOpenFolderEvent
    ) { folderNode ->
        onNavigateToFolder(folderNode.id, folderNode.name)
    }

    EventEffect(
        event = uiState.openOfflineNodeEvent,
        onConsumed = consumeOpenFileEvent
    ) { file ->
        onOpenFile(file)
    }

    EventEffect(
        event = uiState.removeNodesSuccessEvent,
        onConsumed = consumeRemoveEvent
    ) { size ->
        if (size > 1) {
            snackbarEventQueue.queueMessage(
                sharedR.string.offline_remove_multiple_item_success_message,
                size
            )
        } else {
            snackbarEventQueue.queueMessage(sharedR.string.offline_remove_singular_item_success_message)
        }
    }

    BackHandler(uiState.selectedNodeHandles.isNotEmpty()) {
        deselectAll()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        bottomBar = {
            SelectionModeBottomBar(
                modifier = Modifier
                    .testTag(OFFLINE_SCREEN_SELECTION_MODE_BOTTOM_BAR_TAG),
                visible = uiState.selectedNodeHandles.isNotEmpty(),
                actions = OfflineSelectionAction.bottomBarItems,
                onActionPressed = {
                    when (it) {
                        is OfflineSelectionAction.Delete -> {
                            selectedHandlesToRemove = uiState.selectedNodeHandles
                            showRemoveDialog = true
                        }

                        is OfflineSelectionAction.Download -> {
                            saveOfflineFilesToDevice(uiState.selectedNodeHandles)
                            deselectAll()
                        }

                        is OfflineSelectionAction.Share -> {
                            shareOfflineFiles(uiState.selectedOfflineNodes)
                            deselectAll()
                        }
                    }
                }
            )
        },
        topBar = {
            if (uiState.selectedNodeHandles.isEmpty()) {
                MegaSearchTopAppBar(
                    modifier = Modifier
                        .testTag(OFFLINE_SCREEN_SEARCH_TOP_APP_BAR_TAG),
                    navigationType = AppBarNavigationType.Back {
                        Analytics.tracker.trackEvent(BackButtonPressedEvent)
                        onBack()
                    },
                    trailingIcons = {
                        TransfersToolbarWidget(
                            onClick = onNavigateToTransfers,
                            modifier = Modifier.testTag(OFFLINE_SCREEN_TRANSFER_WIDGET)
                        )
                    },
                    title = uiState
                        .title
                        .takeIf { uiState.nodeId != -1 }
                        ?: stringResource(sharedResR.string.offline_screen_title),
                    query = uiState.searchQuery,
                    onQueryChanged = onSearch,
                    isSearchingMode = isSearchMode,
                    onSearchingModeChanged = {
                        isSearchMode = it
                        if (!it) {
                            // Clear search query when exiting search mode and
                            // reset to the original search result
                            onSearch("")
                        }
                    }
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier
                        .testTag(OFFLINE_SCREEN_DEFAULT_TOP_APP_BAR_TAG),
                    title = uiState.selectedNodeHandles.size.toString(),
                    navigationType = AppBarNavigationType.Close(deselectAll),
                    actions = buildList {
                        if (!uiState.areAllNodesSelected) {
                            add(OfflineSelectionAction.SelectAll)
                        }
                    },
                    onActionPressed = { action ->
                        when (action) {
                            is OfflineSelectionAction.SelectAll -> selectAll()
                            else -> return@MegaTopAppBar
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        var visibleOfflineInformation by remember { mutableStateOf<OfflineFileInformation?>(null) }
        val dismissOfflineBottomSheet = remember {
            {
                coroutineScope
                    .launch { offlineBottomSheetState.hide() }
                    .invokeOnCompletion { visibleOfflineInformation = null }
            }
        }
        val dismissSortBottomSheet = remember {
            {
                coroutineScope
                    .launch { sortBottomSheetState.hide() }
                    .invokeOnCompletion { showSortBottomSheet = false }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.excludingBottomPadding()),
        ) {
            if (uiState.showOfflineWarning && !uiState.isLoading) {
                TopWarningBanner(
                    modifier = Modifier
                        .testTag(OFFLINE_SCREEN_TOP_WARNING_BANNER_TAG)
                        .fillMaxWidth(),
                    body = stringResource(sharedResR.string.logout_offline_content_deletion_warning),
                    showCancelButton = true,
                    onCancelButtonClick = onDismissOfflineWarning
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        NodesViewSkeleton(
                            modifier = Modifier
                                .testTag(OFFLINE_SCREEN_LOADING_TAG)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(),
                            isListView = uiState.currentViewType == ViewType.LIST,
                        )
                    }

                    uiState.offlineNodes.isEmpty() -> {
                        MegaEmptyView(
                            modifier = Modifier
                                .testTag(OFFLINE_SCREEN_EMPTY_TAG)
                                .align(Alignment.Center),
                            text = stringResource(sharedR.string.offline_screen_empty_state_description),
                            imagePainter = painterResource(iconPackR.drawable.ic_arrow_circle_down_glass)
                        )
                    }

                    else -> {
                        OfflineContent(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState,
                            onItemClicked = onItemClicked,
                            onItemLongClicked = onItemLongClicked,
                            onMoreClicked = {
                                visibleOfflineInformation = it.offlineFileInformation
                            },
                            contentPadding = PaddingValues(
                                bottom = paddingValues.calculateBottomPadding()
                            ),
                            onChangeViewType = {
                                Analytics.tracker.trackEvent(ViewModeButtonPressedEvent)
                                onChangeViewType()
                            },
                            onSortClick = {
                                showSortBottomSheet = true
                            }
                        )
                    }
                }
            }
        }

        OfflineOptionsBottomSheet(
            modifier = Modifier
                .testTag(OFFLINE_SCREEN_BOTTOM_SHEET_TAG),
            offlineFileInformation = visibleOfflineInformation,
            onShareOfflineFile = { file ->
                shareOfflineFiles(listOf(file))
                dismissOfflineBottomSheet()
            },
            onSaveOfflineFileToDevice = { file ->
                val safeHandle = file.handle.toLongOrNull() ?: return@OfflineOptionsBottomSheet
                saveOfflineFilesToDevice(listOf(safeHandle))
                dismissOfflineBottomSheet()
            },
            onOpenOfflineFile = { file ->
                openFileInformation(file.handle)
                dismissOfflineBottomSheet()
            },
            onOpenWithFile = { file ->
                onOpenWithFile(file)
                dismissOfflineBottomSheet()
            },
            onDeleteOfflineFile = { file ->
                val safeHandle = file.handle.toLongOrNull() ?: return@OfflineOptionsBottomSheet
                selectedHandlesToRemove = listOf(safeHandle)
                showRemoveDialog = true
                dismissOfflineBottomSheet()
            },
            onDismiss = { dismissOfflineBottomSheet() },
            sheetState = offlineBottomSheetState
        )

        if (showRemoveDialog) {
            RemoveFromOfflineDialog(
                modifier = Modifier
                    .testTag(OFFLINE_SCREEN_REMOVE_FROM_OFFLINE_TAG),
                onRemove = {
                    removeOfflineNodes(selectedHandlesToRemove)
                    deselectAll()
                    showRemoveDialog = false
                },
                onCancel = {
                    selectedHandlesToRemove = emptyList()
                    showRemoveDialog = false
                }
            )
        }

        if (showSortBottomSheet) {
            SortBottomSheet(
                modifier = Modifier.testTag(OFFLINE_SCREEN_SORT_BOTTOM_SHEET_TAG),
                options = NodeSortOption.getOptionsForSourceType(NodeSourceType.OFFLINE),
                title = stringResource(sharedResR.string.action_sort_by_header),
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

                        dismissSortBottomSheet()
                    }
                },
                onDismissRequest = {
                    dismissSortBottomSheet()
                }
            )
        }
    }
}

@Composable
private fun OfflineContent(
    uiState: OfflineUiState,
    onItemClicked: (OfflineNodeUiItem) -> Unit,
    onItemLongClicked: (OfflineNodeUiItem) -> Unit,
    onMoreClicked: (OfflineNodeUiItem) -> Unit,
    onChangeViewType: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (uiState.currentViewType) {
        ViewType.LIST -> {
            FastScrollLazyColumn(
                modifier = modifier
                    .testTag(OFFLINE_SCREEN_LIST_COLUMN_TAG),
                contentPadding = contentPadding,
                totalItems = uiState.offlineNodes.size + 1 // +1 for header
            ) {
                item(key = "header") {
                    NodeHeaderItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp),
                        onSortOrderClick = onSortClick,
                        onChangeViewTypeClick = onChangeViewType,
                        onEnterMediaDiscoveryClick = {},
                        sortConfiguration = uiState.selectedSortConfiguration,
                        isListView = true,
                        showSortOrder = true,
                        showChangeViewType = true,
                    )
                }

                items(
                    items = uiState.offlineNodes,
                    key = { it.offlineFileInformation.handle }
                ) { node ->
                    NodeListViewItem(
                        title = node.offlineFileInformation.name,
                        subtitle = getOfflineNodeDescription(node.offlineFileInformation),
                        icon = if (node.offlineFileInformation.isFolder) {
                            iconPackR.drawable.ic_folder_medium_solid
                        } else {
                            getFileTypeIcon(node.offlineFileInformation.name) ?: return@items
                        },
                        thumbnailData = node.offlineFileInformation.thumbnailData,
                        highlightText = uiState.searchQuery ?: "",
                        isSelected = node.isSelected,
                        isInSelectionMode = uiState.selectedNodeHandles.isNotEmpty(),
                        isHighlighted = node.isHighlighted,
                        onMoreClicked = { onMoreClicked(node) },
                        onItemClicked = { onItemClicked(node) },
                        onLongClicked = { onItemLongClicked(node) }
                    )
                }
            }
        }

        ViewType.GRID -> {
            FastScrollLazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier
                    .testTag(OFFLINE_SCREEN_GRID_COLUMN_TAG)
                    .padding(horizontal = 8.dp),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                totalItems = uiState.offlineNodes.size + 1 // +1 for header
            ) {
                item(
                    key = "header",
                    span = { GridItemSpan(currentLineSpan = maxLineSpan) }
                ) {
                    NodeHeaderItem(
                        onSortOrderClick = onSortClick,
                        onChangeViewTypeClick = onChangeViewType,
                        onEnterMediaDiscoveryClick = {},
                        sortConfiguration = uiState.selectedSortConfiguration,
                        isListView = true,
                        showSortOrder = true,
                        showChangeViewType = true,
                    )
                }

                items(
                    items = uiState.offlineNodes,
                    key = { it.offlineFileInformation.handle }
                ) { node ->
                    NodeGridViewItem(
                        name = node.offlineFileInformation.name,
                        iconRes = if (node.offlineFileInformation.isFolder) {
                            iconPackR.drawable.ic_folder_medium_solid
                        } else {
                            getFileTypeIcon(node.offlineFileInformation.name) ?: return@items
                        },
                        thumbnailData = node.offlineFileInformation.thumbnailData,
                        isTakenDown = false,
                        isSelected = node.isSelected,
                        isInSelectionMode = uiState.selectedNodeHandles.isNotEmpty(),
                        isFolderNode = node.offlineFileInformation.isFolder,
                        isVideoNode = node.offlineFileInformation.fileTypeInfo is VideoFileTypeInfo,
                        highlightText = uiState.searchQuery ?: "",
                        isHighlighted = node.isHighlighted,
                        label = null,
                        onClick = { onItemClicked(node) },
                        onLongClick = { onItemLongClicked(node) },
                        onMenuClick = { onMoreClicked(node) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoveFromOfflineDialog(
    onRemove: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier,
        description = stringResource(sharedResR.string.offline_item_deletion_confirmation_title),
        positiveButtonText = stringResource(sharedResR.string.general_remove),
        negativeButtonText = stringResource(sharedResR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = onRemove,
        onNegativeButtonClicked = onCancel,
    )
}

internal const val OFFLINE_SCREEN_SEARCH_TOP_APP_BAR_TAG = "offline_screen:search_top_app_bar"
internal const val OFFLINE_SCREEN_DEFAULT_TOP_APP_BAR_TAG = "offline_screen:top_app_bar"
internal const val OFFLINE_SCREEN_GRID_COLUMN_TAG = "offline_screen:grid_column"
internal const val OFFLINE_SCREEN_LIST_COLUMN_TAG = "offline_screen:list_column"
internal const val OFFLINE_SCREEN_LOADING_TAG = "offline_screen:loading"
internal const val OFFLINE_SCREEN_EMPTY_TAG = "offline_screen:empty"
internal const val OFFLINE_SCREEN_REMOVE_FROM_OFFLINE_TAG =
    "offline_screen:remove_from_offline_dialog"
internal const val OFFLINE_SCREEN_BOTTOM_SHEET_TAG = "offline_screen:bottom_sheet"
internal const val OFFLINE_SCREEN_TOP_WARNING_BANNER_TAG = "offline_screen:top_warning_banner"
internal const val OFFLINE_SCREEN_SELECTION_MODE_BOTTOM_BAR_TAG =
    "offline_screen:selection_mode_bottom_bar"
internal const val OFFLINE_SCREEN_SORT_BOTTOM_SHEET_TAG = "offline_screen:sort_bottom_sheet"
internal const val OFFLINE_SCREEN_TRANSFER_WIDGET = "offline_screen:transfers_widget"
