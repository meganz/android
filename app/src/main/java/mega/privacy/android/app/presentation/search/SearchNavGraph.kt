package mega.privacy.android.app.presentation.search

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotOpenFileDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserNavigation
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.changeNodeExtensionDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.foreignNodeDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.leaveFolderShareDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.overQuotaDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.removeShareFolderDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.searchFilterBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderDialogNavigation
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper


/**
 * Navigation graph for Search
 *
 * @param showSortOrderBottomSheet Function to show sort order bottom sheet
 * @param navigateToLink Function to navigate to link
 * @param handleClick Function to handle click
 * @param navHostController Navigation controller
 * @param nodeActionHandler Node bottom sheet action handler
 * @param searchActivityViewModel Search activity view model
 * @param onBackPressed OnBackPressed
 * @param nodeActionsViewModel
 * @param listToStringWithDelimitersMapper
 */
internal fun NavGraphBuilder.searchNavGraph(
    showSortOrderBottomSheet: () -> Unit,
    navigateToLink: (String) -> Unit,
    navHostController: NavHostController,
    nodeActionHandler: NodeActionHandler,
    searchActivityViewModel: SearchActivityViewModel,
    onBackPressed: () -> Unit,
    nodeActionsViewModel: NodeActionsViewModel,
    handleClick: (TypedNode?) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) {
    composable(searchRoute) {
        SearchScreen(
            navigateToLink = navigateToLink,
            showSortOrderBottomSheet = showSortOrderBottomSheet,
            navHostController = navHostController,
            searchActivityViewModel = searchActivityViewModel,
            onBackPressed = onBackPressed,
            nodeActionHandler = nodeActionHandler,
            fileTypeIconMapper = fileTypeIconMapper,
            handleClick = handleClick
        )
    }
    moveToRubbishOrDeleteNavigation(
        navHostController = navHostController,
        listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    renameDialogNavigation(
        navHostController = navHostController,
    )
    nodeBottomSheetNavigation(
        nodeActionHandler = nodeActionHandler,
        navHostController = navHostController,
        fileTypeIconMapper = fileTypeIconMapper
    )
    searchFilterBottomSheetNavigation(
        navHostController = navHostController,
        searchActivityViewModel = searchActivityViewModel,
    )
    changeLabelBottomSheetNavigation(navHostController)
    changeNodeExtensionDialogNavigation(navHostController)
    cannotVerifyUserNavigation(navHostController)
    removeNodeLinkDialogNavigation(
        navHostController = navHostController,
        listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    shareFolderDialogNavigation(
        navHostController = navHostController,
        nodeActionHandler = nodeActionHandler,
        stringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    removeShareFolderDialogNavigation(
        navHostController = navHostController,
        stringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    leaveFolderShareDialogNavigation(
        navHostController = navHostController,
        stringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    overQuotaDialogNavigation(navHostController = navHostController)
    foreignNodeDialogNavigation(navHostController = navHostController)
    shareFolderAccessDialogNavigation(
        navHostController = navHostController,
        listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
    )
    cannotOpenFileDialogNavigation(
        navHostController = navHostController,
        nodeActionsViewModel = nodeActionsViewModel,
    )
}

/**
 * Route for Search
 */
internal const val searchRoute = "search/main"
internal const val nodeListHandle = "nodeListHandle"