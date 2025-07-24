package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveScreen
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel

@Serializable
data object DriveSync : NavKey

fun NavGraphBuilder.driveSyncScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
) {
    composable<DriveSync> {
        // TODO: drive, sync tabs screen
        val viewModel = hiltViewModel<CloudDriveViewModel>()
        CloudDriveScreen(
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToFolder = onNavigateToFolder,
        )
    }
}