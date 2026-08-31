package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SOLVED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.STALLED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesScreen
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.StalledIssuesScreen
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.android.feature.sync.ui.extension.showAutoDurationSnackbar
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager

/**
 * Composable function that represents the route for the sync list screen.
 *
 * This function serves as an entry point to the sync list feature, handling
 * navigation and data presentation related to syncing folders and backups.
 *
 * @param syncPermissionsManager Manages the permissions required for syncing.
 * @param onSyncFolderClicked Callback invoked when the user clicks to manage sync folders.
 * @param onBackupFolderClicked Callback invoked when the user clicks to manage backup folders.
 * @param onSelectStopBackupDestinationClicked Callback invoked when the user clicks to stop a backup destination.
 * @param onOpenUpgradeAccountClicked Callback invoked when the user clicks to upgrade their account.
 * @param onCameraUploadsSettingsClicked Callback invoked when the user clicks to see the Camera Uploads settings.
 * @param isInCloudDrive Indicates whether the user is currently within the cloud drive context. Defaults to false.
 * @param selectedChip The currently selected chip in the sync list UI. Defaults to [SyncChip.SYNC_FOLDERS].
 * @param onOpenMegaFolderClicked Callback invoked when the user clicks to open a specific Mega folder.
 * @param onStalledIssueMoreClicked Callback invoked with the id of the stalled issue whose resolution options were requested.
 * @param fabState Shared with the [SyncListTabFab] the host places in its scaffold when [isInCloudDrive] is true.
 */
@Composable
fun SyncListRoute(
    syncPermissionsManager: SyncPermissionsManager,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    onOpenMegaFolderClicked: (Long) -> Unit,
    onStalledIssueMoreClicked: (issueId: String) -> Unit,
    isInCloudDrive: Boolean = false,
    onSyncSettingsClicked: (() -> Unit)? = null,
    selectedChip: SyncChip = SyncChip.SYNC_FOLDERS,
    fabState: SyncListFabState = rememberSyncListFabState(),
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val viewModelStoreOwner = syncListViewModelStoreOwner()

    SyncListRoute(
        syncPermissionsManager = syncPermissionsManager,
        onSyncFolderClicked = onSyncFolderClicked,
        onBackupFolderClicked = onBackupFolderClicked,
        onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
        onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
        syncFoldersViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
        syncStalledIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
        syncSolvedIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
        syncIssueNotificationViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
        onSyncSettingsClicked = onSyncSettingsClicked,
        isInCloudDrive = isInCloudDrive,
        viewModel = hiltViewModel(),
        selectedChip = selectedChip,
        onOpenMegaFolderClicked = onOpenMegaFolderClicked,
        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
        fabState = fabState,
        onFabExpanded = onFabExpanded,
        onStalledIssueMoreClicked = onStalledIssueMoreClicked,
    )
}

/**
 * Owner the sync list view models are scoped to. [SyncListTabFab] resolves the same owner, so the
 * FAB a host places in its scaffold reads the [SyncFoldersViewModel] of the route beside it.
 */
@Composable
internal fun syncListViewModelStoreOwner(): ViewModelStoreOwner =
    LocalContext.current.findFragmentActivity()
        ?: checkNotNull(LocalViewModelStoreOwner.current)

@Composable
internal fun SyncListRoute(
    syncPermissionsManager: SyncPermissionsManager,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncIssueNotificationViewModel: SyncIssueNotificationViewModel,
    onStalledIssueMoreClicked: (issueId: String) -> Unit,
    onSyncSettingsClicked: (() -> Unit)? = null,
    isInCloudDrive: Boolean = false,
    viewModel: SyncListViewModel = hiltViewModel(),
    selectedChip: SyncChip = SyncChip.SYNC_FOLDERS,
    fabState: SyncListFabState = rememberSyncListFabState(),
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stalledIssueState by syncStalledIssuesViewModel.state.collectAsStateWithLifecycle()
    val syncFoldersState by syncFoldersViewModel.uiState.collectAsStateWithLifecycle()
    val solvedIssuesState by syncSolvedIssuesViewModel.state.collectAsStateWithLifecycle()
    val notificationState by syncIssueNotificationViewModel.state.collectAsStateWithLifecycle()

    val chipContent: @Composable (SyncChip, () -> Unit) -> Unit = { chip, onIssuesInfoClicked ->
        SelectedChipScreen(
            onAddNewSyncClicked = onSyncFolderClicked,
            onAddNewBackupClicked = onBackupFolderClicked,
            onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
            onOpenMegaFolderClicked = onOpenMegaFolderClicked,
            onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
            moreClicked = { stalledIssueItem -> onStalledIssueMoreClicked(stalledIssueItem.id) },
            issuesInfoClicked = onIssuesInfoClicked,
            checkedChip = chip,
            syncFoldersUiState = syncFoldersState,
            stalledIssues = stalledIssueState.stalledIssues,
            solvedIssues = solvedIssuesState.solvedIssues,
            onFoldersAction = syncFoldersViewModel::handleAction,
            deviceName = state.deviceName,
        )
    }

    if (isInCloudDrive) {
        SyncListTabContent(
            syncFoldersUiState = syncFoldersState,
            syncStalledIssuesState = stalledIssueState,
            syncSolvedIssuesState = solvedIssuesState,
            syncNotificationState = notificationState,
            stalledIssuesCount = state.stalledIssuesCount,
            syncPermissionsManager = syncPermissionsManager,
            onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
            onDismissNotification = syncIssueNotificationViewModel::dismissNotification,
            onSyncRefresh = syncFoldersViewModel::onSyncRefresh,
            chipContent = chipContent,
            fabState = fabState,
            selectedChip = selectedChip,
        )
    } else {
        SyncListScreen(
            syncFoldersUiState = syncFoldersState,
            syncStalledIssuesState = stalledIssueState,
            syncSolvedIssuesState = solvedIssuesState,
            syncNotificationState = notificationState,
            stalledIssuesCount = state.stalledIssuesCount,
            onSyncFolderClicked = { onSyncFolderClicked() },
            onBackupFolderClicked = { onBackupFolderClicked() },
            syncPermissionsManager = syncPermissionsManager,
            actions = listOfNotNull(onSyncSettingsClicked?.let { CommonMenuAction.Settings }),
            onActionPressed = {
                when (it) {
                    is CommonMenuAction.Settings -> onSyncSettingsClicked?.invoke()
                }
            },
            onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
            onDismissNotification = syncIssueNotificationViewModel::dismissNotification,
            onSyncRefresh = syncFoldersViewModel::onSyncRefresh,
            title = state.deviceName,
            chipContent = chipContent,
            selectedChip = selectedChip,
            onFabExpanded = onFabExpanded,
        )
    }

    val resources = LocalResources.current
    val snackBarHostState = LocalSnackBarHostState.current
    val snackbarScope = rememberCoroutineScope()
    EventEffect(
        stalledIssueState.snackbarMessageContent,
        onConsumed = {}
    ) { content ->
        syncStalledIssuesViewModel.handleAction(SyncListAction.SnackBarShown)
        snackbarScope.launch {
            snackBarHostState?.showAutoDurationSnackbar(
                resources.getString(content)
            )
        }
    }

}

@Composable
private fun SelectedChipScreen(
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    issuesInfoClicked: () -> Unit,
    checkedChip: SyncChip,
    syncFoldersUiState: SyncFoldersUiState,
    stalledIssues: List<StalledIssueUiItem>,
    solvedIssues: List<SolvedIssueUiItem>,
    onFoldersAction: (SyncFoldersAction) -> Unit,
    deviceName: String,
) {
    when (checkedChip) {
        SYNC_FOLDERS -> {
            SyncFoldersRoute(
                onAddNewSyncClicked = onAddNewSyncClicked,
                onAddNewBackupClicked = onAddNewBackupClicked,
                onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
                issuesInfoClicked = issuesInfoClicked,
                onAction = onFoldersAction,
                uiState = syncFoldersUiState,
                deviceName = deviceName,
                onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
            )
        }

        STALLED_ISSUES -> {
            StalledIssuesScreen(
                stalledIssues = stalledIssues,
                moreClicked = moreClicked,
            )
        }

        SOLVED_ISSUES -> {
            SyncSolvedIssuesScreen(solvedIssues = solvedIssues)
        }
    }
}
