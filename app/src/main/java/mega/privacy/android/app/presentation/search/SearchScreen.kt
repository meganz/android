package mega.privacy.android.app.presentation.search

import androidx.activity.compose.BackHandler
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    showSortOrderBottomSheet: () -> Unit,
    navigateToLink: (String) -> Unit,
    navHostController: NavHostController,
    onBackPressed: () -> Unit,
    searchViewModel: SearchViewModel,
    nodeActionHandler: NodeActionHandler,
    handleClick: (TypedNode?) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    modifier: Modifier = Modifier,
) {
    val uiState by searchViewModel.state.collectAsStateWithLifecycle()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    var selectedNode: NodeUIItem<TypedNode>? by remember {
        mutableStateOf(null)
    }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasOpenedFolder = remember(uiState.navigationLevel) {
        uiState.navigationLevel.isNotEmpty()
    }
    BackHandler(enabled = modalSheetState.isVisible || hasOpenedFolder) {
        when {
            modalSheetState.isVisible -> {
                selectedNode = null
                coroutineScope.launch { modalSheetState.hide() }
            }

            hasOpenedFolder -> {
                onBackPressed()
            }
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
            if (searchViewModel.state.value.selectedNodes.isEmpty()) {
                handleClick(it.node)
            } else {
                searchViewModel.onItemClicked(it)
            }
        },
        onLongClick = searchViewModel::onLongItemClicked,
        onChangeViewTypeClick = searchViewModel::onChangeViewTypeClicked,
        onSortOrderClick = {
            showSortOrderBottomSheet()
        },
        onMenuClick = {
            keyboardController?.hide()
            navHostController.navigate(
                route = nodeBottomSheetRoute.plus("/${it.node.id.longValue}")
                    .plus("/${searchViewModel.state.value.nodeSourceType.name}")
            ) {
                popUpTo(nodeBottomSheetRoute) { inclusive = true }
                launchSingleTop = true
            }
        },
        onDisputeTakeDownClicked = navigateToLink,
        onLinkClicked = navigateToLink,
        onErrorShown = searchViewModel::errorMessageShown,
        updateSearchQuery = searchViewModel::updateSearchQuery,
        onFilterClicked = {
            keyboardController?.hide()
            navHostController.navigate(
                route = searchFilterBottomSheetRoute.plus("/${it}")
            )
        },
        clearSelection = searchViewModel::clearSelection,
        onBackPressed = onBackPressed,
        onResetScrollEventConsumed = searchViewModel::onResetScrollEventConsumed,
        navHostController = navHostController,
        nodeActionHandler = nodeActionHandler,
        fileTypeIconMapper = fileTypeIconMapper,
        nodeSourceType = uiState.nodeSourceType,
    )
}
