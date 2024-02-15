package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialog
import mega.privacy.android.app.presentation.search.nodeListHandle
import java.io.File

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

        nodeHandle?.let { nodeId ->
            RenameNodeDialog(
                nodeId = nodeId,
                onDismiss = {
                    navHostController.navigateUp()
                },
                onOpenChangeExtensionDialog = { newNodeName ->
                    navHostController.navigate(
                        searchChangeExtensionNodeDialog.plus(File.separator).plus(newNodeName)
                            .plus(File.separator).plus(nodeId)
                    )
                },
            )
        }
    }
}

internal const val searchRenameDialog = "search/rename_dialog"