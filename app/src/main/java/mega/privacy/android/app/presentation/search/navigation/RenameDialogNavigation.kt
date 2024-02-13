package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialog
import mega.privacy.android.app.presentation.search.nodeListHandle

internal fun NavGraphBuilder.renameDialogNavigation(
    navHostController: NavHostController,
) {
    dialog(
        route = "$searchRenameDialog/{$nodeListHandle}",
        arguments = listOf(
            navArgument(nodeListHandle) {
                type = NavType.LongType
            }
        )
    ) {
        val nodeHandle = it.arguments?.getLong(nodeListHandle)

        nodeHandle?.let {
            RenameNodeDialog(
                nodeId = it,
                onDismiss = {
                    navHostController.navigateUp()
                },
                onOpenChangeExtensionDialog = { newNodeName ->
                    navHostController.navigate("$searchChangeExtensionNodeDialog/$newNodeName")
                },
            )
        }
    }
}

internal const val searchRenameDialog = "search/rename_dialog"