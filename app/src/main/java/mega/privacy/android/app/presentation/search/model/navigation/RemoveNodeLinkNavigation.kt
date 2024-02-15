package mega.privacy.android.app.presentation.search.model.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.removelink.RemoveNodeLinkDialog
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber

internal fun NavGraphBuilder.removeNodeLinkDialogNavigation(
    navHostController: NavHostController,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {
    dialog(
        route = "$removeNodeLinkRoute/{$removeNodeArgumentNodeId}",
        arguments = listOf(
            navArgument(removeNodeArgumentNodeId) { type = NavType.StringType },
        )
    ) {
        it.arguments?.getString(removeNodeArgumentNodeId)?.let { nodes ->
            runCatching { listToStringWithDelimitersMapper<Long>(nodes) }.onSuccess { nodeHandles ->
                RemoveNodeLinkDialog(
                    onDismiss = { navHostController.navigateUp() },
                    nodesList = nodeHandles,
                )
            }.onFailure { error -> Timber.e(error) }
        }
    }
}

internal const val removeNodeLinkRoute = "search/node_bottom_sheet/remove_node_link_dialog"
internal const val removeNodeArgumentNodeId = "nodeId"