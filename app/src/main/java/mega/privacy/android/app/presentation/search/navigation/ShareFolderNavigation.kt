package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.warning.ShareFolderDialog
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.app.presentation.search.nodeListHandle
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper

internal fun NavGraphBuilder.shareFolderDialogNavigation(
    navHostController: NavHostController,
    stringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    nodeActionHandler: NodeActionHandler,
) {
    dialog(
        route = "$searchFolderShareDialog/{$nodeListHandle}",
        arguments = listOf(
            navArgument(nodeListHandle) { type = NavType.StringType }
        )
    ) {
        val nodeHandle = it.arguments?.getString(nodeListHandle)

        nodeHandle?.let {
            val nodeHandles = stringWithDelimitersMapper<Long>(it)
            val nodeIds = nodeHandles.map { handle ->
                NodeId(handle)
            }
            if (nodeHandles.size == 1) {
                ShareFolderDialog(
                    nodeIds = nodeIds,
                    onDismiss = {
                        navHostController.navigateUp()
                    },
                    onOkClicked = { typeNodeList ->
                        nodeActionHandler.handleAction(
                            ShareFolderMenuAction(),
                            typeNodeList.first()
                        )
                    }
                )
            } else {
                ShareFolderDialog(
                    nodeIds = nodeIds,
                    onDismiss = {
                        navHostController.navigateUp()
                    },
                    onOkClicked = { typeNodeList ->
                        nodeActionHandler.handleAction(ShareFolderMenuAction(), typeNodeList)
                    }
                )
            }
        }
    }
}

internal const val searchFolderShareDialog = "search/folder_share"