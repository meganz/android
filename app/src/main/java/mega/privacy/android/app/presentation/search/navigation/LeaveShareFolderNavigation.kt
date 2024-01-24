package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.leaveshare.LeaveShareDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.isFromToolbar

internal fun NavGraphBuilder.leaveFolderShareDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel
) {
    dialog(
        route = "$searchLeaveShareFolderDialog/{$isFromToolbar}",
        arguments = listOf(
            navArgument(isFromToolbar) { type = NavType.BoolType }
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            val nodeOptionsState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
            nodeOptionsState.node?.let { node ->
                LeaveShareDialog(
                    handles = listOf(node.id.longValue),
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