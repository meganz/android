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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetRoute
import mega.privacy.android.app.presentation.search.navigation.searchFilterBottomSheetRoute
import mega.privacy.android.app.presentation.search.view.SearchComposeView
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Search activity to search Nodes and display
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    showSortOrderBottomSheet: () -> Unit,
    navigateToLink: (String) -> Unit,
    navHostController: NavHostController,
    onBackPressed: () -> Unit,
    searchActivityViewModel: SearchActivityViewModel,
    nodeActionHandler: NodeActionHandler,
    handleClick: (TypedNode?) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
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
        modifier = modifier.semantics { testTagsAsResourceId = true },
        state = uiState,
        sortOrder = stringResource(
            SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                ?: R.string.sortby_name
        ),
        onItemClick = {
            if (searchActivityViewModel.state.value.selectedNodes.isEmpty()) {
                handleClick(it.node)
            } else {
                searchActivityViewModel.onItemClicked(it)
            }
        },
        onLongClick = searchActivityViewModel::onLongItemClicked,
        onChangeViewTypeClick = searchActivityViewModel::onChangeViewTypeClicked,
        onSortOrderClick = {
            showSortOrderBottomSheet()
        },
        onMenuClick = {
            keyboardController?.hide()
            navHostController.navigate(
                route = nodeBottomSheetRoute.plus("/${it.node.id.longValue}")
                    .plus("/${searchActivityViewModel.state.value.nodeSourceType.name}")
            )
        },
        onDisputeTakeDownClicked = navigateToLink,
        onLinkClicked = navigateToLink,
        onErrorShown = searchActivityViewModel::errorMessageShown,
        updateSearchQuery = searchActivityViewModel::updateSearchQuery,
        onFilterClicked = {
            keyboardController?.hide()
            navHostController.navigate(
                route = searchFilterBottomSheetRoute.plus("/${it}")
            )
        },
        clearSelection = searchActivityViewModel::clearSelection,
        onBackPressed = onBackPressed,
        navHostController = navHostController,
        nodeActionHandler = nodeActionHandler,
        fileTypeIconMapper = fileTypeIconMapper,
        nodeSourceType = uiState.nodeSourceType,
    )
}
