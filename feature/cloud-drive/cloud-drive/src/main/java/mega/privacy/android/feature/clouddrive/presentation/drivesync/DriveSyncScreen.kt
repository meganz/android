package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.shared.original.core.ui.model.TopAppBarActionWithClick
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Drive Sync Screen, shown in the Drive bottom navigation tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DriveSyncScreen(
    onNavigateToFolder: (NodeId) -> Unit,
    setNavigationItemVisibility: (Boolean) -> Unit,
    viewModel: DriveSyncViewModel = hiltViewModel(),
    cloudDriveViewModel: CloudDriveViewModel = hiltViewModel(),
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    val cloudDriveUiState by cloudDriveViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val megaNavigator = viewModel.megaNavigator
    var showUploadOptionsBottomSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = cloudDriveUiState.isInSelectionMode) {
        cloudDriveViewModel.processAction(DeselectAllItems)
    }

    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            if (cloudDriveUiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = cloudDriveUiState.selectedItemsCount,
                    onSelectAllClicked = { cloudDriveViewModel.processAction(SelectAllItems) },
                    onCancelSelectionClicked = { cloudDriveViewModel.processAction(DeselectAllItems) }
                )
            } else {
                MegaTopAppBar(
                    navigationType = AppBarNavigationType.None,
                    title = stringResource(sharedR.string.general_drive),
                    actions = buildList {
                        when {
                            selectedTabIndex == 0 && cloudDriveUiState.items.isNotEmpty() -> add(
                                TopAppBarActionWithClick(CloudDriveAppBarAction.Search) {
                                    // TODO Handle search
                                })

                            selectedTabIndex == 1 -> add(
                                TopAppBarActionWithClick(
                                    CloudDriveAppBarAction.More
                                ) {
                                    // TODO Handle sync more action
                                })
                        }
                    }
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                count = cloudDriveUiState.selectedItemsCount,
                visible = cloudDriveUiState.isInSelectionMode,
                onActionPressed = {
                    // TODO
                }
            )
        },
        floatingActionButton = {
            val showFab =
                selectedTabIndex == 0 && !cloudDriveUiState.isInSelectionMode && cloudDriveUiState.visibleItemsCount > 0
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
                addTextTabWithLazyListState(
                    tabItem = TabItems(stringResource(sharedR.string.general_section_cloud_drive)),
                ) { _, _, modifier ->
                    CloudDriveContent(
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                        uiState = cloudDriveUiState,
                        onAction = cloudDriveViewModel::processAction,
                        onNavigateToFolder = onNavigateToFolder,
                        onNavigateBack = { }, // Ignore back navigation in this tab
                        onTransfer = onTransfer,
                        onAddFilesClick = {
                            showUploadOptionsBottomSheet = true
                        }
                    )
                }
                addTextTabWithLazyListState(
                    tabItem = TabItems(stringResource(sharedR.string.general_syncs)),
                ) { _, _, modifier ->
                    SyncListRoute(
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
                            onNavigateToFolder(NodeId(handle))
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

    if (showUploadOptionsBottomSheet) {
        UploadOptionsBottomSheet(
            onUploadFilesClicked = {
                // TODO: Handle upload files
            },
            onUploadFolderClicked = {
                // TODO: Handle upload folder
            },
            onScanDocumentClicked = {
                // TODO: Handle scan document
            },
            onCaptureClicked = {
                // TODO: Handle capture
            },
            onNewFolderClicked = {
                // TODO: Handle new folder
            },
            onNewTextFileClicked = {
                // TODO: Handle new text file
            },
            onDismissSheet = {
                showUploadOptionsBottomSheet = false
            }
        )
    }
}