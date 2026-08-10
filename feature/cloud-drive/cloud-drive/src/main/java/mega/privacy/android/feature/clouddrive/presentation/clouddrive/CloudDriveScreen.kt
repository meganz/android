package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentHandler
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentViewModel
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.getSelectedItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.contract.state.ReportSelectionMode
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.mobile.analytics.event.CloudDriveBottomToolBarMoreMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveFABPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveParentNodeMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveScreenEvent

/**
 * Cloud Drive Screen, used to display contents of a folder
 *
 * @param onBack Callback to be invoked when the back button is pressed
 * @param onTransfer Callback to handle transfer events
 * @param viewModel ViewModel for managing the state of the Cloud Drive screen
 */
@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    navigateToCloudDriveFolder: (TypedFolderNode, NodeSourceType) -> Unit,
    setNavigationBarVisibility: (Boolean) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
        ),
    scanDocumentViewModel: ScanDocumentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    var showUploadOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }
    val megaNavigator = rememberMegaNavigator()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )

    val selectionState = rememberNodeSelectionState()
    ReportSelectionMode(isInSelectionMode = selectionState.isInSelectionMode)

    val isAllSelected by remember {
        derivedStateOf {
            val itemsCount =
                (uiState as? CloudDriveUiState.Data)?.items?.size ?: 0
            selectionState.selectedItemsCount == itemsCount && itemsCount > 0
        }
    }
    val selectedNodes by remember {
        derivedStateOf {
            uiState.getSelectedItems(selectionState.selectedNodeIds)
        }
    }


    // Select-all-during-partial-load: once fully loaded, select all items
    LaunchedEffect(selectionState.selectAllAwaitingMoreItems, uiState) {
        val state = uiState
        if (state is CloudDriveUiState.Data) {
            if (selectionState.selectAllAwaitingMoreItems) {
                selectionState.selectAll(
                    state.items.map { it.node.id }.toSet(),
                    state.nodesLoadingState
                )
            }
        }
    }

    BackHandler(enabled = selectionState.isInSelectionMode) {
        selectionState.deselectAll()
    }

    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(CloudDriveScreenEvent)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            when (val state = uiState) {
                is CloudDriveUiState.Loading -> {
                    MegaTopAppBar(
                        modifier = Modifier.testTag(CLOUD_DRIVE_MAIN_APP_BAR_TAG),
                        title = uiState.title.text,
                        navigationType = AppBarNavigationType.Back(onBack),
                        trailingIcons = {
                            TransfersToolbarWidget {
                                navigationHandler.navigate(TransfersNavKey())
                            }
                        }
                    )
                }

                is CloudDriveUiState.Data -> {
                    if (selectionState.isInSelectionMode) {
                        NodeSelectionModeAppBar(
                            modifier = Modifier.testTag(CLOUD_DRIVE_SELECTION_MODE_APP_BAR_TAG),
                            count = selectionState.selectedItemsCount,
                            isAllSelected = isAllSelected,
                            isSelecting = selectionState.selectAllAwaitingMoreItems,
                            onSelectAllClicked = {
                                val allIds = state.items.map { it.node.id }.toSet()
                                selectionState.selectAll(allIds, state.nodesLoadingState)
                            },
                            onCancelSelectionClicked = { selectionState.deselectAll() }
                        )
                    } else {
                        MegaTopAppBar(
                            modifier = Modifier.testTag(CLOUD_DRIVE_MAIN_APP_BAR_TAG),
                            title = uiState.title.text,
                            navigationType = AppBarNavigationType.Back(onBack),
                            trailingIcons = {
                                TransfersToolbarWidget {
                                    navigationHandler.navigate(TransfersNavKey())
                                }
                            },
                            actions = buildList {
                                if (state.items.isNotEmpty()) {
                                    add(
                                        MenuActionWithClick(CommonMenuAction.Search) {
                                            navigationHandler.navigate(
                                                SearchNavKey(
                                                    parentHandle = state.currentFolderId.longValue,
                                                    nodeSourceType = uiState.nodeSourceType
                                                )
                                            )
                                        }
                                    )
                                }

                                if (!state.isCloudDriveRoot) {
                                    add(
                                        MenuActionWithClick(
                                            CommonMenuAction.More
                                        ) {
                                            Analytics.tracker.trackEvent(
                                                CloudDriveParentNodeMoreButtonPressedEvent
                                            )
                                            val folderId = state.currentFolderId.longValue
                                            if (folderId != -1L) {
                                                navigationHandler.navigate(
                                                    NodeOptionsBottomSheetNavKey(
                                                        nodeHandle = folderId,
                                                        nodeSourceType = uiState.nodeSourceType,
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
        bottomBar = {
            @SuppressLint("ComposeViewModelForwarding")
            NodeSelectionModeBottomBar(
                modifier = Modifier.testTag(CLOUD_DRIVE_SELECTION_MODE_BOTTOM_BAR_TAG),
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && selectionState.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = selectedNodes,
                isSelecting = selectionState.selectAllAwaitingMoreItems,
                onMoreClicked = {
                    Analytics.tracker.trackEvent(CloudDriveBottomToolBarMoreMenuItemEvent)
                }
            )
        },
        floatingActionButton = {
            val visible = with(uiState as? CloudDriveUiState.Data) {
                this?.isUploadAllowed == true && this.items.isEmpty()
                    .not() && selectionState.isInSelectionMode.not()
            }
            AddContentFab(
                modifier = Modifier
                    .testTag(CLOUD_DRIVE_FAB_TAG)
                    .applyScrollToHideFabBehavior(),
                visible = visible,
                onClick = {
                    Analytics.tracker.trackEvent(CloudDriveFABPressedEvent)
                    showUploadOptionsBottomSheet = true
                }
            )
        },
        content = { innerPadding ->
            @SuppressLint("ComposeViewModelForwarding")
            CloudDriveContent(
                isTabContent = false,
                navigationHandler = navigationHandler,
                uiState = uiState,
                showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
                onToggleShowUploadOptionsBottomSheet = { showUploadOptionsBottomSheet = it },
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onPrepareScanDocument = scanDocumentViewModel::prepareDocumentScanner,
                onNavigateBack = onBack,
                onTransfer = onTransfer,
                onSortNodes = viewModel::setCloudSortOrder,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                selectionState = selectionState,
                isInSelectionMode = selectionState.isInSelectionMode,
                selectedItemsCount = selectionState.selectedItemsCount,
                selectedNodes = selectedNodes,
                navigateToFolder = navigateToCloudDriveFolder,
            )
        }
    )

    LaunchedEffect(selectionState.isInSelectionMode) {
        setNavigationBarVisibility(!selectionState.isInSelectionMode)
    }

    (uiState as? CloudDriveUiState.Data)?.currentFolderId?.let {
        @SuppressLint("ComposeViewModelForwarding")
        ScanDocumentHandler(
            parentNodeId = it,
            navigate = navigationHandler::navigate,
            viewModel = scanDocumentViewModel
        )
    }
}

internal const val CLOUD_DRIVE_FAB_TAG = "cloud_drive_screen:add_content_fab"
internal const val CLOUD_DRIVE_MAIN_APP_BAR_TAG = "cloud_drive_screen:main_app_bar"
internal const val CLOUD_DRIVE_SELECTION_MODE_APP_BAR_TAG =
    "cloud_drive_screen:selection_mode_app_bar"
internal const val CLOUD_DRIVE_SELECTION_MODE_BOTTOM_BAR_TAG =
    "cloud_drive_screen:selection_mode_bottom_bar"
