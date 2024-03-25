package mega.privacy.android.app.presentation.search.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.model.DateFilterOption
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.model.TypeFilterOption
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewForSearch

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
 * @param onTypeFilterItemClicked a type filter has been clicked
 * @param onDateModifiedFilterItemClicked a date modified filter has been clicked
 * @param onDateAddedFilterItemClicked a date added filter has been clicked
 */
@OptIn(ExperimentalMaterialApi::class)
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
    onTypeFilterItemClicked: (TypeFilterOption?) -> Unit,
    onDateModifiedFilterItemClicked: (DateFilterOption?) -> Unit,
    onDateAddedFilterItemClicked: (DateFilterOption?) -> Unit,
    onBackPressed: () -> Unit,
    navHostController: NavHostController,
    nodeActionHandler: NodeActionHandler,
    clearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scaffoldState = rememberScaffoldState()
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val typeBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val dateModifiedBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val dateAddedBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    BackHandler(
        enabled = typeBottomSheetState.isVisible
                || dateModifiedBottomSheetState.isVisible
                || dateAddedBottomSheetState.isVisible
    ) {
        coroutineScope.launch {
            if (typeBottomSheetState.isVisible) {
                typeBottomSheetState.hide()
            }
            if (dateModifiedBottomSheetState.isVisible) {
                dateModifiedBottomSheetState.hide()
            }
            if (dateAddedBottomSheetState.isVisible) {
                dateAddedBottomSheetState.hide()
            }
        }
    }

    var searchQuery by rememberSaveable {
        mutableStateOf(state.searchQuery)
    }

    searchQuery.useDebounce(onChange = {
        updateSearchQuery(it)
    })

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
        modifier = modifier,
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
                nodeSourceType = state.nodeSourceType
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(snackbarData = data)
            }
        }
    ) { padding ->
        Column {
            if (state.nodeSourceType == NodeSourceType.CLOUD_DRIVE || state.nodeSourceType == NodeSourceType.HOME) {
                if (state.dropdownChipsEnabled) {
                    DropdownChipToolbar(
                        listOf(
                            ChipItem(
                                isSelected = state.typeSelectedFilterOption != null,
                                notSelectedTitle = "Type",
                                selectedFilterTitle = state.typeSelectedFilterOption?.title
                                    ?: "Type",
                                onFilterClicked = {
                                    coroutineScope.launch {
                                        keyboardController?.hide()
                                        typeBottomSheetState.show()
                                    }
                                },
                                testTag = TYPE_DROPDOWN_CHIP_TEST_TAG,
                            ),
                            ChipItem(
                                isSelected = state.dateModifiedSelectedFilterOption != null,
                                notSelectedTitle = "Last modified",
                                selectedFilterTitle = state.dateModifiedSelectedFilterOption?.title
                                    ?: "Last modified",
                                onFilterClicked = {
                                    coroutineScope.launch {
                                        keyboardController?.hide()
                                        dateModifiedBottomSheetState.show()
                                    }
                                },
                                testTag = DATE_MODIFIED_DROPDOWN_CHIP_TEST_TAG,
                            ),
                            ChipItem(
                                isSelected = state.dateAddedSelectedFilterOption != null,
                                notSelectedTitle = "Date added",
                                selectedFilterTitle = state.dateAddedSelectedFilterOption?.title
                                    ?: "Date added",
                                onFilterClicked = {
                                    coroutineScope.launch {
                                        keyboardController?.hide()
                                        dateAddedBottomSheetState.show()
                                    }
                                },
                                testTag = DATE_ADDED_DROPDOWN_CHIP_TEST_TAG,
                            ),
                        )
                    )
                } else {
                    SearchFilterChipsView(
                        filters = state.filters,
                        selectedFilter = state.selectedFilter,
                        updateFilter = {
                            trackAnalytics(it)
                            updateFilter(it)
                        }
                    )
                }
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
                        isListView = state.currentViewType == ViewType.LIST,
                        onSortOrderClick = onSortOrderClick,
                        onChangeViewTypeClick = onChangeViewTypeClick,
                        onLinkClicked = onLinkClicked,
                        onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                        listState = listState,
                        gridState = gridState,
                        modifier = Modifier.padding(padding)
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

        SearchFilterBottomSheet(
            modalSheetState = typeBottomSheetState,
            title = "Type",
            options = TypeFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    option.title,
                    option == state.typeSelectedFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    typeBottomSheetState.hide()
                }
                val typeOption = TypeFilterOption.entries.getOrNull(item.id)
                    ?.takeIf { it.ordinal != state.typeSelectedFilterOption?.ordinal }

                onTypeFilterItemClicked(typeOption)
            }
        )

        SearchFilterBottomSheet(
            modalSheetState = dateModifiedBottomSheetState,
            title = "Last modified",
            options = DateFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    option.title,
                    option == state.dateModifiedSelectedFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    dateModifiedBottomSheetState.hide()
                }
                val dateModifiedOption = DateFilterOption.entries.getOrNull(item.id)
                    ?.takeIf { it.ordinal != state.dateModifiedSelectedFilterOption?.ordinal }

                onDateModifiedFilterItemClicked(dateModifiedOption)
            }
        )

        SearchFilterBottomSheet(
            modalSheetState = dateAddedBottomSheetState,
            title = "Date added",
            options = DateFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    option.title,
                    option == state.dateAddedSelectedFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    dateAddedBottomSheetState.hide()
                }
                val dateAddedOption = DateFilterOption.entries.getOrNull(item.id)
                    ?.takeIf { it.ordinal != state.dateAddedSelectedFilterOption?.ordinal }

                onDateAddedFilterItemClicked(dateAddedOption)
            }
        )
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
        onTypeFilterItemClicked = {},
        onDateModifiedFilterItemClicked = {},
        onDateAddedFilterItemClicked = {},
        onBackPressed = {},
        navHostController = NavHostController(LocalContext.current),
        modifier = Modifier,
        nodeActionHandler = NodeActionHandler(
            LocalContext.current as SearchActivity,
            hiltViewModel()
        ),
        clearSelection = {}
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
