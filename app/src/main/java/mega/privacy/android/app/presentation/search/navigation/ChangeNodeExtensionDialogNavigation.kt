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
        "$searchChangeExtensionNodeDialog/{${searchChangeNodeExtensionDialogArgumentNodeId}}/{${searchChangeNodeExtensionDialogArgumentNodeNewName}}",
        arguments = listOf(
            navArgument(searchChangeNodeExtensionDialogArgumentNodeId) {
                type = NavType.LongType
            },
            navArgument(searchChangeNodeExtensionDialogArgumentNodeNewName) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val nodeId =
            backStackEntry.arguments?.getLong(searchChangeNodeExtensionDialogArgumentNodeId)
        val nodeNewName =
            backStackEntry.arguments?.getString(searchChangeNodeExtensionDialogArgumentNodeNewName)
        if (nodeId == null || nodeNewName == null) {
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
internal const val searchChangeNodeExtensionDialogArgumentNodeId = "nodeId"
internal const val searchChangeNodeExtensionDialogArgumentNodeNewName = "nodeNewName"