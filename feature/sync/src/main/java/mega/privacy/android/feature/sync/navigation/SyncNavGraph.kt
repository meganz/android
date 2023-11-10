package mega.privacy.android.feature.sync.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute

const val syncRoute = "sync"

private const val syncEmptyRoute = "sync/empty"
private const val syncNewFolderRoute = "sync/new-folder"
private const val syncMegaPicker = "sync/mega-picker"
private const val syncList = "sync/list"

internal fun NavGraphBuilder.syncNavGraph(
    showOnboardingScreen: Boolean,
    navController: NavController,
    fileTypeIconMapper: FileTypeIconMapper
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
            SyncEmptyScreen {
                navController.navigate(syncNewFolderRoute)
            }
        }
        composable(route = syncNewFolderRoute) {
            SyncNewFolderScreenRoute(
                hiltViewModel(),
                SyncPermissionsManager(),
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
                SyncPermissionsManager(),
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
                addFolderClicked = {
                    navController.navigate(syncNewFolderRoute)
                }
            )
        }
    }
}