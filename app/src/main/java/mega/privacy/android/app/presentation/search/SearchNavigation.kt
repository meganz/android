package mega.privacy.android.app.presentation.search

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.dialogs.RenameNodeDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialogViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
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
            searchActivityViewModel = searchActivityViewModel,
            onBackPressed = onBackPressed
        )
    }
    moveToRubbishOrDeleteNavigation(
        navHostController = navHostController,
        moveToRubbishOrDeleteNodeDialogViewModel = moveToRubbishOrDeleteNodeDialogViewModel,
        searchActivityViewModel = searchActivityViewModel
    )
    renameDialogNavigation(navHostController, renameNodeDialogViewModel)
    nodeBottomSheetNavigation(nodeBottomSheetActionHandler, navHostController)
    changeLabelBottomSheetNavigation(navHostController)
}


/**
 * Route for Search
 */
internal const val searchRoute = "search/main"
