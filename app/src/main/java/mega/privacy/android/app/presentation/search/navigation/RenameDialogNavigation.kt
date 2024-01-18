package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialog
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogViewModel

internal fun NavGraphBuilder.renameDialogNavigation(
    navHostController: NavHostController,
    renameNodeDialogViewModel: RenameNodeDialogViewModel,
) {
    dialog(
        "$searchRenameDialog/{${searchRenameDialogArgumentNodeId}}",
        arguments = listOf(navArgument(searchRenameDialogArgumentNodeId) {
            type = NavType.LongType
        }),
    ) {
        it.arguments?.getLong(searchRenameDialogArgumentNodeId)?.let { nodeId ->
            RenameNodeDialog(
                nodeId = nodeId,
                onDismiss = {
                    navHostController.navigateUp()
                },
                onOpenChangeExtensionDialog = { newNodeName ->
                    navHostController.navigate("$searchChangeExtensionNodeDialog/$nodeId/$newNodeName")
                },
                viewModel = renameNodeDialogViewModel
            )
        }
    }
}

internal const val searchRenameDialog = "search/rename_dialog"
internal const val searchRenameDialogArgumentNodeId = "nodeId"