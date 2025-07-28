package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel

@Serializable
data object DriveSync : NavKey

fun NavGraphBuilder.driveSyncScreen(
    onNavigateToFolder: (NodeId) -> Unit,
    setNavigationVisibility: (Boolean) -> Unit,
) {
    composable<DriveSync> {
        val viewModel = hiltViewModel<DriveSyncViewModel>()
        val cloudDriveViewModel = hiltViewModel<CloudDriveViewModel>()
        DriveSyncScreen(
            viewModel = viewModel,
            cloudDriveViewModel = cloudDriveViewModel,
            onNavigateToFolder = onNavigateToFolder,
            setNavigationItemVisibility = setNavigationVisibility
        )
    }
}