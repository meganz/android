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
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDrive

fun NavGraphBuilder.cloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onNavigateToFolder: (NodeId, String?) -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
    onRenameNode: (NodeId) -> Unit,
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
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToFolder = onNavigateToFolder,
            onCreatedNewFolder = onCreatedNewFolder,
            onTransfer = onTransfer,
            onRenameNode = onRenameNode,
        )
    }
}