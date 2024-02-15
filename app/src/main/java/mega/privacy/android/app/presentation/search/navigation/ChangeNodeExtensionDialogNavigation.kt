package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.changeextension.ChangeNodeExtensionDialog

internal fun NavGraphBuilder.changeNodeExtensionDialogNavigation(
    navHostController: NavHostController,
) {
    dialog(
        "$searchChangeExtensionNodeDialog/{$searchChangeNodeExtensionDialogArgumentNodeNewName}/{$searchChangeNodeExtensionNodeDialogArgumentNodeId}",
        arguments = listOf(
            navArgument(searchChangeNodeExtensionDialogArgumentNodeNewName) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val nodeNewName =
            backStackEntry.arguments?.getString(searchChangeNodeExtensionDialogArgumentNodeNewName)
        val nodeId =
            backStackEntry.arguments?.getLong(searchChangeNodeExtensionNodeDialogArgumentNodeId)
        if (nodeId == null || nodeId != -1L || nodeNewName == null) {
            navHostController.navigateUp()
            return@dialog
        }
        ChangeNodeExtensionDialog(
            nodeId = nodeId,
            newNodeName = nodeNewName,
            onDismiss = {
                navHostController.navigateUp()
            },
        )
    }
}

internal const val searchChangeExtensionNodeDialog = "search/changeNodeExtensionDialog"
internal const val searchChangeNodeExtensionDialogArgumentNodeNewName = "nodeNewName"
internal const val searchChangeNodeExtensionNodeDialogArgumentNodeId = "nodeId"