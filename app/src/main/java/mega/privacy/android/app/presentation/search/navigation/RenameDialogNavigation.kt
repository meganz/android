package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialog

internal fun NavGraphBuilder.renameDialogNavigation(
    navHostController: NavHostController,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel
) {
    dialog(
        searchRenameDialog,
    ) {
        val nodeOptionsState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
        nodeOptionsState.node?.let { nodeId ->
            RenameNodeDialog(
                nodeId = nodeId.id.longValue,
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