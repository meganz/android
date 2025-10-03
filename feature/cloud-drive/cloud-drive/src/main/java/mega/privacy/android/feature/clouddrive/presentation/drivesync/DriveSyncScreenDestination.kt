package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object DriveSync : NavKey

fun NavGraphBuilder.driveSyncScreen(
    navigationHandler: NavigationHandler,
    setNavigationVisibility: (Boolean) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    openSearch: (Boolean, Long, NodeSourceType) -> Unit,
) {
    composable<DriveSync> {
        val viewModel = hiltViewModel<DriveSyncViewModel>()
        val cloudDriveViewModel = hiltViewModel<CloudDriveViewModel>()
        DriveSyncScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            cloudDriveViewModel = cloudDriveViewModel,
            setNavigationItemVisibility = setNavigationVisibility,
            onTransfer = onTransfer,
            openSearch = openSearch
        )
    }
}