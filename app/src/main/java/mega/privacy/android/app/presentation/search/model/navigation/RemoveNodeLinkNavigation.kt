package mega.privacy.android.app.presentation.search.model.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.google.gson.Gson
import mega.privacy.android.app.presentation.node.dialogs.removelink.RemoveNodeLinkDialog

internal fun NavGraphBuilder.removeNodeLinkDialogNavigation(
    navHostController: NavHostController,
) {
    dialog(
        route = "$removeNodeLinkRoute/{$removeNodeArgumentNodeId}",
        arguments = listOf(
            navArgument(removeNodeArgumentNodeId) { type = NavType.StringType },
        )
    ) {
        it.arguments?.getString(removeNodeArgumentNodeId)?.let { nodes ->
            val nodeHandles =
                runCatching { Gson().fromJson(nodes, Array<Long>::class.java).toList() }
                    .getOrDefault(emptyList())
            RemoveNodeLinkDialog(
                onDismiss = { navHostController.navigateUp() },
                nodesList = nodeHandles,
            )
        }
    }
}

internal const val removeNodeLinkRoute = "search/node_bottom_sheet/remove_node_link_dialog"
internal const val removeNodeArgumentNodeId = "nodeId"