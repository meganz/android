package mega.privacy.android.app.presentation.search.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialog
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialogViewModel
import mega.privacy.android.app.presentation.search.SearchActivityViewModel


internal fun NavGraphBuilder.moveToRubbishOrDeleteNavigation(
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
) {
    dialog(
        route = "$moveToRubbishOrDelete/{$argumentNodeId}/{$argumentIsInRubbish}/{$isFromToolbar}",
        arguments = listOf(
            navArgument(argumentNodeId) { type = NavType.LongType },
            navArgument(argumentIsInRubbish) { type = NavType.BoolType },
            navArgument(isFromToolbar) { type = NavType.BoolType },
        )
    ) {
        if (it.arguments?.getBoolean(isFromToolbar) == false) {
            it.arguments?.getLong(argumentNodeId)?.let { nodeId ->
                MoveToRubbishOrDeleteNodeDialog(
                    onDismiss = { navHostController.navigateUp() },
                    nodesList = listOf(nodeId),
                    isNodeInRubbish = it.arguments?.getBoolean(argumentIsInRubbish) ?: false,
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
            MoveToRubbishOrDeleteNodeDialog(
                onDismiss = { navHostController.navigateUp() },
                nodesList = list.value,
                isNodeInRubbish = it.arguments?.getBoolean(argumentIsInRubbish) ?: false,
            )
        }
    }
}

internal const val moveToRubbishOrDelete = "search/moveToRubbishOrDelete/isInRubbish"
internal const val argumentNodeId = "nodeId"
internal const val argumentIsInRubbish = "isInRubbish"
internal const val isFromToolbar = "isFromToolbar"