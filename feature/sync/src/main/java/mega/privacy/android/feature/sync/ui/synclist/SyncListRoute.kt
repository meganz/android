package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
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
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val fragmentActivity = LocalContext.current.findFragmentActivity()
    val viewModelStoreOwner =
        fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

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
        onFabExpanded = onFabExpanded,
        onStalledIssueMoreClicked = onStalledIssueMoreClicked,
    )
}

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
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stalledIssueState by syncStalledIssuesViewModel.state.collectAsStateWithLifecycle()

    SyncListScreen(
        isInCloudDrive = isInCloudDrive,
        stalledIssuesCount = state.stalledIssuesCount,
        onOpenMegaFolderClicked = onOpenMegaFolderClicked,
        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
        onSyncFolderClicked = { onSyncFolderClicked() },
        onBackupFolderClicked = { onBackupFolderClicked() },
        syncPermissionsManager = syncPermissionsManager,
        actions = listOfNotNull(onSyncSettingsClicked?.let { CommonMenuAction.Settings }),
        onActionPressed = {
            when (it) {
                is CommonMenuAction.Settings -> onSyncSettingsClicked?.invoke()
            }
        },
        onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
        onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
        title = state.deviceName,
        syncFoldersViewModel = syncFoldersViewModel,
        syncStalledIssuesViewModel = syncStalledIssuesViewModel,
        syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
        syncIssueNotificationViewModel = syncIssueNotificationViewModel,
        selectedChip = selectedChip,
        onFabExpanded = onFabExpanded,
        onStalledIssueMoreClicked = onStalledIssueMoreClicked,
    )

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
