package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.navigation.ManageSyncExclusionsNavKey
import mega.privacy.android.feature.sync.ui.synclist.SyncListFabState
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.SelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.destination.SyncSelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.SyncStalledIssueResolutionNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager

/**
 * The Syncs tab of the Drive screen, shared by [DriveSyncScreen] and [LegacyDriveSyncScreen] so a
 * change to it lands in both during the pagination flag window.
 */
@Composable
internal fun SyncsTab(
    navigationHandler: NavigationHandler,
    syncPermissionsManager: SyncPermissionsManager,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    fabState: SyncListFabState,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
) {
    val useCloudExplorerPicker by produceState(initialValue = false) {
        value = runCatching {
            getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)
        }.getOrDefault(false)
    }

    SyncListRoute(
        isInCloudDrive = true,
        onStalledIssueMoreClicked = { issueId ->
            navigationHandler.navigate(SyncStalledIssueResolutionNavKey(issueId = issueId))
        },
        syncPermissionsManager = syncPermissionsManager,
        onSyncFolderClicked = onSyncFolderClicked,
        onBackupFolderClicked = onBackupFolderClicked,
        onSelectStopBackupDestinationClicked = {
            navigationHandler.navigate(
                if (useCloudExplorerPicker) {
                    SelectStopBackupDestinationNavKey(folderName = it)
                } else {
                    SyncSelectStopBackupDestinationNavKey(folderName = it)
                }
            )
        },
        onOpenUpgradeAccountClicked = {
            navigationHandler.navigate(UpgradeAccountNavKey())
        },
        onOpenMegaFolderClicked = { handle ->
            navigationHandler.navigate(CloudDriveNavKey(nodeHandle = handle))
        },
        onExclusionsClicked = { syncId, localRoot ->
            navigationHandler.navigate(ManageSyncExclusionsNavKey(syncId, localRoot))
        },
        onCameraUploadsSettingsClicked = {
            navigationHandler.navigate(SettingsCameraUploadsNavKey)
        },
        fabState = fabState,
    )
}
