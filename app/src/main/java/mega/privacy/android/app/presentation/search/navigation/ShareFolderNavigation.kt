package mega.privacy.android.app.presentation.search.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.warning.ShareFolderDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.isFromToolbar
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.NodeId

internal fun NavGraphBuilder.shareFolderDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
) {
    dialog(
        route = "$searchFolderShareDialog/{$isFromToolbar}",
        arguments = listOf(
            navArgument(isFromToolbar) { type = NavType.BoolType }
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            val nodeOptionsState by nodeOptionsBottomSheetViewModel.state.collectAsStateWithLifecycle()
            nodeOptionsState.node?.let { node ->
                ShareFolderDialog(
                    nodeIds = listOf(NodeId(node.id.longValue)),
                    onDismiss = {
                        navHostController.navigateUp()
                    },
                    onOkClicked = {
                        launchFileContactListActivity(
                            navHostController.context,
                            node.id
                        )
                    }
                )
            }
        } else {
            val searchState by searchActivityViewModel.state.collectAsStateWithLifecycle()
            val nodeIds = searchState.selectedNodes.map { typedNode ->
                typedNode.id
            }
            ShareFolderDialog(
                nodeIds = nodeIds,
                onDismiss = {
                    navHostController.navigateUp()
                },
                onOkClicked = {
                    launchMultipleShareFolder(
                        navHostController.context,
                        nodeIds
                    )
                }
            )
        }
    }
}

private fun launchFileContactListActivity(context: Context, nodeId: NodeId) {
    val intent = Intent()
        .apply {
            setClass(context, AddContactActivity::class.java)
            putExtra("contactType", Constants.CONTACT_TYPE_BOTH)
            putExtra("MULTISELECT", 0)
            putExtra(AddContactActivity.EXTRA_NODE_HANDLE, nodeId.longValue)
        }
    context.startActivity(intent)
}

private fun launchMultipleShareFolder(context: Context, nodeId: List<NodeId>) {
    val handles = nodeId.map {
        it.longValue
    }.toLongArray()
    val intent = Intent().apply {
        setClass(context, AddContactActivity::class.java)
        putExtra("contactType", Constants.CONTACT_TYPE_BOTH)
        putExtra(AddContactActivity.EXTRA_NODE_HANDLE, handles)
        putExtra("MULTISELECT", 1)
    }
    context.startActivity(intent)
}

internal const val searchFolderShareDialog = "search/folder_share"