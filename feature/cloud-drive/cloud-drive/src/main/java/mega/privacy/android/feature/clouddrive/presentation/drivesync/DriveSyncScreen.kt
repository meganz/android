package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveContent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
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
) {
    val cloudDriveUiState by cloudDriveViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val megaNavigator = viewModel.megaNavigator

    BackHandler(enabled = cloudDriveUiState.isInSelectionMode) {
        cloudDriveViewModel.deselectAllItems()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            if (cloudDriveUiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = cloudDriveUiState.selectedNodeIds.size,
                    onSelectAllClicked = cloudDriveViewModel::selectAllItems,
                    onCancelSelectionClicked = cloudDriveViewModel::deselectAllItems
                )
            } else {
                MegaTopAppBar(
                    navigationType = AppBarNavigationType.None,
                    title = stringResource(sharedR.string.general_drive),
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                count = cloudDriveUiState.selectedNodeIds.size,
                visible = cloudDriveUiState.isInSelectionMode,
                onActionPressed = {
                    // TODO
                }
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
                        fileTypeIconMapper = cloudDriveViewModel.fileTypeIconMapper,
                        onItemClicked = cloudDriveViewModel::onItemClicked,
                        onItemLongClicked = cloudDriveViewModel::onItemLongClicked,
                        onChangeViewTypeClicked = cloudDriveViewModel::onChangeViewTypeClicked,
                        onNavigateToFolder = onNavigateToFolder,
                        onNavigateToFolderEventConsumed = cloudDriveViewModel::onNavigateToFolderEventConsumed,
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
                true
            }
        )
    }

    LaunchedEffect(cloudDriveUiState.isInSelectionMode) {
        setNavigationItemVisibility(!cloudDriveUiState.isInSelectionMode)
    }
}
