package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewForSearch
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * View for Search compose
 * @param state [SearchActivityState]
 * @param sortOrder String
 * @param onItemClick item click listener
 * @param onLongClick item long click listener
 * @param onMenuClick item menu click listener
 * @param onSortOrderClick Change sort order click listener
 * @param onChangeViewTypeClick change view type click listener
 * @param onLinkClicked link click listener for item
 * @param onDisputeTakeDownClicked dispute take-down click listener
 * @param onFilterClicked a filter has been clicked
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchComposeView(
    state: SearchActivityState,
    sortOrder: String,
    onItemClick: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    onErrorShown: () -> Unit,
    updateFilter: (SearchFilter) -> Unit,
    trackAnalytics: (SearchFilter) -> Unit,
    updateSearchQuery: (String) -> Unit,
    onFilterClicked: (String) -> Unit,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    nodeActionHandler: NodeActionHandler,
    clearSelection: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    nodeSourceType: NodeSourceType,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scaffoldState = rememberScaffoldState()
    val snackBarHostState = remember { SnackbarHostState() }
    var topBarPadding by remember { mutableStateOf(0.dp) }

    LaunchedEffect(key1 = state.resetScroll) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(key1 = state.resetScroll) {
        gridState.scrollToItem(0)
    }

    var searchQuery by rememberSaveable {
        mutableStateOf(state.searchQuery)
    }

    searchQuery.useDebounce(
        onChange = {
            updateSearchQuery(it)
        },
    )

    val highlightText by remember(state.navigationLevel) {
        derivedStateOf {
            searchQuery.takeIf { state.navigationLevel.isEmpty() }.orEmpty()
        }
    }

    topBarPadding = if (state.navigationLevel.isNotEmpty()) 8.dp else 0.dp

    state.errorMessageId?.let {
        val errorMessage = stringResource(id = it)
        LaunchedEffect(key1 = scaffoldState.snackbarHostState) {
            val s = scaffoldState.snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Long
            )
            if (s == SnackbarResult.Dismissed) {
                onErrorShown()
            }

        }
    }
    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            SearchToolBar(
                searchQuery = searchQuery,
                updateSearchQuery = {
                    searchQuery = it
                },
                onBackPressed = onBackPressed,
                selectedNodes = state.selectedNodes,
                totalCount = state.searchItemList.size,
                navHostController = navHostController,
                nodeActionHandler = nodeActionHandler,
                clearSelection = clearSelection,
                nodeSourceType = state.nodeSourceType,
                navigationLevel = state.navigationLevel
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(top = topBarPadding)) {
            if ((state.nodeSourceType == NodeSourceType.CLOUD_DRIVE || state.nodeSourceType == NodeSourceType.HOME) && state.navigationLevel.isEmpty()) {
                FilterChipsView(
                    state = state,
                    onFilterClicked = onFilterClicked,
                    updateFilter = updateFilter,
                    trackAnalytics = trackAnalytics
                )
            }
            if (state.isSearching) {
                LoadingStateView(
                    isList = state.currentViewType == ViewType.LIST,
                    modifier = Modifier
                )
            } else {
                if (state.searchItemList.isNotEmpty()) {
                    NodesView(
                        nodeUIItems = state.searchItemList,
                        onMenuClick = onMenuClick,
                        onItemClicked = onItemClick,
                        onLongClick = onLongClick,
                        sortOrder = sortOrder,
                        highlightText = if (state.searchDescriptionEnabled == true) highlightText else "",
                        isListView = state.currentViewType == ViewType.LIST,
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        onLinkClicked = onLinkClicked,
                        onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                        listState = listState,
                        gridState = gridState,
                        modifier = Modifier.padding(padding),
                        fileTypeIconMapper = fileTypeIconMapper,
                        inSelectionMode = state.selectedNodes.isNotEmpty(),
                        accountType = state.accountType,
                        nodeSourceType = nodeSourceType,
                    )
                } else {
                    LegacyMegaEmptyViewForSearch(
                        imagePainter = painterResource(
                            id = state.emptyState?.first ?: R.drawable.ic_empty_search
                        ),
                        text = state.emptyState?.second
                            ?: stringResource(id = R.string.search_empty_screen_no_results)
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> T.useDebounce(
    delayMillis: Long = 300L,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onChange: (T) -> Unit,
): T {
    val state by rememberUpdatedState(this)

    DisposableEffect(state) {
        val job = coroutineScope.launch {
            delay(delayMillis)
            onChange(state)
        }
        onDispose {
            job.cancel()
        }
    }
    return state
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchComposeView() {
    SearchComposeView(
        state = SearchActivityState(),
        sortOrder = SortOrder.ORDER_NONE.toString(),
        onItemClick = {},
        onLongClick = {},
        onMenuClick = {},
        onSortOrderClick = { },
        onChangeViewTypeClick = { },
        onLinkClicked = {},
        onDisputeTakeDownClicked = {},
        onErrorShown = {},
        updateFilter = {},
        trackAnalytics = {},
        updateSearchQuery = {},
        onFilterClicked = {},
        onBackPressed = {},
        navHostController = NavHostController(LocalContext.current),
        modifier = Modifier,
        nodeActionHandler = NodeActionHandler(
            LocalContext.current as SearchActivity,
            hiltViewModel()
        ),
        clearSelection = {},
        fileTypeIconMapper = FileTypeIconMapper(),
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
    )
}

/**
 * Test tag for type
 */
internal const val TYPE_DROPDOWN_CHIP_TEST_TAG = "search_compose_view:dropdown_chip_type"

/**
 * Test tag for date modified
 */
internal const val DATE_MODIFIED_DROPDOWN_CHIP_TEST_TAG =
    "search_compose_view:dropdown_chip_date_modified"

/**
 * Test tag for date added
 */
internal const val DATE_ADDED_DROPDOWN_CHIP_TEST_TAG =
    "search_compose_view:dropdown_chip_date_added"
