package mega.privacy.android.app.presentation.search.model.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.removelink.RemoveNodeLinkDialog
import mega.privacy.android.app.presentation.search.SearchActivityViewModel

internal fun NavGraphBuilder.removeNodeLinkDialogNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
) {
    dialog(
        route = "$removeNodeLinkRoute/{$removeNodeArgumentNodeId}/{$removeNodeArgumentIsFromToolbar}",
        arguments = listOf(
            navArgument(removeNodeArgumentNodeId) { type = NavType.LongType },
            navArgument(removeNodeArgumentIsFromToolbar) { type = NavType.BoolType },
        )
    ) {
        if (it.arguments?.getBoolean(removeNodeArgumentIsFromToolbar) == false) {
            it.arguments?.getLong(removeNodeArgumentNodeId)?.let { nodeId ->
                RemoveNodeLinkDialog(
                    onDismiss = { navHostController.navigateUp() },
                    nodesList = listOf(nodeId),
                )
            }
        } else {
            val searchState by searchActivityViewModel.state.collectAsStateWithLifecycle()
            val list = remember {
                mutableStateOf(
                    searchState.selectedNodes.map { node ->
                        node.id.longValue
                    }
                )
            }
            RemoveNodeLinkDialog(
                onDismiss = { navHostController.navigateUp() },
                nodesList = list.value,
            )
        }
    }
}

internal const val removeNodeLinkRoute = "search/node_bottom_sheet/remove_node_link_dialog"
internal const val removeNodeArgumentNodeId = "nodeId"
internal const val removeNodeArgumentIsFromToolbar = "isFromToolbar"