package mega.privacy.android.app.presentation.search

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout

/**
 * Search nav host controller
 *
 * @param viewModel Search activity view model
 * @param handleClick Function to handle click
 * @param navigateToLink Function to navigate to link
 * @param showSortOrderBottomSheet Function to show sort order bottom sheet
 * @param onBackPressed
 * @param nodeActionHandler Node bottom sheet action handler
 * @param navHostController
 * @param bottomSheetNavigator
 * @param nodeActionsViewModel
 * @param listToStringWithDelimitersMapper
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun SearchNavHostController(
    viewModel: SearchViewModel,
    navigateToLink: (String) -> Unit,
    showSortOrderBottomSheet: () -> Unit,
    onBackPressed: () -> Unit,
    nodeActionHandler: NodeActionHandler,
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    nodeActionsViewModel: NodeActionsViewModel,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    fileTypeIconMapper: FileTypeIconMapper,
    handleClick: (TypedNode?) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    MegaBottomSheetLayout(
        modifier = modifier.navigationBarsPadding(),
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        val nodeActionsState by nodeActionsViewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        EventEffect(
            nodeActionsState.onRenameSucceedEvent,
            nodeActionsViewModel::resetOnRenameSucceedEvent
        ) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.context_correctly_renamed)
            )
        }

        NavHost(
            modifier = modifier.navigationBarsPadding(),
            navController = navHostController,
            startDestination = searchRoute
        ) {
            searchNavGraph(
                navigateToLink = navigateToLink,
                showSortOrderBottomSheet = showSortOrderBottomSheet,
                navHostController = navHostController,
                searchViewModel = viewModel,
                nodeActionHandler = nodeActionHandler,
                onBackPressed = onBackPressed,
                nodeActionsViewModel = nodeActionsViewModel,
                handleClick = handleClick,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
                fileTypeIconMapper = fileTypeIconMapper,
                onRenameNode = nodeActionsViewModel::renameNode,
            )
        }
    }
}