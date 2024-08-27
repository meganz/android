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
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.mapper.SyncChipValueMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
import mega.privacy.mobile.analytics.event.AndroidSyncFABButtonEvent
import mega.privacy.mobile.analytics.event.AndroidSyncGetStartedButtonEvent
import timber.log.Timber

/**
 * Route ro the Sync feature
 */
const val syncRoute = "sync"

/**
 * Route to the Sync list
 */
const val syncListRoute = "$syncRoute/list"

/**
 * Route to the onboarding screen
 */
private const val syncEmptyRoute = "$syncRoute/empty"

/**
 * Route to the add new sync screen
 */
private const val syncNewFolderRoute = "$syncRoute/new-folder"

/**
 * Route to the MEGA folder picker screen
 */
private const val syncMegaPicker = "$syncRoute/mega-picker"

internal fun NavGraphBuilder.syncNavGraph(
    navController: NavController,
    fileTypeIconMapper: FileTypeIconMapper,
    syncChipValueMapper: SyncChipValueMapper,
    syncPermissionsManager: SyncPermissionsManager,
    openUpgradeAccountPage: () -> Unit,
    title: String? = null,
    openNewSync: Boolean = false,
) {
    navigation(
        startDestination = when {
            openNewSync -> syncNewFolderRoute
            else -> syncListRoute
        },
        route = syncRoute
    ) {
        composable(route = syncEmptyRoute) {
            Analytics.tracker.trackEvent(AddSyncScreenEvent)
            SyncEmptyScreen {
                Analytics.tracker.trackEvent(AndroidSyncGetStartedButtonEvent)
                navController.navigate(syncNewFolderRoute)
            }
        }
        composable(route = syncNewFolderRoute) {
            SyncNewFolderScreenRoute(
                hiltViewModel(),
                syncPermissionsManager,
                openSelectMegaFolderScreen = {
                    navController.navigate(syncMegaPicker)
                },
                openNextScreen = {
                    navController.navigate(syncListRoute)
                },
                openUpgradeAccount = {
                    openUpgradeAccountPage()
                },
                onBackClicked = {
                    if (openNewSync) {
                        navController.navigate(syncListRoute) {
                            popUpTo(syncNewFolderRoute) {
                                inclusive = true
                            }
                        }
                    } else {
                        navController.popBackStack()
                    }
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
                uriPattern = "https://mega.nz/$syncListRoute/{selectedChip}"
                action = Intent.ACTION_VIEW
            }),
            arguments = listOf(
                navArgument("selectedChip") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { navBackStackEntry ->
            val selectedChip =
                syncChipValueMapper(navBackStackEntry.arguments?.getInt("selectedChip", 0) ?: 0)
            Timber.d("Selected Chip = $selectedChip")

            val fragmentActivity = LocalContext.current.findFragmentActivity()
            val viewModelStoreOwner =
                fragmentActivity ?: checkNotNull(LocalViewModelStoreOwner.current)

            SyncListRoute(
                hiltViewModel(),
                syncPermissionsManager,
                addFolderClicked = {
                    Analytics.tracker.trackEvent(AndroidSyncFABButtonEvent)
                    navController.navigate(syncNewFolderRoute)
                },
                onOpenUpgradeAccountClicked = { openUpgradeAccountPage() },
                title = title,
                syncFoldersViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                syncStalledIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                syncSolvedIssuesViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner),
                selectedChip = selectedChip,
            )
        }
    }
}