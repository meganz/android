package mega.privacy.android.feature.clouddrive.presentation.drivesync

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.modifiers.excludeTopPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentHandler
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentViewModel
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.computeSelectedItemsCount
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.feature.sync.ui.settings.SyncSettingsBottomSheetViewM3
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.state.ReportSelectionMode
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import mega.privacy.android.navigation.destination.SyncSelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CloudDriveBottomToolBarMoreMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveFABPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveSearchBarPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveTabEvent
import mega.privacy.mobile.analytics.event.SyncsTabEvent

/**
 * Drive Sync Screen, shown in the Drive bottom navigation tab
 *
 * @param setNavigationBarVisibility Callback to set the visibility of the navigation bar
 * @param viewModel ViewModel for managing the state of the Drive Sync screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DriveSyncScreen(
    navigationHandler: NavigationHandler,
    setNavigationBarVisibility: (Boolean) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    cloudDriveViewModel: CloudDriveViewModel,
    viewModel: DriveSyncViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
        creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
    ),
    scanDocumentViewModel: ScanDocumentViewModel = hiltViewModel(),
    initialTabIndex: Int = 0,
) {
    val cloudDriveUiState by cloudDriveViewModel.uiState.collectAsStateWithLifecycle()

    val selectionState = rememberNodeSelectionState()

    val isInSelectionMode by remember {
        derivedStateOf { selectionState.isInSelectionMode }
    }
    val selectedItemsCount by remember {
        derivedStateOf {
            cloudDriveUiState.computeSelectedItemsCount(
                selectedIds = selectionState.selectedNodeIds,
            )
        }
    }
    val isAllSelected by remember {
        derivedStateOf {
            selectedItemsCount == cloudDriveUiState.visibleItemsCount && cloudDriveUiState.visibleItemsCount > 0
        }
    }
    val selectedNodes by remember {
        derivedStateOf {
            val ids = selectionState.selectedNodeIds
            cloudDriveUiState.items.mapNotNull { item ->
                if (item.node.id in ids) item.node else null
            }
        }
    }

    // Select-all-during-partial-load
    LaunchedEffect(selectionState.selectAllAwaitingMoreItems, cloudDriveUiState.nodesLoadingState) {
        if (selectionState.selectAllAwaitingMoreItems) {
            selectionState.selectAll(cloudDriveUiState.items.map { it.node.id }.toSet(), cloudDriveUiState.nodesLoadingState)
        }
    }

    ReportSelectionMode(isInSelectionMode = isInSelectionMode)

    val megaNavigator = viewModel.megaNavigator
    var showUploadOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTabIndex) }
    var showSyncSettings by rememberSaveable { mutableStateOf(false) }
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator
    )

    BackHandler(enabled = isInSelectionMode) {
        selectionState.deselectAll()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            if (isInSelectionMode) {
                NodeSelectionModeAppBar(
                    modifier = Modifier.testTag(DRIVE_SYNCS_SELECTION_MODE_APP_BAR_TAG),
                    count = selectedItemsCount,
                    isAllSelected = isAllSelected,
                    isSelecting = selectionState.selectAllAwaitingMoreItems,
                    onSelectAllClicked = {
                        val allIds = cloudDriveUiState.items.map { it.node.id }.toSet()
                        selectionState.selectAll(allIds, cloudDriveUiState.nodesLoadingState)
                    },
                    onCancelSelectionClicked = { selectionState.deselectAll() }
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier.testTag(DRIVE_SYNCS_MAIN_APP_BAR_TAG),
                    navigationType = AppBarNavigationType.None,
                    title = stringResource(sharedR.string.general_drive),
                    trailingIcons = {
                        TransfersToolbarWidget {
                            navigationHandler.navigate(TransfersNavKey())
                        }
                    },
                    actions = buildList {
                        when {
                            selectedTabIndex == 0 && cloudDriveUiState.items.isNotEmpty() -> add(
                                MenuActionWithClick(CommonMenuAction.Search) {
                                    Analytics.tracker.trackEvent(CloudDriveSearchBarPressedEvent)
                                    navigationHandler.navigate(
                                        SearchNavKey(
                                            parentHandle = cloudDriveUiState.currentFolderId.longValue,
                                            nodeSourceType = cloudDriveUiState.nodeSourceType
                                        )
                                    )
                                }
                            )

                            selectedTabIndex == 1 -> add(
                                MenuActionWithClick(CommonMenuAction.More) {
                                    showSyncSettings = true
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                modifier = Modifier.testTag(DRIVE_SYNCS_SELECTION_MODE_BOTTOM_BAR_TAG),
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = selectedNodes,
                isSelecting = selectionState.selectAllAwaitingMoreItems,
                onMoreClicked = {
                    Analytics.tracker.trackEvent(CloudDriveBottomToolBarMoreMenuItemEvent)
                }
            )
        },
        floatingActionButton = {
            val showFab =
                selectedTabIndex == 0 && !isInSelectionMode && !cloudDriveUiState.isEmpty

            AddContentFab(
                modifier = Modifier
                    .testTag(DRIVE_SYNCS_FAB_TAG)
                    .applyScrollToHideFabBehavior(),
                visible = showFab,
                onClick = {
                    Analytics.tracker.trackEvent(CloudDriveFABPressedEvent)
                    showUploadOptionsBottomSheet = true
                }
            )
        },
    ) { paddingValues ->
        MegaCollapsibleTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs = isInSelectionMode,
            pagerScrollEnabled = !isInSelectionMode,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.general_section_cloud_drive),
                        testTag = DRIVE_SYNCS_CLOUD_DRIVE_TAB_TAG
                    ),
                ) { _, modifier ->
                    CloudDriveContent(
                        isTabContent = true,
                        navigationHandler = navigationHandler,
                        contentPadding = paddingValues.excludeTopPadding(),
                        uiState = cloudDriveUiState,
                        onAction = cloudDriveViewModel::processAction,
                        onPrepareScanDocument = scanDocumentViewModel::prepareDocumentScanner,
                        onNavigateBack = { }, // Ignore back navigation in this tab
                        onTransfer = onTransfer,
                        showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
                        onToggleShowUploadOptionsBottomSheet = {
                            showUploadOptionsBottomSheet = it
                        },
                        onSortNodes = cloudDriveViewModel::setCloudSortOrder,
                        nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                        selectionState = selectionState,
                        isInSelectionMode = isInSelectionMode,
                        selectedItemsCount = selectedItemsCount,
                        selectedNodes = selectedNodes,
                        modifier = modifier,
                    )
                }
                addTextTabWithProvidedScrollableModifier(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.general_syncs),
                        testTag = DRIVE_SYNCS_SYNCS_TAB_TAG
                    ),
                ) {
                    SyncListRoute(
                        isSingleActivity = true,
                        isInCloudDrive = true,
                        syncPermissionsManager = viewModel.syncPermissionsManager,
                        onSyncFolderClicked = {
                            navigationHandler.navigate(
                                SyncNewFolderNavKey(
                                    syncType = SyncType.TYPE_TWOWAY,
                                )
                            )
                        },
                        onBackupFolderClicked = {
                            navigationHandler.navigate(
                                SyncNewFolderNavKey(
                                    syncType = SyncType.TYPE_BACKUP,
                                )
                            )
                        },
                        onSelectStopBackupDestinationClicked = {
                            navigationHandler.navigate(
                                SyncSelectStopBackupDestinationNavKey(
                                    folderName = it
                                )
                            )
                        },
                        onOpenUpgradeAccountClicked = {
                            navigationHandler.navigate(UpgradeAccountNavKey())
                        },
                        onOpenMegaFolderClicked = { handle ->
                            navigationHandler.navigate(CloudDriveNavKey(nodeHandle = handle))
                        },
                        onCameraUploadsSettingsClicked = {
                            navigationHandler.navigate(SettingsCameraUploadsNavKey)
                        },
                        onFabExpanded = { isExpanded -> }
                    )
                }
            },
            initialSelectedIndex = initialTabIndex,
            onTabSelected = {
                selectedTabIndex = it
                when (selectedTabIndex) {
                    0 -> Analytics.tracker.trackEvent(CloudDriveTabEvent)
                    1 -> Analytics.tracker.trackEvent(SyncsTabEvent)
                }
                true
            }
        )
    }

    LaunchedEffect(isInSelectionMode) {
        setNavigationBarVisibility(!isInSelectionMode)
    }

    @SuppressLint("ComposeViewModelForwarding")
    ScanDocumentHandler(
        parentNodeId = cloudDriveUiState.currentFolderId,
        viewModel = scanDocumentViewModel,
        megaNavigator = megaNavigator,
    )

    SyncSettingsBottomSheetViewM3(shouldShowBottomSheet = showSyncSettings) {
        showSyncSettings = false
    }
}

internal const val DRIVE_SYNCS_FAB_TAG = "drive_syncs_screen:add_content_fab"
internal const val DRIVE_SYNCS_CLOUD_DRIVE_TAB_TAG = "drive_syncs_screen:cloud_drive_tab"
internal const val DRIVE_SYNCS_SYNCS_TAB_TAG = "drive_syncs_screen:syncs_tab"
internal const val DRIVE_SYNCS_MAIN_APP_BAR_TAG = "drive_syncs_screen:main_app_bar"
internal const val DRIVE_SYNCS_SELECTION_MODE_APP_BAR_TAG =
    "drive_syncs_screen:selection_mode_app_bar"
internal const val DRIVE_SYNCS_SELECTION_MODE_BOTTOM_BAR_TAG =
    "drive_syncs_screen:selection_mode_bottom_bar"
