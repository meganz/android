package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterType
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.components.NodesView
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.rememberDynamicSpanCount
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.search.presentation.SearchShellScaffold
import mega.privacy.android.shared.search.presentation.model.SearchEmptyContent
import mega.privacy.mobile.analytics.event.SearchDateAddedDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedDropdownChipPressedEvent

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
    val shellState by viewModel.shellState.collectAsStateWithLifecycle()
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val megaNavigator = rememberMegaNavigator()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )
    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(SearchUiAction.DeselectAllItems)
    }
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val snackbarHostState = LocalSnackBarHostState.current
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val localKeyboardController = LocalSoftwareKeyboardController.current

    SearchShellScaffold(
        modifier = modifier,
        state = shellState,
        landingContent = SearchEmptyContent(
            title = LocalizedText.StringRes(sharedR.string.search_landing_title),
            description = LocalizedText.StringRes(sharedR.string.search_landing_subtitle),
            image = IconPackR.drawable.ic_search_02,
        ),
        emptyContent = SearchEmptyContent(
            title = LocalizedText.StringRes(sharedR.string.photos_search_empty_state_title),
            description = LocalizedText.StringRes(sharedR.string.photos_search_empty_state_description),
            image = IconPackR.drawable.ic_search_02,
        ),
        onSearchTextChange = { viewModel.processAction(SearchUiAction.UpdateSearchText(it)) },
        onBack = navigationHandler::back,
        onRecentSearchSelected = { viewModel.processAction(SearchUiAction.SelectRecentSearch(it)) },
        onClearRecentSearches = { viewModel.processAction(SearchUiAction.ClearRecentSearches) },
        filterOptionsProvider = viewModel::filterOptions,
        onFilterChipClicked = { filterId -> trackFilterChipPressed(filterId) },
        onFilterOptionSelected = viewModel::onFilterOptionSelected,
        topBarOverride = if (uiState.isInSelectionMode) {
            {
                NodeSelectionModeAppBar(
                    count = uiState.selectedItemsCount,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = uiState.isSelecting,
                    onSelectAllClicked = { viewModel.processAction(SearchUiAction.SelectAllItems) },
                    onCancelSelectionClicked = { viewModel.processAction(SearchUiAction.DeselectAllItems) }
                )
            }
        } else null,
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeActionState.availableActions,
                visibleActions = nodeActionState.visibleActions,
                visible = nodeActionState.visibleActions.isNotEmpty() && uiState.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = uiState.selectedNodes,
                isSelecting = uiState.isSelecting,
            )
        },
        loadingContent = {
            NodesViewSkeleton(
                contentPadding = PaddingValues(top = 8.dp),
                isListView = isListView,
                spanCount = spanCount,
            )
        },
    ) { contentPadding ->
        NodesView(
            modifier = Modifier.fillMaxWidth(),
            listContentPadding = PaddingValues(
                top = 8.dp,
                bottom = contentPadding.calculateSafeBottomPadding()
            ),
            spanCount = spanCount,
            items = uiState.items,
            highlightText = uiState.searchedQuery,
            isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
            isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
            showHiddenNodes = uiState.showHiddenNodes,
            onMenuClicked = { nodeUiItem ->
                localKeyboardController?.hide()
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = nodeUiItem.id.longValue,
                        nodeSourceType = uiState.nodeSourceType
                    )
                )
            },
            onItemClicked = {
                localKeyboardController?.hide()
                viewModel.processAction(SearchUiAction.ItemClicked(it))
            },
            onLongClicked = { nodeUiItem ->
                viewModel.processAction(SearchUiAction.ItemLongClicked(nodeUiItem))
            },
            sortConfiguration = uiState.selectedSortConfiguration,
            isListView = isListView,
            onSortOrderClick = { showSortBottomSheet = true },
            onChangeViewTypeClicked = {
                viewModel.processAction(SearchUiAction.ChangeViewTypeClicked)
            },
            showMediaDiscoveryButton = false,
            onEnterMediaDiscoveryClick = { /* No-op */ },
            inSelectionMode = uiState.isInSelectionMode,
            isContactVerificationOn = uiState.isContactVerificationOn
        )
    }

    EventEffect(
        event = uiState.navigateToFolderEvent,
        onConsumed = { viewModel.processAction(SearchUiAction.NavigateToFolderEventConsumed) }
    ) { node ->
        navigationHandler.navigate(
            CloudDriveNavKey(
                nodeHandle = node.id.longValue,
                nodeName = node.name,
                nodeSourceType = uiState.nodeSourceType
            )
        )
    }

    EventEffect(
        event = uiState.navigateBack,
        onConsumed = { viewModel.processAction(SearchUiAction.NavigateBackEventConsumed) }
    ) {
        navigationHandler.back()
    }

    LaunchedEffect(uiState.selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            uiState.selectedNodes.toSet(),
            nodeSourceType = uiState.nodeSourceType
        )
    }

    EventEffect(
        event = nodeActionState.actionTriggeredEvent,
        onConsumed = nodeOptionsActionViewModel::resetActionTriggered
    ) {
        viewModel.processAction(SearchUiAction.DeselectAllItems)
    }

    EventEffect(
        event = nodeActionState.dismissEvent,
        onConsumed = nodeOptionsActionViewModel::resetDismiss
    ) {
        viewModel.processAction(SearchUiAction.DeselectAllItems)
    }

    uiState.openedFileNode?.let { openedFileNode ->
        HandleNodeAction3(
            typedFileNode = openedFileNode,
            snackBarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            onActionHandled = { viewModel.processAction(SearchUiAction.OpenedFileNodeHandled) },
            nodeSourceData = NodeSourceData.Default(uiState.nodeSourceType),
            onDownloadEvent = onTransfer,
            sortOrder = uiState.selectedSortOrder,
            onNavigate = navigationHandler::navigate,
        )
    }

    if (showSortBottomSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(uiState.nodeSourceType),
            sheetState = sortBottomSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = uiState.selectedSortConfiguration.sortOption,
                sortDirection = uiState.selectedSortConfiguration.sortDirection
            ),
            onSortOptionSelected = { result ->
                result?.let {
                    viewModel.processAction(
                        SearchUiAction.SetSortOrder(
                            NodeSortConfiguration(
                                sortOption = it.sortOptionItem,
                                sortDirection = it.sortDirection
                            )
                        )
                    )
                    showSortBottomSheet = false
                }
            },
            onDismissRequest = {
                showSortBottomSheet = false
            }
        )
    }
}

internal fun trackFilterChipPressed(filterId: String) {
    val event = when (runCatching { SearchFilterType.valueOf(filterId) }.getOrNull()) {
        SearchFilterType.TYPE -> SearchFileTypeDropdownChipPressedEvent
        SearchFilterType.LAST_MODIFIED -> SearchLastModifiedDropdownChipPressedEvent
        SearchFilterType.DATE_ADDED -> SearchDateAddedDropdownChipPressedEvent
        null -> return
    }
    Analytics.tracker.trackEvent(event)
}
