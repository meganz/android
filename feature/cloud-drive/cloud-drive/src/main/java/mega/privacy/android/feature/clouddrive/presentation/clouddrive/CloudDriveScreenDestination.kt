package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R

@Serializable
data class CloudDrive(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val isNewFolder: Boolean = false,
)

fun NavGraphBuilder.cloudDriveScreen(
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
) {
    composable<CloudDrive> { backStackEntry ->
        val args = backStackEntry.toRoute<CloudDrive>()
        val viewModel = hiltViewModel<CloudDriveViewModel>(key = args.nodeHandle.toString())
        val snackbarHostState = LocalSnackBarHostState.current
        val context = LocalContext.current

        var hasShownSnackbar by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(args.isNewFolder) {
            if (args.isNewFolder && !hasShownSnackbar) {
                snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.context_folder_created))
                hasShownSnackbar = true
            }
        }

        CloudDriveScreen(
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToFolder = onNavigateToFolder,
            onCreatedNewFolder = onCreatedNewFolder,
            onTransfer = onTransfer,
        )
    }
}