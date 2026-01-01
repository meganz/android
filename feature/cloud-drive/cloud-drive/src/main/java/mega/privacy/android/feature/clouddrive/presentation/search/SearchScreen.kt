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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.resources.R as sharedR

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
        val topPadding = 12.dp // TODO adjust if there is a header

        Column(
            modifier = Modifier
                .padding(contentPadding.excludingBottomPadding())
        ) {
            when {
                uiState.isLoading -> {
                    NodesViewSkeleton(
                        isListView = isListView,
                        spanCount = spanCount,
                        contentPadding = PaddingValues(top = topPadding),
                    )
                }

                uiState.isPreSearch -> {
                    // TODO show recent searches
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        MegaText(
                            "Search for files and folders",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                uiState.isEmpty -> {
                    // TODO update empty UI
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                        .fillMaxWidth(),
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
                    onMenuClicked = {
                        navigationHandler.navigate(
                            NodeOptionsBottomSheetNavKey(
                                nodeHandle = it.id.longValue,
                                nodeSourceType = uiState.nodeSourceType
                            )
                        )
                    },
                    onItemClicked = {
                        // TODO handle file opening
                    },
                    onLongClicked = {
                        // TODO handle selection mode
                    },
                    sortConfiguration = NodeSortConfiguration.default, // TODO handle sort
                    isListView = isListView,
                    onSortOrderClick = {
                        // TODO handle sort order change
                    },
                    onChangeViewTypeClicked = {
                        // TODO handle view type change
                    },
                    showMediaDiscoveryButton = false,
                    onEnterMediaDiscoveryClick = {
                        // TODO in phase 2
                    },
                    inSelectionMode = uiState.isInSelectionMode, // TODO handle selection mode
                    isContactVerificationOn = uiState.isContactVerificationOn
                )
            }
        }
    }
}

@Composable
private fun SearchTopAppBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExiting by remember { mutableStateOf(false) }
    val localKeyboardController = LocalSoftwareKeyboardController.current

    MegaSearchTopAppBar(
        modifier = modifier,
        query = searchText,
        title = "",
        navigationType = AppBarNavigationType.Back(onBack),
        searchPlaceholder = stringResource(sharedR.string.search_bar_placeholder_text),
        onQueryChanged = {
            if (!isExiting) {
                onSearchTextChanged(it)
            }
        },
        onSearchAction = {
            localKeyboardController?.hide()
        },
        isSearchingMode = true,
        onSearchingModeChanged = { isSearching ->
            if (!isSearching) {
                isExiting = true
                onBack()
            }
        }
    )
}
