package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetRoute
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.core.sharedcomponents.node.rememberNodeId
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.HandleNodeOptionEvent
import mega.privacy.android.feature.clouddrive.presentation.rubbishbin.view.ClearRubbishBinDialog
import mega.privacy.android.feature.clouddrive.presentation.rubbishbin.view.RubbishBinAppBarAction
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration.Companion.milliseconds

/**
 * M3 Compose Screen for Rubbish Bin
 * This is a fully Compose replacement for RubbishBinComposeFragment when isSingleActivityEnabled == true
 * Uses the new NodeViews implementation from core.nodecomponents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RubbishBinScreen(
    viewModel: NewRubbishBinViewModel,
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onFolderClick: (NodeId) -> Unit,
    openSearch: (Long) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val megaNavigator = rememberMegaNavigator()
    val megaResultContract = rememberMegaResultContract()
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val actionHandler: NodeActionHandler = rememberNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler
    )
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

    var visibleNodeOptionId by rememberNodeId(null)
    val nodeOptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showClearRubbishBinDialog by remember { mutableStateOf(false) }
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }


    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(message)
            }
        }
    }

    val spanCount = rememberDynamicSpanCount(
        isListView = uiState.currentViewType == ViewType.LIST
    )

    EventEffect(
        event = uiState.openFolderEvent,
        onConsumed = viewModel::onOpenFolderEventConsumed
    ) { folderId ->
        onFolderClick(folderId)
    }

    EventEffect(
        event = uiState.messageEvent,
        onConsumed = viewModel::onMessageShown
    ) { message ->
        coroutineScope.launch {
            snackbarHostState?.showSnackbar(message.get(context))
        }
    }

    val isRootDirectory = uiState.parentFolderId == null
    var shouldShowSkeleton by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            delay(100.milliseconds)
            shouldShowSkeleton = true
        } else {
            shouldShowSkeleton = false
        }
    }

    LaunchedEffect(uiState.selectedNodes.size) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            uiState.selectedNodes.toSet(),
            NodeSourceType.RUBBISH_BIN
        )
    }

    HandleNodeOptionEvent(
        megaNavigator = megaNavigator,
        nodeActionState = nodeActionState,
        nameCollisionLauncher = nameCollisionLauncher,
        snackbarHostState = snackbarHostState,
        onNodeNameCollisionResultHandled = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        onInfoToShowEventConsumed = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
        onForeignNodeDialogShown = nodeOptionsActionViewModel::markForeignNodeDialogShown,
        onQuotaDialogShown = nodeOptionsActionViewModel::markQuotaDialogShown,
        onHandleNodesWithoutConflict = { collisionType, nodes ->
            when (collisionType) {
                NodeNameCollisionType.MOVE -> nodeOptionsActionViewModel.moveNodes(nodes)
                NodeNameCollisionType.COPY -> nodeOptionsActionViewModel.copyNodes(nodes)
                else -> {
                    /* No-op for other types */
                }
            }
        },
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .navigationBarsPadding(),
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedNodes.size,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = uiState.isSelecting,
                    onSelectAllClicked = { viewModel.selectAllNodes() },
                    onCancelSelectionClicked = { viewModel.clearAllSelectedNodes() }
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier,
                    navigationType = AppBarNavigationType.Back(
                        onNavigationIconClicked = {
                            onBackPressedDispatcher?.onBackPressed()
                        }
                    ),
                    title = uiState.title.text,
                    maxActionsToShow = if (isRootDirectory) 1 else 2,
                    actions = if (uiState.items.isNotEmpty()) {
                        buildList {
                            add(MenuActionWithClick(RubbishBinAppBarAction.Search) {
                                openSearch(
                                    uiState.currentFolderId.longValue,
                                )
                            })
                            if (isRootDirectory) {
                                add(
                                    MenuActionWithClick(RubbishBinAppBarAction.Empty) {
                                        showClearRubbishBinDialog = true
                                    },
                                )
                            } else {
                                add(
                                    MenuActionWithClick(RubbishBinAppBarAction.More) {
                                        visibleNodeOptionId = uiState.currentFolderId
                                    },
                                )
                            }
                        }
                    } else emptyList(),
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeActionState.availableActions,
                visibleActions = nodeActionState.visibleActions,
                visible = nodeActionState.visibleActions.isNotEmpty() && uiState.isInSelectionMode,
                nodeActionHandler = actionHandler,
                isSelecting = uiState.isSelecting,
                selectedNodes = uiState.selectedNodes,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                if (shouldShowSkeleton) {
                    NodesViewSkeleton(
                        contentPadding = innerPadding,
                        isListView = uiState.currentViewType == ViewType.LIST,
                        spanCount = spanCount
                    )
                }
            }

            uiState.items.isEmpty() && uiState.nodesLoadingState == NodesLoadingState.FullyLoaded -> {
                MegaEmptyView(
                    modifier = Modifier
                        .testTag(NODES_EMPTY_VIEW_VISIBLE),
                    imagePainter = painterResource(
                        if (isRootDirectory) {
                            iconPackR.drawable.ic_empty_trash_glass
                        } else {
                            iconPackR.drawable.ic_empty_folder_glass
                        }
                    ),
                    text = stringResource(
                        if (isRootDirectory) {
                            R.string.context_empty_rubbish_bin
                        } else {
                            R.string.file_browser_empty_folder_new
                        }
                    )
                )
            }

            else -> {
                NodesView(
                    items = uiState.items,
                    listContentPadding = innerPadding,
                    onMenuClicked = { nodeUiItem ->
                        visibleNodeOptionId = nodeUiItem.node.id
                    },
                    onItemClicked = { nodeUiItem ->
                        viewModel.onItemClicked(nodeUiItem)
                    },
                    onLongClicked = { nodeUiItem ->
                        viewModel.onItemLongClicked(nodeUiItem)
                    },
                    sortConfiguration = uiState.sortConfiguration,
                    isListView = uiState.currentViewType == ViewType.LIST,
                    onSortOrderClick = { showSortBottomSheet = true },
                    onChangeViewTypeClicked = viewModel::onChangeViewTypeClicked,
                    spanCount = spanCount,
                    showHiddenNodes = uiState.isHiddenNodesEnabled
                            && uiState.accountType?.isPaid == true
                            && !uiState.isBusinessAccountExpired,
                    isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                    inSelectionMode = uiState.isInSelectionMode,
                )
            }
        }
    }

    // Show node options bottom sheet
    visibleNodeOptionId?.let { nodeId ->
        MegaModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetState = nodeOptionSheetState,
            onDismissRequest = { visibleNodeOptionId = null },
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1
        ) {
            NodeOptionsBottomSheetRoute(
                navigationHandler = navigationHandler,
                onDismiss = { visibleNodeOptionId = null },
                nodeId = nodeId.longValue,
                nodeSourceType = NodeSourceType.RUBBISH_BIN,
                onTransfer = onTransfer,
                actionHandler = actionHandler,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            )
        }
    }

    // Handle file click for opening files
    uiState.openedFileNode?.let { fileNode ->
        HandleNodeAction3(
            typedFileNode = fileNode,
            nodeSourceType = NodeSourceType.RUBBISH_BIN,
            sortOrder = uiState.sortOrder,
            snackBarHostState = snackbarHostState,
            onActionHandled = {
                viewModel.onOpenedFileNodeHandled()
            },
            onDownloadEvent = onTransfer,
            coroutineScope = coroutineScope,
            navigationHandler = navigationHandler,
        )
    }

    // Show clear rubbish bin confirmation dialog
    if (showClearRubbishBinDialog) {
        ClearRubbishBinDialog(
            onDismiss = { showClearRubbishBinDialog = false },
            onClearRubbishBin = {
                viewModel.clearRubbishBin()
            }
        )
    }

    // Show sort bottom sheet
    if (showSortBottomSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(NodeSourceType.RUBBISH_BIN),
            sheetState = sortBottomSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = uiState.sortConfiguration.sortOption,
                sortDirection = uiState.sortConfiguration.sortDirection
            ),
            onDismissRequest = {
                showSortBottomSheet = false
            },
            onSortOptionSelected = { result ->
                result?.let {
                    viewModel.setSortConfiguration(
                        NodeSortConfiguration(
                            sortOption = it.sortOptionItem,
                            sortDirection = it.sortDirection
                        )
                    )
                    showSortBottomSheet = false
                }
            },
        )
    }
}

/**
 * Test tag for empty view in nodes screen
 */
const val NODES_EMPTY_VIEW_VISIBLE = "rubbish_bin_screen:empty_view"
