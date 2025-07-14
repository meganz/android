package mega.privacy.android.feature.sync.navigation

import android.content.Intent
import android.os.Parcelable
import android.provider.DocumentsContract
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
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
@Serializable
data object Sync

/**
 * Gets the route to the Sync feature
 */
fun getSyncRoute() = "Sync"

/**
 * Route to the Sync list
 */
@Serializable
data class SyncList(val selectedChip: SyncChip = SyncChip.SYNC_FOLDERS)

/**
 * Gets the route to the Sync list and allow set some allowed possible parameters through it
 *
 * @param selectedChip [SyncChip] to set as selected in the Sync list
 * @return The route to the Sync list with the allowed possible parameters
 */
fun getSyncListRoute(selectedChip: SyncChip = SyncChip.SYNC_FOLDERS) =
    "${getSyncRoute()}/SyncList?selectedChip=$selectedChip"

/**
 * Route to the onboarding screen
 */
@Serializable
data object SyncEmptyRoute

/**
 * Route to the add new sync screen
 */
@Parcelize
@Serializable
data class SyncNewFolder(
    val syncType: SyncType = SyncType.TYPE_TWOWAY,
    val isFromManagerActivity: Boolean = false,
    val remoteFolderHandle: Long? = null,
    val remoteFolderName: String? = null,
) : Parcelable

/**
 * Route to the MEGA folder picker screen
 */
@Serializable
data object SyncMegaPicker

/**
 * Route to the Stop Backup MEGA folder picker screen
 */
@Serializable
data object StopBackupMegaPicker

internal fun NavGraphBuilder.syncNavGraph(
    navController: NavController,
    megaNavigator: MegaNavigator,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    openUpgradeAccountPage: () -> Unit,
    startDestination: Any,
    shouldNavigateToSyncList: Boolean = true,
) {
    navigation<Sync>(
        startDestination = startDestination,//if (shouldNavigateToSyncList) SyncList() else SyncNewFolder(),
    ) {

        /**
         * Method to specifically navigate from syncNewFolderRoute to syncListRoute
         * It avoids duplicated navigation due the use of shortcuts, deep links, etc.
         */
        fun navFromNewFolderRouteToListRoute() {
            navController.navigate(
                SyncList()
            ) {
                popUpTo(SyncNewFolder()) {
                    inclusive = true
                }
            }
            if (navController.previousBackStackEntry?.destination?.route == navController.currentBackStackEntry?.destination?.route) {
                navController.popBackStack()
            }
        }

        composable<SyncEmptyRoute> {
            Analytics.tracker.trackEvent(AddSyncScreenEvent)
            SyncEmptyScreen {
                Analytics.tracker.trackEvent(AndroidSyncGetStartedButtonEvent)
                navController.navigate(SyncNewFolder())
            }
        }
        composable<SyncNewFolder>(
            deepLinks = listOf(
                navDeepLink<SyncNewFolder>(
                    basePath = "https://mega.nz/${getSyncRoute()}/SyncNewFolder",
                ) {
                    action = Intent.ACTION_VIEW
                }),
        ) { navBackStackEntry ->
            val routeArg = navBackStackEntry.toRoute<SyncNewFolder>()

            val context = LocalContext.current
            val syncType = routeArg.syncType
            val isFromManagerActivity = routeArg.isFromManagerActivity
            val remoteFolderHandle = routeArg.remoteFolderHandle
            val remoteFolderName = routeArg.remoteFolderName

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
                    navController.navigate(SyncMegaPicker)
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
        composable<SyncMegaPicker> {
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
        composable<StopBackupMegaPicker> {
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
            val fragmentActivity = LocalContext.current.findFragmentActivity()
            val context = LocalContext.current
            val viewModelStoreOwner =
                fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

            SyncListRoute(
                viewModel = hiltViewModel(),
                syncPermissionsManager = syncPermissionsManager,
                onSyncFolderClicked = {
                    navController.navigate(
                        SyncNewFolder(syncType = SyncType.TYPE_TWOWAY)
                    )
                },
                onBackupFolderClicked = {
                    navController.navigate(
                        SyncNewFolder(syncType = SyncType.TYPE_BACKUP)
                    )
                },
                onSelectStopBackupDestinationClicked = { navController.navigate(StopBackupMegaPicker) },
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
    navigation<Sync>(
        startDestination = StopBackupMegaPicker,
    ) {
        composable<StopBackupMegaPicker> {
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
