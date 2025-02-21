package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLatestModifiedTimeEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLocalFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseRemoteFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncClearResolvedIssuesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMergeFoldersEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesAndRemoveRestEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRenameAllItemsEvent

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
 */
@Composable
fun SyncListRoute(
    syncPermissionsManager: SyncPermissionsManager,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: () -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    isInCloudDrive: Boolean = false,
    selectedChip: SyncChip = SyncChip.SYNC_FOLDERS,
    onFabExpanded: (Boolean) -> Unit = {},
    onOpenMegaFolderClicked: (Long) -> Unit,
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
        isInCloudDrive = isInCloudDrive,
        viewModel = hiltViewModel(),
        selectedChip = selectedChip,
        onOpenMegaFolderClicked = onOpenMegaFolderClicked,
        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
        onFabExpanded = onFabExpanded,
    )
}

@Composable
internal fun SyncListRoute(
    syncPermissionsManager: SyncPermissionsManager,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: () -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    isInCloudDrive: Boolean = false,
    viewModel: SyncListViewModel = hiltViewModel(),
    selectedChip: SyncChip = SyncChip.SYNC_FOLDERS,
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackBarHostState = remember { SnackbarHostState() }

    val message = state.snackbarMessage?.let {
        stringResource(id = it)
    }

    SyncListScreen(
        isInCloudDrive = isInCloudDrive,
        stalledIssuesCount = state.stalledIssuesCount,
        onOpenMegaFolderClicked = onOpenMegaFolderClicked,
        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
        onSyncFolderClicked = { onSyncFolderClicked() },
        onBackupFolderClicked = { onBackupFolderClicked() },
        actionSelected = { item, selectedAction ->
            when (selectedAction.resolutionActionType) {
                StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> {
                    Analytics.tracker.trackEvent(AndroidSyncRenameAllItemsEvent)
                }

                StalledIssueResolutionActionType.REMOVE_DUPLICATES -> {
                    Analytics.tracker.trackEvent(AndroidSyncRemoveDuplicatesEvent)
                }

                StalledIssueResolutionActionType.MERGE_FOLDERS -> {
                    Analytics.tracker.trackEvent(AndroidSyncMergeFoldersEvent)
                }

                StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST -> {
                    Analytics.tracker.trackEvent(AndroidSyncRemoveDuplicatesAndRemoveRestEvent)
                }

                StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE -> {
                    Analytics.tracker.trackEvent(AndroidSyncChooseLocalFileEvent)
                }

                StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE -> {
                    Analytics.tracker.trackEvent(AndroidSyncChooseRemoteFileEvent)
                }

                StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> {
                    Analytics.tracker.trackEvent(AndroidSyncChooseLatestModifiedTimeEvent)
                }

                else -> {

                }
            }
            viewModel.handleAction(
                SyncListAction.ResolveStalledIssue(item, selectedAction)
            )
        },
        snackBarHostState = snackBarHostState,
        syncPermissionsManager = syncPermissionsManager,
        actions = prepareMenuActions(state),
        onActionPressed = {
            when (it) {
                is SyncListMenuAction.ClearSyncOptions -> {
                    Analytics.tracker.trackEvent(AndroidSyncClearResolvedIssuesEvent)
                    viewModel.onClearSyncOptionsPressed()
                }
            }
        },
        onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
        onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
        title = state.deviceName,
        syncFoldersViewModel = syncFoldersViewModel,
        syncStalledIssuesViewModel = syncStalledIssuesViewModel,
        syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
        selectedChip = selectedChip,
        onFabExpanded = onFabExpanded,
    )

    LaunchedEffect(key1 = state.snackbarMessage) {
        message?.let {
            snackBarHostState.showAutoDurationSnackbar(it)
            viewModel.handleAction(SyncListAction.SnackBarShown)
        }
    }

}

private fun prepareMenuActions(state: SyncListState): List<MenuAction> {
    val menuActionList = mutableListOf<MenuAction>()
    if (state.shouldShowCleanSolvedIssueMenuItem) {
        menuActionList.add(SyncListMenuAction.ClearSyncOptions)
    }
    return menuActionList
}
