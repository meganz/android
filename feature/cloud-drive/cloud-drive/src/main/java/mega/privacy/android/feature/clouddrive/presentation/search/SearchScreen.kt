package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiState
import mega.privacy.android.feature.clouddrive.presentation.search.view.FilterType
import mega.privacy.android.feature.clouddrive.presentation.search.view.SearchFilterChips
import mega.privacy.android.feature.clouddrive.presentation.search.view.SearchTopAppBar
import mega.privacy.android.navigation.contract.NavigationHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            SearchTopAppBar(
                searchText = uiState.searchText,
                onSearchTextChanged = {
                    viewModel.processAction(SearchUiAction.UpdateSearchText(it))
                },
                onBack = navigationHandler::back,
            )
        }
    ) { contentPadding ->
        SearchContent(
            uiState = uiState,
            contentPadding = contentPadding,
            isListView = isListView,
            spanCount = spanCount,
            onFilterClicked = { filterType ->
                // TODO: Navigate to filter bottom sheet
            },
            onMenuClicked = { nodeUiItem ->
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = nodeUiItem.id.longValue,
                        nodeSourceType = uiState.nodeSourceType
                    )
                )
            },
            onItemClicked = { nodeUiItem ->
                // TODO handle file opening
            },
            onLongClicked = { nodeUiItem ->
                // TODO handle selection mode
            },
            onSortOrderClick = {
                // TODO handle sort order change
            },
            onChangeViewTypeClicked = {
                // TODO handle view type change
            },
            onEnterMediaDiscoveryClick = {
                // TODO in phase 2
            },
        )
    }
}

@Composable
fun SearchContent(
    uiState: SearchUiState,
    contentPadding: PaddingValues,
    isListView: Boolean,
    spanCount: Int,
    onFilterClicked: (FilterType) -> Unit,
    onMenuClicked: (NodeUiItem<TypedNode>) -> Unit,
    onItemClicked: (NodeUiItem<TypedNode>) -> Unit,
    onLongClicked: (NodeUiItem<TypedNode>) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClicked: () -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = 12.dp

    Column(
        modifier = modifier.padding(contentPadding.excludingBottomPadding())
    ) {
        if (uiState.isFilterAllowed) {
            SearchFilterChips(
                typeFilterOption = uiState.typeFilterOption,
                dateModifiedFilterOption = uiState.dateModifiedFilterOption,
                dateAddedFilterOption = uiState.dateAddedFilterOption,
                onFilterClicked = onFilterClicked,
            )
        }

        when {
            uiState.isLoading -> {
                NodesViewSkeleton(
                    isListView = isListView,
                    spanCount = spanCount,
                    contentPadding = PaddingValues(top = topPadding),
                )
            }

            uiState.isPreSearch -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SEARCH_CONTENT_PRE_SEARCH_TAG),
                ) {
                    MegaText(
                        "Search for files and folders",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            uiState.isEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SEARCH_CONTENT_EMPTY_TAG),
                ) {
                    MegaText(
                        "No results found",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            else -> NodesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SEARCH_CONTENT_RESULTS_TAG),
                listContentPadding = PaddingValues(
                    top = topPadding,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                ),
                spanCount = spanCount,
                items = uiState.items,
                highlightText = uiState.searchedQuery,
                isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                showHiddenNodes = uiState.showHiddenNodes,
                onMenuClicked = onMenuClicked,
                onItemClicked = onItemClicked,
                onLongClicked = onLongClicked,
                sortConfiguration = NodeSortConfiguration.default,
                isListView = isListView,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClicked = onChangeViewTypeClicked,
                showMediaDiscoveryButton = false,
                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                inSelectionMode = uiState.isInSelectionMode,
                isContactVerificationOn = uiState.isContactVerificationOn
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SearchContentPreSearchPreview() {
    AndroidThemeForPreviews {
        SearchContent(
            uiState = SearchUiState(),
            contentPadding = PaddingValues(0.dp),
            isListView = true,
            spanCount = 2,
            onFilterClicked = {},
            onMenuClicked = {},
            onItemClicked = {},
            onLongClicked = {},
            onSortOrderClick = {},
            onChangeViewTypeClicked = {},
            onEnterMediaDiscoveryClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SearchContentEmptyPreview() {
    AndroidThemeForPreviews {
        SearchContent(
            uiState = SearchUiState(
                searchText = "test",
                searchedQuery = "test",
                nodesLoadingState = NodesLoadingState.FullyLoaded,
            ),
            contentPadding = PaddingValues(0.dp),
            isListView = true,
            spanCount = 2,
            onFilterClicked = {},
            onMenuClicked = {},
            onItemClicked = {},
            onLongClicked = {},
            onSortOrderClick = {},
            onChangeViewTypeClicked = {},
            onEnterMediaDiscoveryClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SearchContentLoadingPreview() {
    AndroidThemeForPreviews {
        SearchContent(
            uiState = SearchUiState(
                searchText = "test",
                nodesLoadingState = NodesLoadingState.Loading,
            ),
            contentPadding = PaddingValues(0.dp),
            isListView = true,
            spanCount = 2,
            onFilterClicked = {},
            onMenuClicked = {},
            onItemClicked = {},
            onLongClicked = {},
            onSortOrderClick = {},
            onChangeViewTypeClicked = {},
            onEnterMediaDiscoveryClick = {},
        )
    }
}

internal const val SEARCH_CONTENT_PRE_SEARCH_TAG = "search_content:pre_search"
internal const val SEARCH_CONTENT_EMPTY_TAG = "search_content:empty"
internal const val SEARCH_CONTENT_RESULTS_TAG = "search_content:results"