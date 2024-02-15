package mega.privacy.android.app.presentation.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.removesharefolder.RemoveShareFolderDialog
import mega.privacy.android.app.presentation.search.nodeListHandle
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber

internal fun NavGraphBuilder.removeShareFolderDialogNavigation(
    navHostController: NavHostController,
    stringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {
    dialog(
        route = "$searchRemoveFolderShareDialog/{$nodeListHandle}",
        arguments = listOf(
            navArgument(nodeListHandle) { type = NavType.StringType }
        )
    ) {
        it.arguments?.getString(nodeListHandle)?.let { handles ->
            runCatching {
                stringWithDelimitersMapper<NodeId>(handles)
            }.onSuccess { nodeIds ->
                RemoveShareFolderDialog(
                    nodeList = nodeIds,
                    onDismiss = {
                        navHostController.navigateUp()
                    }
                )
            }
                .onFailure { throwable -> Timber.e(throwable) }
        }
    }
}

internal const val searchRemoveFolderShareDialog = "search/folder_share_remove"