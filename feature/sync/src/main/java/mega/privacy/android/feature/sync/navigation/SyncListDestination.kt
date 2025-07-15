package mega.privacy.android.feature.sync.navigation

import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncViewModel
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel

/**
 * Route to the Sync list
 */
@Serializable
data class SyncList(val selectedChip: SyncChip = SyncChip.SYNC_FOLDERS)

internal fun NavGraphBuilder.syncListDestination(
    syncPermissionsManager: SyncPermissionsManager,
    openUpgradeAccountPage: () -> Unit,
    onOpenSyncFolder: (handle: Long) -> Unit,
    onNavigateToCameraUploadSettings: () -> Unit,
    onNavigateToNewSyncFolder: (SyncType) -> Unit,
    onNavigateToStopBackupMegaPicker: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncIssueNotificationViewModel: SyncIssueNotificationViewModel,
    settingsSyncViewModel: SettingsSyncViewModel,
) {
    composable<SyncList>(
        deepLinks = listOf(
            navDeepLink<SyncList>(
                basePath = "https://mega.nz/${getSyncRoute()}/SyncList",
            ) {
                action = Intent.ACTION_VIEW
            }),
    ) { navBackStackEntry ->
        val routeArg = navBackStackEntry.toRoute<SyncList>()
        val selectedChip = routeArg.selectedChip


        SyncListRoute(
            viewModel = hiltViewModel(),
            syncPermissionsManager = syncPermissionsManager,
            onSyncFolderClicked = { onNavigateToNewSyncFolder(SyncType.TYPE_TWOWAY) },
            onBackupFolderClicked = { onNavigateToNewSyncFolder(SyncType.TYPE_BACKUP) },
            onSelectStopBackupDestinationClicked = onNavigateToStopBackupMegaPicker,
            onOpenUpgradeAccountClicked = { openUpgradeAccountPage() },
            syncFoldersViewModel = syncFoldersViewModel,
            syncStalledIssuesViewModel = syncStalledIssuesViewModel,
            syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
            syncIssueNotificationViewModel = syncIssueNotificationViewModel,
            selectedChip = selectedChip,
            onOpenMegaFolderClicked = onOpenSyncFolder,
            onCameraUploadsSettingsClicked = onNavigateToCameraUploadSettings,
            settingsSyncViewModel = settingsSyncViewModel
        )
    }
}
