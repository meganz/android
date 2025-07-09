package mega.privacy.android.feature.devicecenter.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceUINode

@Serializable
data object DeviceCenter

@Serializable
data class DeviceCenterInfo(
    val name: String,
    val icon: Int,
    val rootHandle: Long? = null,
    val folders: List<Long>? = null,
) {
    fun isDevice() = folders != null

    companion object {
        fun from(uiNode: DeviceCenterUINode): DeviceCenterInfo {
            return DeviceCenterInfo(
                name = uiNode.name,
                icon = uiNode.icon.iconRes,
                rootHandle = (uiNode as? DeviceFolderUINode)?.rootHandle,
                folders = (uiNode as? DeviceUINode)?.folders?.map { it.rootHandle }
            )
        }
    }
}

internal fun NavGraphBuilder.deviceCenterInfoNavGraph(
    navController: NavController,
    selectedItem: DeviceCenterUINode,
    onBackPressHandled: () -> Unit,
) {
    navigation<DeviceCenter>(
        startDestination = DeviceCenterInfo.from(selectedItem)
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