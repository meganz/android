package mega.privacy.android.app.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.view.NodeOptionsBottomSheet
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.view.SearchComposeView
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Search activity to search Nodes and display
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    trackAnalytics: (SearchFilter?) -> Unit,
    showSortOrderBottomSheet: () -> Unit,
    navigateToLink: (String) -> Unit,
    handleClick: (TypedNode?) -> Unit,
    navHostController: NavHostController,
    searchActivityViewModel: SearchActivityViewModel,
    nodeBottomSheetActionHandler: NodeBottomSheetActionHandler,
    modifier: Modifier = Modifier,
) {
    val uiState by searchActivityViewModel.state.collectAsStateWithLifecycle()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    var selectedNode: NodeUIItem<TypedNode>? by remember {
        mutableStateOf(null)
    }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(enabled = modalSheetState.isVisible) {
        selectedNode = null
        coroutineScope.launch {
            modalSheetState.hide()
        }
    }
    SearchComposeView(
        modifier = modifier,
        state = uiState,
        sortOrder = stringResource(
            SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                ?: R.string.sortby_name
        ),
        onItemClick = searchActivityViewModel::onItemClicked,
        onLongClick = searchActivityViewModel::onLongItemClicked,
        onChangeViewTypeClick = searchActivityViewModel::onChangeViewTypeClicked,
        onSortOrderClick = {
            showSortOrderBottomSheet()
        },
        onMenuClick = {
            keyboardController?.hide()
            selectedNode = it
            coroutineScope.launch {
                modalSheetState.show()
            }
        },
        onDisputeTakeDownClicked = navigateToLink,
        onLinkClicked = navigateToLink,
        onErrorShown = searchActivityViewModel::errorMessageShown,
        updateFilter = searchActivityViewModel::updateFilter,
        trackAnalytics = trackAnalytics,
        updateSearchQuery = searchActivityViewModel::updateSearchQuery,
    )
    handleClick(uiState.lastSelectedNode)

    selectedNode?.let {
        NodeOptionsBottomSheet(
            modalSheetState = modalSheetState,
            node = it.node,
            handler = nodeBottomSheetActionHandler,
            navHostController = navHostController,
            onDismiss = {
                selectedNode = null
                coroutineScope.launch {
                    modalSheetState.hide()
                }
            },
        )
    }
}
