package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.removesharefolder.RemoveShareFolderDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.isFromToolbar
import mega.privacy.android.domain.entity.node.NodeId

internal fun NavGraphBuilder.removeShareFolderDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel
) {
    dialog(
        route = "$searchRemoveFolderShareDialog/{$isFromToolbar}",
        arguments = listOf(
            navArgument(isFromToolbar) { type = NavType.BoolType }
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            val nodeOptionsState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
            nodeOptionsState.node?.let { node ->
                RemoveShareFolderDialog(
                    nodeList = listOf(NodeId(node.id.longValue)),
                    onDismiss = {
                        navHostController.navigateUp()
                    }
                )
            }
        } else {
            val searchState by searchActivityViewModel.state.collectAsStateWithLifecycle()
            val nodeIds = searchState.selectedNodes.map { typedNode ->
                typedNode.id
            }
            RemoveShareFolderDialog(
                nodeList = nodeIds,
                onDismiss = {
                    navHostController.navigateUp()
                }
            )
        }
    }
}

internal const val searchRemoveFolderShareDialog = "search/folder_share_remove"