package mega.privacy.android.app.presentation.search

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialog
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.RenameNodeDialog
import mega.privacy.android.app.presentation.node.dialogs.RenameNodeDialogViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * Navigation graph for Search
 *
 * @param trackAnalytics Function to track analytics
 * @param showSortOrderBottomSheet Function to show sort order bottom sheet
 * @param navigateToLink Function to navigate to link
 * @param handleClick Function to handle click
 * @param navHostController Navigation controller
 * @param nodeBottomSheetActionHandler Node bottom sheet action handler
 * @param searchActivityViewModel Search activity view model
 * @param onBackPressed OnBackPressed
 */
internal fun NavGraphBuilder.searchNavGraph(
    trackAnalytics: (SearchFilter?) -> Unit,
    showSortOrderBottomSheet: () -> Unit,
    navigateToLink: (String) -> Unit,
    handleClick: (TypedNode?) -> Unit,
    navHostController: NavHostController,
    nodeBottomSheetActionHandler: NodeBottomSheetActionHandler,
    searchActivityViewModel: SearchActivityViewModel,
    moveToRubbishOrDeleteNodeDialogViewModel: MoveToRubbishOrDeleteNodeDialogViewModel,
    renameNodeDialogViewModel: RenameNodeDialogViewModel,
    onBackPressed: () -> Unit,
) {
    composable(searchRoute) {
        SearchScreen(
            trackAnalytics = trackAnalytics,
            handleClick = handleClick,
            navigateToLink = navigateToLink,
            showSortOrderBottomSheet = showSortOrderBottomSheet,
            navHostController = navHostController,
            nodeBottomSheetActionHandler = nodeBottomSheetActionHandler,
            searchActivityViewModel = searchActivityViewModel,
            onBackPressed = onBackPressed
        )
    }

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
                    viewModel = moveToRubbishOrDeleteNodeDialogViewModel
                )
            }
        } else {
            val searchState by searchActivityViewModel.state.collectAsStateWithLifecycle()
            val list = searchState.selectedNodes.map { node ->
                node.id.longValue
            }
            MoveToRubbishOrDeleteNodeDialog(
                onDismiss = { navHostController.navigateUp() },
                nodesList = list,
                isNodeInRubbish = it.arguments?.getBoolean(argumentIsInRubbish) ?: false,
                viewModel = moveToRubbishOrDeleteNodeDialogViewModel
            )
        }
    }

    dialog(
        "$searchRenameDialog/{$argumentNodeId}",
        arguments = listOf(navArgument(argumentNodeId) { type = NavType.LongType }),
    ) {
        it.arguments?.getLong(argumentNodeId)?.let { nodeId ->
            RenameNodeDialog(
                nodeId = nodeId,
                onDismiss = {
                    navHostController.popBackStack()
                },
                viewModel = renameNodeDialogViewModel
            )
        }
    }
}

/**
 * Route for Search
 */
internal const val searchRoute = "search/main"
internal const val moveToRubbishOrDelete = "search/moveToRubbishOrDelete/isInRubbish"
internal const val searchRenameDialog = "search/rename_dialog"
internal const val argumentNodeId = "nodeId"
internal const val argumentIsInRubbish = "isInRubbish"
internal const val isFromToolbar = "isFromToolbar"
