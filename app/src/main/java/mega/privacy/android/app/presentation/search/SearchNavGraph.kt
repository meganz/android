package mega.privacy.android.app.presentation.search

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.dialogs.changeextension.ChangeNodeExtensionDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.deletenode.MoveToRubbishOrDeleteNodeDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.removelink.RemoveNodeLinkViewModel
import mega.privacy.android.app.presentation.node.dialogs.removesharefolder.RemoveShareFolderViewModel
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogViewModel
import mega.privacy.android.app.presentation.node.dialogs.sharefolder.ShareFolderDialogViewModel
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserNavigation
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.changeNodeExtensionDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.leaveFolderShareDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.removeShareFolderDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderDialogNavigation
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
    removeNodeLinkViewModel: RemoveNodeLinkViewModel,
    shareFolderDialogViewModel: ShareFolderDialogViewModel,
    changeNodeExtensionDialogViewModel: ChangeNodeExtensionDialogViewModel,
    removeShareFolderViewModel: RemoveShareFolderViewModel,
    nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
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
    nodeBottomSheetNavigation(
        nodeBottomSheetActionHandler,
        navHostController,
        nodeOptionsBottomSheetViewModel
    )
    changeLabelBottomSheetNavigation(navHostController)
    changeNodeExtensionDialogNavigation(
        navHostController = navHostController,
        changeNodeExtensionDialogViewModel = changeNodeExtensionDialogViewModel,
    )
    renameDialogNavigation(navHostController, renameNodeDialogViewModel)
    cannotVerifyUserNavigation(navHostController)
    removeNodeLinkDialogNavigation(
        navHostController = navHostController,
        removeNodeLinkViewModel = removeNodeLinkViewModel,
        searchActivityViewModel = searchActivityViewModel
    )
    shareFolderDialogNavigation(
        navHostController = navHostController,
        shareFolderDialogViewModel = shareFolderDialogViewModel,
        searchActivityViewModel = searchActivityViewModel
    )
    removeShareFolderDialogNavigation(
        navHostController = navHostController,
        removeShareFolderViewModel = removeShareFolderViewModel,
        searchActivityViewModel = searchActivityViewModel
    )
    leaveFolderShareDialogNavigation(
        navHostController = navHostController,
        searchActivityViewModel = searchActivityViewModel
    )
}

/**
 * Route for Search
 */
internal const val searchRoute = "search/main"
