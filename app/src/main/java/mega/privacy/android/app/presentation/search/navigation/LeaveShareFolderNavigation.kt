package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.leaveshare.LeaveShareDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel

internal fun NavGraphBuilder.leaveFolderShareDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
) {
    dialog(
        route = "$searchLeaveShareFolderDialog/{$searchLeaveShareDialogArgumentNodeId}/{$isLeaveShareFromToolbar}",
        arguments = listOf(
            navArgument(searchRemoveFolderShareDialogArgumentNodeId) { type = NavType.LongType },
            navArgument(isRemoveFolderShareFromToolbar) { type = NavType.BoolType }
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            it.arguments?.getLong(searchRemoveFolderShareDialogArgumentNodeId)?.let { handle ->
                LeaveShareDialog(
                    handles = listOf(handle),
                    onDismiss = {
                        navHostController.navigateUp()
                    }
                )
            }
        } else {
            val searchState by searchActivityViewModel.state.collectAsStateWithLifecycle()
            val handles = searchState.selectedNodes.map { typedNode ->
                typedNode.id.longValue
            }
            LeaveShareDialog(
                handles = handles,
                onDismiss = {
                    navHostController.navigateUp()
                }
            )
        }
    }
}

internal const val searchLeaveShareFolderDialog = "search/leave_share_folder"
internal const val searchLeaveShareDialogArgumentNodeId = "nodeId"
internal const val isLeaveShareFromToolbar = "isFromToolbar"