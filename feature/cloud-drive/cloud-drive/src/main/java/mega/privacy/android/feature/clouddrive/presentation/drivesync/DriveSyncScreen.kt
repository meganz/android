package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionIconWithClick
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveScanDocumentHandler
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.feature.sync.ui.settings.SyncSettingsBottomSheetViewM3
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Drive Sync Screen, shown in the Drive bottom navigation tab
 *
 * @param setNavigationItemVisibility Callback to set the visibility of the navigation item
 * @param viewModel ViewModel for managing the state of the Drive Sync screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DriveSyncScreen(
    navigationHandler: NavigationHandler,
    setNavigationItemVisibility: (Boolean) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    openSearch: (Boolean, Long, NodeSourceType) -> Unit,
    cloudDriveViewModel: CloudDriveViewModel,
    viewModel: DriveSyncViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel()
) {
    val cloudDriveUiState by cloudDriveViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val megaNavigator = viewModel.megaNavigator
    var showUploadOptionsBottomSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSyncSettings by rememberSaveable { mutableStateOf(false) }
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionHandler = rememberNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator
    )

    BackHandler(enabled = cloudDriveUiState.isInSelectionMode) {
        cloudDriveViewModel.processAction(DeselectAllItems)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (cloudDriveUiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = cloudDriveUiState.selectedItemsCount,
                    isSelecting = cloudDriveUiState.nodesLoadingState != NodesLoadingState.FullyLoaded,
                    onSelectAllClicked = { cloudDriveViewModel.processAction(SelectAllItems) },
                    onCancelSelectionClicked = { cloudDriveViewModel.processAction(DeselectAllItems) }
                )
            } else {
                MegaTopAppBar(
                    navigationType = AppBarNavigationType.None,
                    title = stringResource(sharedR.string.general_drive),
                    trailingIcons = {
                        TransfersToolbarWidget(navigationHandler)
                    },
                    actions = buildList {
                        when {
                            selectedTabIndex == 0 && cloudDriveUiState.items.isNotEmpty() -> add(
                                MenuActionIconWithClick(CloudDriveAppBarAction.Search) {
                                    openSearch(
                                        true,
                                        cloudDriveUiState.currentFolderId.longValue,
                                        cloudDriveViewModel.nodeSourceType
                                    )
                                })

                            selectedTabIndex == 1 -> add(
                                MenuActionIconWithClick(
                                    CloudDriveAppBarAction.More
                                ) {
                                    showSyncSettings = true
                                })
                        }
                    }
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && cloudDriveUiState.isInSelectionMode,
                nodeActionHandler = nodeActionHandler,
                selectedNodes = cloudDriveUiState.selectedNodes,
                isSelecting = cloudDriveUiState.isSelecting
            )
        },
        floatingActionButton = {
            val showFab =
                selectedTabIndex == 0 && !cloudDriveUiState.isInSelectionMode
            AddContentFab(
                visible = showFab,
                onClick = { showUploadOptionsBottomSheet = true }
            )
        },
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs = cloudDriveUiState.isInSelectionMode,
            pagerScrollEnabled = !cloudDriveUiState.isInSelectionMode,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(sharedR.string.general_section_cloud_drive)),
                ) { _, modifier ->
                    CloudDriveContent(
                        isTabContent = true,
                        navigationHandler = navigationHandler,
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                        uiState = cloudDriveUiState,
                        onAction = cloudDriveViewModel::processAction,
                        onNavigateBack = { }, // Ignore back navigation in this tab
                        onTransfer = onTransfer,
                        showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
                        onDismissUploadOptionsBottomSheet = {
                            showUploadOptionsBottomSheet = false
                        },
                        onSortNodes = cloudDriveViewModel::setCloudSortOrder,
                        nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                        nodeActionHandler = nodeActionHandler,
                        modifier = modifier,
                    )
                }
                addTextTabWithProvidedScrollableModifier(
                    tabItem = TabItems(stringResource(sharedR.string.general_syncs)),
                ) {
                    SyncListRoute(
                        applyRevampStyles = true,
                        isInCloudDrive = true,
                        syncPermissionsManager = viewModel.syncPermissionsManager,
                        onSyncFolderClicked = {
                            megaNavigator.openNewSync(
                                context,
                                SyncType.TYPE_TWOWAY,
                                isFromCloudDrive = true
                            )
                        },
                        onBackupFolderClicked = {
                            megaNavigator.openNewSync(
                                context,
                                SyncType.TYPE_BACKUP,
                                isFromCloudDrive = true
                            )
                        },
                        onSelectStopBackupDestinationClicked = {
                            megaNavigator.openSelectStopBackupDestinationFromSyncsTab(context, it)
                        },
                        onOpenUpgradeAccountClicked = {
                            megaNavigator.openUpgradeAccount(context)
                        },
                        onOpenMegaFolderClicked = { handle ->
                            navigationHandler.navigate(CloudDriveNavKey(nodeHandle = handle))
                        },
                        onCameraUploadsSettingsClicked = {
                            megaNavigator.openSettingsCameraUploads(context)
                        },
                        onFabExpanded = { isExpanded -> }
                    )
                }
            },
            initialSelectedIndex = 0,
            onTabSelected = {
                selectedTabIndex = it
                true
            }
        )
    }

    LaunchedEffect(cloudDriveUiState.isInSelectionMode) {
        setNavigationItemVisibility(!cloudDriveUiState.isInSelectionMode)
    }

    // Handle scan document functionality
    CloudDriveScanDocumentHandler(
        cloudDriveUiState = cloudDriveUiState,
        onDocumentScannerFailedToOpen = cloudDriveViewModel::onDocumentScannerFailedToOpen,
        onGmsDocumentScannerConsumed = cloudDriveViewModel::onGmsDocumentScannerConsumed,
        onDocumentScanningErrorConsumed = cloudDriveViewModel::onDocumentScanningErrorConsumed,
    )

    SyncSettingsBottomSheetViewM3(shouldShowBottomSheet = showSyncSettings) {
        showSyncSettings = false
    }
}