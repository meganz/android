package mega.privacy.android.feature.sync.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.google.gson.GsonBuilder
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
import mega.privacy.mobile.analytics.event.AndroidSyncFABButtonEvent
import mega.privacy.mobile.analytics.event.AndroidSyncGetStartedButtonEvent
import timber.log.Timber

/**
 * Route ro the Sync feature
 */
private const val syncRoute = "sync"

/**
 * Gets the route to the Sync feature
 */
fun getSyncRoute() = syncRoute

/**
 * Route to the Sync list
 */
private const val syncListRoute =
    "$syncRoute/list?selectedChip={selectedChip}"

/**
 * Gets the route to the Sync list and allow set some allowed possible parameters through it
 *
 * @param selectedChip [SyncChip] to set as selected in the Sync list
 * @return The route to the Sync list with the allowed possible parameters
 */
fun getSyncListRoute(selectedChip: SyncChip = SyncChip.SYNC_FOLDERS): String {
    val selectedChipJson = GsonBuilder().create().toJson(selectedChip)
    return syncListRoute.replace(oldValue = "{selectedChip}", newValue = selectedChipJson)
}

/**
 * Route to the onboarding screen
 */
private const val syncEmptyRoute = "$syncRoute/empty"

/**
 * Route to the add new sync screen
 */
private const val syncNewFolderRoute = "$syncRoute/new-folder/{syncType}"

/**
 * Gets the route to the add new sync screen and allow set some allowed possible parameters through it
 *
 * @param syncType The [SyncType] for the new sync
 * @return The route to the the add new sync screen with the allowed possible parameters
 */
fun getSyncNewFolderRoute(syncType: SyncType): String {
    val syncTypeJson = GsonBuilder().create().toJson(syncType)
    return syncNewFolderRoute.replace(oldValue = "{syncType}", newValue = syncTypeJson)
}

/**
 * Route to the MEGA folder picker screen
 */
private const val syncMegaPicker = "$syncRoute/mega-picker"

/**
 * Route to the Stop Backup MEGA folder picker screen
 */
private const val stopBackupMegaPicker = "$syncRoute/stop-backup-mega-picker"

internal fun NavGraphBuilder.syncNavGraph(
    navController: NavController,
    megaNavigator: MegaNavigator,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    openUpgradeAccountPage: () -> Unit,
) {
    navigation(
        startDestination = syncListRoute,
        route = syncRoute,
    ) {

        /**
         * Method to specifically navigate from syncNewFolderRoute to syncListRoute
         * It avoids duplicated navigation due the use of shortcuts, deep links, etc.
         */
        fun navFromNewFolderRouteToListRoute() {
            navController.navigate(
                getSyncListRoute()
            ) {
                popUpTo(syncNewFolderRoute) {
                    inclusive = true
                }
            }
            if (navController.previousBackStackEntry?.destination?.route == navController.currentBackStackEntry?.destination?.route) {
                navController.popBackStack()
            }
        }

        composable(route = syncEmptyRoute) {
            Analytics.tracker.trackEvent(AddSyncScreenEvent)
            SyncEmptyScreen {
                Analytics.tracker.trackEvent(AndroidSyncGetStartedButtonEvent)
                navController.navigate(syncNewFolderRoute)
            }
        }
        composable(
            route = syncNewFolderRoute,
            deepLinks = listOf(navDeepLink {
                uriPattern =
                    "https://mega.nz/$syncNewFolderRoute"
                action = Intent.ACTION_VIEW
            }),
            arguments = listOf(
                navArgument("syncType") {
                    nullable = false
                }
            )
        ) { navBackStackEntry ->
            val context = LocalContext.current
            val syncType = GsonBuilder().create()
                .fromJson(navBackStackEntry.arguments?.getString("syncType"), SyncType::class.java)
                ?: SyncType.TYPE_TWOWAY
            Timber.d("Sync Type = $syncType")

            val viewModel =
                hiltViewModel<SyncNewFolderViewModel, SyncNewFolderViewModel.SyncNewFolderViewModelFactory> { factory ->
                    factory.create(syncType = syncType)
                }

            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_OK) {
                        val uri = it.data?.data
                        if (uri != null) {
                            viewModel.handleAction(SyncNewFolderAction.LocalFolderSelected(uri))
                        }
                    }
                }

            SyncNewFolderScreenRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = {
                    navController.navigate(syncMegaPicker)
                },
                openNextScreen = {
                    navFromNewFolderRouteToListRoute()
                },
                openUpgradeAccount = {
                    openUpgradeAccountPage()
                },
                onBackClicked = {
                    navFromNewFolderRouteToListRoute()
                },
                onSelectFolder = {
                    megaNavigator.openInternalFolderPicker(
                        context = context,
                        launcher,
                        Uri.fromFile(Environment.getExternalStorageDirectory())
                    )
                },
            )
        }
        composable(route = syncMegaPicker) {
            MegaPickerRoute(
                hiltViewModel(),
                syncPermissionsManager,
                folderSelected = {
                    navController.popBackStack()
                },
                backClicked = {
                    navController.popBackStack()
                },
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
        composable(route = stopBackupMegaPicker) {
            MegaPickerRoute(
                hiltViewModel(),
                syncPermissionsManager,
                folderSelected = {
                    navController.popBackStack()
                },
                backClicked = {
                    navController.popBackStack()
                },
                fileTypeIconMapper = fileTypeIconMapper,
                isStopBackupMegaPicker = true,
            )
        }
        composable(
            route = syncListRoute,
            deepLinks = listOf(navDeepLink {
                uriPattern = "https://mega.nz/$syncListRoute"
                action = Intent.ACTION_VIEW
            }),
            arguments = listOf(
                navArgument("selectedChip") {
                    nullable = true
                }
            )
        ) { navBackStackEntry ->
            val selectedChip = GsonBuilder().create().fromJson(
                navBackStackEntry.arguments?.getString("selectedChip"),
                SyncChip::class.java
            ) ?: SyncChip.SYNC_FOLDERS
            Timber.d("Selected Chip = $selectedChip")

            val fragmentActivity = LocalContext.current.findFragmentActivity()
            val context = LocalContext.current
            val viewModelStoreOwner =
                fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

            SyncListRoute(
                hiltViewModel(),
                syncPermissionsManager,
                onSyncFolderClicked = {
                    Analytics.tracker.trackEvent(AndroidSyncFABButtonEvent)
                    navController.navigate(
                        getSyncNewFolderRoute(syncType = SyncType.TYPE_TWOWAY)
                    )
                },
                onBackupFolderClicked = {
                    navController.navigate(
                        getSyncNewFolderRoute(syncType = SyncType.TYPE_BACKUP)
                    )
                },
                onSelectStopBackupDestinationClicked = { navController.navigate(stopBackupMegaPicker) },
                onOpenUpgradeAccountClicked = { openUpgradeAccountPage() },
                syncFoldersViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                syncStalledIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                syncSolvedIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                selectedChip = selectedChip,
                onOpenMegaFolderClicked = { handle ->
                    megaNavigator.openSyncMegaFolder(context, handle)
                },
            )
        }
    }
}
