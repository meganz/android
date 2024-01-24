package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.removesharefolder.RemoveShareFolderDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.domain.entity.node.NodeId

internal fun NavGraphBuilder.removeShareFolderDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
) {
    dialog(
        route = "$searchRemoveFolderShareDialog/{$searchRemoveFolderShareDialogArgumentNodeId}/{$isRemoveFolderShareFromToolbar}",
        arguments = listOf(
            navArgument(searchRemoveFolderShareDialogArgumentNodeId) { type = NavType.LongType },
            navArgument(isRemoveFolderShareFromToolbar) { type = NavType.BoolType }
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            it.arguments?.getLong(searchRemoveFolderShareDialogArgumentNodeId)?.let { handle ->
                RemoveShareFolderDialog(
                    nodeList = listOf(NodeId(handle)),
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
internal const val searchRemoveFolderShareDialogArgumentNodeId = "nodeId"
internal const val isRemoveFolderShareFromToolbar = "isFromToolbar"