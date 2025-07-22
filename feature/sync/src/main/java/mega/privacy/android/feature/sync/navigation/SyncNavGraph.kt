package mega.privacy.android.feature.sync.navigation

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncViewModel
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity


/**
 * Gets the route to the Sync feature
 */
fun getSyncRoute() = "Sync"

/**
 * Gets the route to the Sync list and allow set some allowed possible parameters through it
 *
 * @param selectedChip [SyncChip] to set as selected in the Sync list
 * @return The route to the Sync list with the allowed possible parameters
 */
fun getSyncListRoute(selectedChip: SyncChip = SyncChip.SYNC_FOLDERS) =
    "${getSyncRoute()}/SyncList?selectedChip=$selectedChip"


internal fun NavGraphBuilder.syncNavGraph(
    navController: NavController,
    megaNavigator: MegaNavigator,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    openUpgradeAccountPage: () -> Unit,
    shouldNavigateToSyncList: Boolean = true,
    context: Context,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncIssueNotificationViewModel: SyncIssueNotificationViewModel,
    settingsSyncViewModel: SettingsSyncViewModel,
) {

    syncEmptyDestination(onNavigateToNewFolder = { navController.navigate(SyncNewFolder()) })
    syncNewFolderDestination(
        syncPermissionsManager = syncPermissionsManager,
        navController = navController,
        shouldNavigateToSyncList = shouldNavigateToSyncList,
        openUpgradeAccountPage = openUpgradeAccountPage,
        popToSyncListView = {
            navController.navFromNewFolderRouteToListRoute()
        },
    )
    syncMegaPickerDestination(
        syncPermissionsManager = syncPermissionsManager,
        fileTypeIconMapper = fileTypeIconMapper,
        onNavigateBack = { navController.popBackStack() }
    )
    stopBackupMegaPickerDestination(
        syncPermissionsManager = syncPermissionsManager,
        fileTypeIconMapper = fileTypeIconMapper,
        onBackPressed = {
            if (!navController.popBackStack()) {
                context.findFragmentActivity()?.finish()
            }
        })
    syncListDestination(
        syncPermissionsManager = syncPermissionsManager,
        openUpgradeAccountPage = openUpgradeAccountPage,
        onOpenSyncFolder = { handle ->
            megaNavigator.openSyncMegaFolder(context, handle)
        },
        onNavigateToCameraUploadSettings = {
            megaNavigator.openSettingsCameraUploads(context)
        },
        onNavigateToNewSyncFolder = { type: SyncType ->
            navController.navigate(
                SyncNewFolder(syncType = type)
            )
        },
        onNavigateToStopBackupMegaPicker = {
            navController.navigate(StopBackupMegaPicker)
        },
        syncFoldersViewModel = syncFoldersViewModel,
        syncStalledIssuesViewModel = syncStalledIssuesViewModel,
        syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
        syncIssueNotificationViewModel = syncIssueNotificationViewModel,
        settingsSyncViewModel = settingsSyncViewModel
    )
}

/**
 * Method to specifically navigate from syncNewFolderRoute to syncListRoute
 * It avoids duplicated navigation due the use of shortcuts, deep links, etc.
 */
private fun NavController.navFromNewFolderRouteToListRoute() {
    navigate(SyncList()) {
        popUpTo(SyncNewFolder()) { inclusive = true }
    }
    if (previousBackStackEntry?.destination?.route == currentBackStackEntry?.destination?.route) {
        popBackStack()
    }
}
