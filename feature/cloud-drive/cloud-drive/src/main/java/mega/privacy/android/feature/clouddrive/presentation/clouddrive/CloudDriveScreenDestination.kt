package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId

@Serializable
data class CloudDrive(
    val nodeHandle: Long = -1L
)

fun NavGraphBuilder.cloudDriveScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
) {
    composable<CloudDrive> { backStackEntry ->
        val args = backStackEntry.toRoute<CloudDrive>()
        val viewModel = hiltViewModel<CloudDriveViewModel>(key = args.nodeHandle.toString())
        CloudDriveScreen(
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToFolder = onNavigateToFolder,
        )
    }
}