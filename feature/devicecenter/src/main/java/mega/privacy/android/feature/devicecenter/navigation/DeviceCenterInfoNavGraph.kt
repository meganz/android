package mega.privacy.android.feature.devicecenter.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode

@Serializable
data object DeviceCenter

@Serializable
data class DeviceCenterInfo(val deviceCenterNode: DeviceCenterUINode)

internal fun NavGraphBuilder.deviceCenterInfoNavGraph(
    navController: NavController,
    selectedItem: DeviceCenterUINode,
    onBackPressHandled: () -> Unit,
) {
    navigation<DeviceCenter>(
        startDestination = DeviceCenterInfo(selectedItem)
    ) {
        composable<DeviceCenterInfo> {
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