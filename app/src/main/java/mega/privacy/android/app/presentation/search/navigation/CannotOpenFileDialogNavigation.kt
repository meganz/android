package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.dialogs.cannotopenfile.CannotOpenFileDialog

internal fun NavGraphBuilder.cannotOpenFileDialogNavigation(
    navHostController: NavHostController,
    nodeActionsViewModel: NodeActionsViewModel,
) {
    dialog(route = cannotOpenFileDialog) {
        CannotOpenFileDialog(
            onDismiss = { navHostController.navigateUp() },
            onDownload = nodeActionsViewModel::downloadNodeForPreview
        )
    }
}

internal const val cannotOpenFileDialog = "search/cannotOpenFileDialog"