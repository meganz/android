package mega.privacy.android.feature.sync.navigation

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
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
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
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
    "$syncRoute/list?deviceName={deviceName}&selectedChip={selectedChip}"

/**
 * Gets the route to the Sync list and allow set some allowed possible parameters through it
 *
 * @param deviceName Name of the device
 * @param selectedChip [SyncChip] to set as selected in the Sync list
 * @return The route to the Sync list with the allowed possible parameters
 */
fun getSyncListRoute(deviceName: String? = null, selectedChip: SyncChip? = null): String {
    var finalRoute = syncListRoute
    deviceName?.let { deviceNameValue ->
        finalRoute = finalRoute.replace(oldValue = "{deviceName}", newValue = deviceNameValue)
    }
    selectedChip?.let { selectedChipValue ->
        val selectedChipJson = GsonBuilder().create().toJson(selectedChipValue)
        finalRoute = finalRoute.replace(oldValue = "{selectedChip}", newValue = selectedChipJson)
    }
    return finalRoute
}

/**
 * Route to the onboarding screen
 */
private const val syncEmptyRoute = "$syncRoute/empty"

/**
 * Route to the add new sync screen
 */
private const val syncNewFolderRoute = "$syncRoute/new-folder/{syncType}?deviceName={deviceName}"

/**
 * Gets the route to the add new sync screen and allow set some allowed possible parameters through it
 *
 * @param syncType The [SyncType] for the new sync
 * @param deviceName Name of the device
 * @return The route to the the add new sync screen with the allowed possible parameters
 */
fun getSyncNewFolderRoute(syncType: SyncType, deviceName: String? = null): String {
    val syncTypeJson = GsonBuilder().create().toJson(syncType)
    var finalRoute = syncNewFolderRoute.replace(oldValue = "{syncType}", newValue = syncTypeJson)
    deviceName?.let { deviceNameValue ->
        finalRoute = finalRoute.replace(oldValue = "{deviceName}", newValue = deviceNameValue)
    }
    return finalRoute
}

/**
 * Route to the MEGA folder picker screen
 */
private const val syncMegaPicker = "$syncRoute/mega-picker"

internal fun NavGraphBuilder.syncNavGraph(
    navController: NavController,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    openUpgradeAccountPage: () -> Unit,
) {
    var deviceName: String? = null

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
                getSyncListRoute(deviceName = deviceName)
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
                navArgument("deviceName") {
                    type = NavType.StringType
                    nullable = true
                },
            )
        ) { navBackStackEntry ->
            val syncType = GsonBuilder().create()
                .fromJson(navBackStackEntry.arguments?.getString("syncType"), SyncType::class.java)
                ?: SyncType.TYPE_TWOWAY
            Timber.d("Sync Type = $syncType")
            deviceName =
                navBackStackEntry.arguments?.getString("deviceName", null)?.replace("+", " ")

            SyncNewFolderScreenRoute(
                hiltViewModel<SyncNewFolderViewModel, SyncNewFolderViewModel.SyncNewFolderViewModelFactory> { factory ->
                    factory.create(syncType = syncType, deviceName = deviceName.toString())
                },
                syncPermissionsManager,
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
            )
        }
        composable(route = syncMegaPicker) {
            MegaPickerRoute(
                hiltViewModel(),
                syncPermissionsManager,
                folderSelected = {
                    navController.popBackStack()
                }, backClicked = {
                    navController.popBackStack()
                },
                fileTypeIconMapper = fileTypeIconMapper
            )
        }
        composable(
            route = syncListRoute,
            deepLinks = listOf(navDeepLink {
                uriPattern = "https://mega.nz/$syncListRoute"
                action = Intent.ACTION_VIEW
            }),
            arguments = listOf(
                navArgument("deviceName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("selectedChip") {
                    nullable = true
                }
            )
        ) { navBackStackEntry ->
            deviceName =
                navBackStackEntry.arguments?.getString("deviceName", null)?.replace("+", " ")
            val selectedChip = GsonBuilder().create().fromJson(
                navBackStackEntry.arguments?.getString("selectedChip"),
                SyncChip::class.java
            ) ?: SyncChip.SYNC_FOLDERS
            Timber.d("Selected Chip = $selectedChip")

            val fragmentActivity = LocalContext.current.findFragmentActivity()
            val viewModelStoreOwner =
                fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

            SyncListRoute(
                hiltViewModel(),
                syncPermissionsManager,
                addFolderClicked = {
                    Analytics.tracker.trackEvent(AndroidSyncFABButtonEvent)
                    navController.navigate(
                        getSyncNewFolderRoute(
                            syncType = SyncType.TYPE_TWOWAY,
                            deviceName = deviceName
                        )
                    )
                },
                onOpenUpgradeAccountClicked = { openUpgradeAccountPage() },
                title = deviceName,
                syncFoldersViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                syncStalledIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                syncSolvedIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                selectedChip = selectedChip,
            )
        }
    }
}