package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.cannotopenfile.CannotOpenFileDialog

internal fun NavGraphBuilder.cannotOpenFileDialogNavigation(
    navHostController: NavHostController,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
) {
    dialog(route = cannotOpenFileDialog) {
        CannotOpenFileDialog(
            onDismiss = { navHostController.navigateUp() },
            onDownload = nodeOptionsBottomSheetViewModel::downloadNodeForPreview
        )
    }
}

internal const val cannotOpenFileDialog = "search/cannotOpenFileDialog"