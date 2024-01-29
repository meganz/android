package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.changeextension.ChangeNodeExtensionDialog

internal fun NavGraphBuilder.changeNodeExtensionDialogNavigation(
    navHostController: NavHostController,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel
) {
    dialog(
        "$searchChangeExtensionNodeDialog/{$searchChangeNodeExtensionDialogArgumentNodeNewName}",
        arguments = listOf(
            navArgument(searchChangeNodeExtensionDialogArgumentNodeNewName) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val nodeOptionState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
        val nodeNewName =
            backStackEntry.arguments?.getString(searchChangeNodeExtensionDialogArgumentNodeNewName)
        if (nodeOptionState.node == null || nodeNewName == null) {
            navHostController.navigateUp()
            return@dialog
        }
        nodeOptionState.node?.let {
            ChangeNodeExtensionDialog(
                nodeId = it.id.longValue,
                newNodeName = nodeNewName,
                onDismiss = {
                    navHostController.navigateUp()
                },
            )
        }
    }
}

internal const val searchChangeExtensionNodeDialog = "search/changeNodeExtensionDialog"
internal const val searchChangeNodeExtensionDialogArgumentNodeNewName = "nodeNewName"