package mega.privacy.android.feature.sync.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.navigation
import com.google.accompanist.navigation.animation.composable
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute

const val syncRoute = "sync"

private const val syncEmptyRoute = "sync/empty"
private const val syncNewFolderRoute = "sync/new-folder"
private const val syncMegaPicker = "sync/mega-picker"
private const val syncList = "sync/list"

@OptIn(ExperimentalAnimationApi::class)
internal fun NavGraphBuilder.syncNavGraph(navController: NavController) {
    navigation(startDestination = syncEmptyRoute, route = syncRoute) {
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
                })
        }
        composable(route = syncMegaPicker) {
            MegaPickerRoute(viewModel = hiltViewModel(), folderSelected = {
                navController.popBackStack()
            }, backClicked = {
                navController.popBackStack()
            }
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