package mega.privacy.android.feature.sync.navigation

import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
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
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
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
private const val syncNewFolderRoute =
    "$syncRoute/new-folder/{syncType}/{isFromManagerActivity}/{remoteFolderHandle}/{remoteFolderName}"

/**
 * Gets the route to the add new sync screen and allow set some allowed possible parameters through it
 *
 * @param syncType The [SyncType] for the new sync
 * @param isFromManagerActivity Indicates if the new sync is being set from Manager Activity. False by default
 * @param remoteFolderHandle The MEGA folder handle for the new sync
 * @param remoteFolderName The MEGA folder name for the new sync
 * @return The route to the the add new sync screen with the allowed possible parameters
 */
fun getSyncNewFolderRoute(
    syncType: SyncType,
    isFromManagerActivity: Boolean = false,
    remoteFolderHandle: Long? = null,
    remoteFolderName: String? = null,
): String {
    val syncTypeJson = GsonBuilder().create().toJson(syncType)
    return syncNewFolderRoute
        .replace(oldValue = "{syncType}", newValue = syncTypeJson)
        .replace(oldValue = "{isFromManagerActivity}", newValue = isFromManagerActivity.toString())
        .replace(oldValue = "{remoteFolderHandle}", newValue = remoteFolderHandle.toString())
        .replace(oldValue = "{remoteFolderName}", newValue = remoteFolderName.toString())
}

/**-
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
    shouldNavigateToSyncList: Boolean = true,
) {
    navigation(
        startDestination = if (shouldNavigateToSyncList) syncListRoute else syncNewFolderRoute,
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
                },
                navArgument("isFromManagerActivity") {
                    nullable = false
                },
                navArgument("remoteFolderHandle") {
                    nullable = true
                },
                navArgument("remoteFolderName") {
                    nullable = true
                }
            )
        ) { navBackStackEntry ->
            val context = LocalContext.current
            val syncType = GsonBuilder().create()
                .fromJson(navBackStackEntry.arguments?.getString("syncType"), SyncType::class.java)
                ?: SyncType.TYPE_TWOWAY
            val isFromManagerActivity =
                navBackStackEntry.arguments?.getString("isFromManagerActivity")?.toBoolean() == true
            val remoteFolderHandle =
                navBackStackEntry.arguments?.getString("remoteFolderHandle")?.toLongOrNull()
            val remoteFolderName = navBackStackEntry.arguments?.getString("remoteFolderName")

            val viewModel =
                hiltViewModel<SyncNewFolderViewModel, SyncNewFolderViewModel.SyncNewFolderViewModelFactory> { factory ->
                    factory.create(
                        syncType = syncType,
                        remoteFolderHandle = remoteFolderHandle,
                        remoteFolderName = remoteFolderName
                    )
                }

            val launcher = launchFolderPicker(
                onFolderSelected = { uri ->
                    runCatching {
                        val documentFile = if (DocumentsContract.isTreeUri(uri)) {
                            DocumentFile.fromTreeUri(context, uri)
                        } else {
                            DocumentFile.fromFile(uri.toFile())
                        }
                        documentFile?.let {
                            viewModel.handleAction(
                                SyncNewFolderAction.LocalFolderSelected(
                                    documentFile
                                )
                            )
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                },
            )

            SyncNewFolderScreenRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = {
                    navController.navigate(syncMegaPicker)
                },
                openNextScreen = {
                    if (shouldNavigateToSyncList) {
                        navFromNewFolderRouteToListRoute()
                    } else {
                        if (!navController.popBackStack()) {
                            context.findFragmentActivity()?.finish()
                        }
                    }
                },
                openUpgradeAccount = {
                    openUpgradeAccountPage()
                },
                onBackClicked = {
                    if (shouldNavigateToSyncList && isFromManagerActivity.not()) {
                        navFromNewFolderRouteToListRoute()
                    } else {
                        if (!navController.popBackStack()) {
                            context.findFragmentActivity()?.finish()
                        }
                    }
                },
                onSelectFolder = {
                    launcher.launch(null)
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

            val fragmentActivity = LocalContext.current.findFragmentActivity()
            val context = LocalContext.current
            val viewModelStoreOwner =
                fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

            SyncListRoute(
                viewModel = hiltViewModel(),
                syncPermissionsManager = syncPermissionsManager,
                onSyncFolderClicked = {
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
                syncIssueNotificationViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                selectedChip = selectedChip,
                onOpenMegaFolderClicked = { handle ->
                    megaNavigator.openSyncMegaFolder(context, handle)
                },
                onCameraUploadsSettingsClicked = { megaNavigator.openSettingsCameraUploads(context) },
            )
        }
    }
}

internal fun NavGraphBuilder.syncStopBackupNavGraph(
    navController: NavController,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
) {
    navigation(
        startDestination = stopBackupMegaPicker,
        route = syncRoute,
    ) {
        composable(route = stopBackupMegaPicker) {
            val context = LocalContext.current
            MegaPickerRoute(
                hiltViewModel(),
                syncPermissionsManager,
                folderSelected = {
                    if (!navController.popBackStack()) {
                        context.findFragmentActivity()?.finish()
                    }
                },
                backClicked = {
                    if (!navController.popBackStack()) {
                        context.findFragmentActivity()?.finish()
                    }
                },
                fileTypeIconMapper = fileTypeIconMapper,
                isStopBackupMegaPicker = true,
            )
        }
    }
}
