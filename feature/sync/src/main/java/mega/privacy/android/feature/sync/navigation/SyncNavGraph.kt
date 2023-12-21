package mega.privacy.android.feature.sync.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
import mega.privacy.mobile.analytics.event.AndroidSyncFABButtonEvent
import mega.privacy.mobile.analytics.event.AndroidSyncGetStartedButtonEvent

const val syncRoute = "sync"

private const val syncEmptyRoute = "sync/empty"
private const val syncNewFolderRoute = "sync/new-folder"
private const val syncMegaPicker = "sync/mega-picker"
private const val syncList = "sync/list"

internal fun NavGraphBuilder.syncNavGraph(
    showOnboardingScreen: Boolean,
    navController: NavController,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager
) {
    navigation(
        startDestination = if (showOnboardingScreen) {
            syncEmptyRoute
        } else {
            syncList
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
                }, openNextScreen = {
                    navController.navigate(syncList)
                }, onBackClicked = {
                    navController.popBackStack()
                }
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
        composable(route = syncList) {
            SyncListRoute(
                hiltViewModel(),
                syncPermissionsManager,
                addFolderClicked = {
                    Analytics.tracker.trackEvent(AndroidSyncFABButtonEvent)
                    navController.navigate(syncNewFolderRoute)
                }
            )
        }
    }
}