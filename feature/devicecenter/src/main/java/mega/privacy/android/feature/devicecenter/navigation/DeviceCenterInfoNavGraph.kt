package mega.privacy.android.feature.devicecenter.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode

internal const val deviceCenterRoute = "device-center"

private const val deviceCenterInfoRoute = "device-center/info"

internal fun NavGraphBuilder.deviceCenterInfoNavGraph(
    navController: NavController,
    selectedItem: DeviceCenterUINode,
    onBackPressHandled: () -> Unit,
) {
    navigation(
        startDestination = deviceCenterInfoRoute,
        route = deviceCenterRoute
    ) {
        composable(route = deviceCenterInfoRoute) {
            DeviceCenterInfoScreenRoute(
                viewModel = hiltViewModel(),
                selectedItem = selectedItem,
                onBackPressHandled = {
                    onBackPressHandled()
                    navController.popBackStack()
                },
            )
        }
    }
}