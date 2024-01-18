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
import mega.privacy.android.app.main.FileContactListActivity
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.ShareFolderDialog
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.ShareFolderDialogViewModel
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.NodeId

internal fun NavGraphBuilder.shareFolderDialogNavigation(
    navHostController: NavHostController,
    shareFolderDialogViewModel: ShareFolderDialogViewModel,
    searchActivityViewModel: SearchActivityViewModel,
) {
    dialog(
        route = "$searchFolderShareDialog/{$searchFolderShareDialogArgumentNodeId}/{$isFolderShareFromToolbar}",
        arguments = listOf(
            navArgument(searchFolderShareDialogArgumentNodeId) { type = NavType.LongType },
            navArgument(isFolderShareFromToolbar) { type = NavType.BoolType }
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            it.arguments?.getLong(searchFolderShareDialogArgumentNodeId)?.let { handle ->
                ShareFolderDialog(
                    nodeIds = listOf(NodeId(handle)),
                    shareFolderDialogViewModel = shareFolderDialogViewModel,
                    onDismiss = {
                        navHostController.navigateUp()
                    },
                    onOkClicked = {
                        launchFileContactListActivity(
                            navHostController.context,
                            NodeId(handle)
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
                shareFolderDialogViewModel = shareFolderDialogViewModel,
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
internal const val searchFolderShareDialogArgumentNodeId = "nodeId"
internal const val isFolderShareFromToolbar = "isFromToolbar"