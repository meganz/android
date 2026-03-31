package mega.privacy.android.feature.photos.presentation.playlists.videoselect.view

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.android.core.ui.modifiers.conditional
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.SelectVideoSearchUiState
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.SelectVideosSearchViewModel
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.navigation.destination.SelectVideosForPlaylistNavKey
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun SelectVideosSearchRoute(
    isNewlyCreated: Boolean,
    playlistHandle: Long,
    navigate: (NavKey) -> Unit,
    returnResult: (String, Int) -> Unit,
    onBack: () -> Unit,
    navigateAndClearTo: () -> Unit,
    viewModel: SelectVideosSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateToFolderEvent by viewModel.navigateToFolderEvent.collectAsStateWithLifecycle()
    val numberOfAddedVideosEvent by viewModel.numberOfAddedVideosEvent.collectAsStateWithLifecycle()

    val localKeyboardController = LocalSoftwareKeyboardController.current

    val dataState = uiState as? SelectVideoSearchUiState.Data

    EventEffect(
        event = navigateToFolderEvent,
        onConsumed = viewModel::resetNavigateToFolderEvent
    ) { item ->
        navigate(
            SelectVideosForPlaylistNavKey(
                item.id.longValue,
                item.name,
                playlistHandle,
                isNewlyCreated
            )
        )
    }

    EventEffect(
        event = numberOfAddedVideosEvent,
        onConsumed = viewModel::resetNumberOfAddedVideosEvent,
    ) { numberOfAddedVideos ->
        returnResult(SelectVideosForPlaylistNavKey.RESULT, numberOfAddedVideos)
        navigateAndClearTo()
    }

    SelectVideosSearchScreen(
        uiState = uiState,
        onSortNodes = viewModel::setCloudSortOrder,
        onChangeViewTypeClick = viewModel::changeViewTypeClicked,
        onItemClicked = {
            localKeyboardController?.hide()
            viewModel.itemClicked(it)
        },
        onBackPressed = {
            when {
                dataState?.selectItemHandles?.isNotEmpty() == true -> viewModel.clearSelection()
                isNewlyCreated && dataState?.isCloudDriveRoot == true -> navigateAndClearTo()
                else -> onBack()
            }
        },
        confirmAddVideos = {
            dataState?.selectItemHandles?.takeIf { it.isNotEmpty() }?.let { handles ->
                viewModel.addVideosToPlaylist(videoIDs = handles.map { NodeId(it) })
            }
        },
        selectAll = viewModel::selectAll,
        updateSearchQuery = viewModel::searchQuery
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectVideosSearchScreen(
    uiState: SelectVideoSearchUiState,
    updateSearchQuery: (String?) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    onChangeViewTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onItemClicked: (SelectVideoItemUiEntity) -> Unit = {},
    confirmAddVideos: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    selectAll: () -> Unit = {},
) {
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val localKeyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val dataState = uiState as? SelectVideoSearchUiState.Data
    val videoSelectedCount = dataState?.selectItemHandles?.size ?: 0
    val areAllVideosSelected = dataState?.areAllSelected ?: false

    BackHandler(onBack = onBackPressed)

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier,
        topBar = {
            val selectedHandles = dataState?.selectItemHandles

            if (!selectedHandles.isNullOrEmpty()) {
                MegaTopAppBar(
                    modifier = Modifier.testTag(SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG),
                    title = selectedHandles.size.toString(),
                    navigationType = AppBarNavigationType.Close(onBackPressed),
                    actions = buildList {
                        if (videoSelectedCount > 0 && !areAllVideosSelected) {
                            add(CommonMenuAction.SelectAll)
                        }
                    },
                    onActionPressed = {
                        if (it is CommonMenuAction.SelectAll) {
                            localKeyboardController?.hide()
                            selectAll()
                        }
                    }
                )
            } else {
                SearchTopAppBar(
                    modifier = Modifier.testTag(SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG),
                    searchText = dataState?.searchText ?: "",
                    placeholderText = dataState?.placeholderText?.text ?: "",
                    onQueryChanged = updateSearchQuery,
                    onBack = onBackPressed,
                    focusRequester = focusRequester
                )
            }
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                modifier = Modifier.testTag(SELECT_VIDEOS_SEARCH_BOTTOM_VIEW_ROW_TEST_TAG),
                primaryButtonText = stringResource(sharedR.string.video_to_playlist_add_button),
                textOnlyButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                onPrimaryButtonClick = confirmAddVideos,
                onTextOnlyButtonClick = onBackPressed,
                primaryButtonEnabled = dataState?.selectItemHandles?.isNotEmpty() == true
            )
        }
    ) { contentPadding ->
        when (uiState) {
            is SelectVideoSearchUiState.Loading -> {
                NodesViewSkeleton(
                    modifier = Modifier
                        .padding(contentPadding)
                        .testTag(SELECT_VIDEOS_SEARCH_LOADING_VIEW_TEST_TAG),
                    contentPadding = PaddingValues(top = 8.dp),
                    isListView = dataState?.currentViewType == ViewType.LIST
                )
            }

            is SelectVideoSearchUiState.Data -> {
                when {
                    uiState.items.isEmpty() -> SearchEmptyView(
                        title = stringResource(id = sharedR.string.search_landing_title),
                        description =
                            SpannableText(text = stringResource(id = sharedR.string.select_videos_search_empty_view_description)),
                        modifier = Modifier
                            .padding(contentPadding)
                            .testTag(SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG)
                    )

                    else -> {
                        Box(
                            modifier = Modifier
                                .padding(
                                    PaddingValues(
                                        top = contentPadding.calculateTopPadding(),
                                        end = contentPadding.calculateEndPadding(
                                            LocalLayoutDirection.current
                                        )
                                    )
                                )
                        ) {
                            if (uiState.currentViewType == ViewType.LIST) {
                                SelectVideoListView(
                                    modifier = Modifier
                                        .testTag(SELECT_VIDEOS_SEARCH_LIST_VIEW_TAG),
                                    items = uiState.items,
                                    listState = listState,
                                    sortConfiguration = uiState.selectedSortConfiguration,
                                    onChangeViewTypeClick = onChangeViewTypeClick,
                                    onSortOrderClick = { showSortBottomSheet = true },
                                    onItemsClicked = onItemClicked,
                                    isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                                    showHiddenItems = uiState.showHiddenItems,
                                    listContentPadding = PaddingValues(
                                        bottom = contentPadding.calculateSafeBottomPadding()
                                    ),
                                    isSelectionMode = uiState.selectItemHandles.isNotEmpty()
                                )
                            } else {
                                SelectVideoGridView(
                                    modifier = Modifier
                                        .testTag(SELECT_VIDEOS_SEARCH_GRID_VIEW_TAG),
                                    items = uiState.items,
                                    gridState = gridState,
                                    onItemClicked = onItemClicked,
                                    sortConfiguration = uiState.selectedSortConfiguration,
                                    onChangeViewTypeClick = onChangeViewTypeClick,
                                    onSortOrderClick = { showSortBottomSheet = true },
                                    isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                                    showHiddenItems = uiState.showHiddenItems,
                                    listContentPadding = PaddingValues(
                                        bottom = contentPadding.calculateSafeBottomPadding()
                                    ),
                                    isSelectionMode = uiState.selectItemHandles.isNotEmpty()
                                )
                            }
                        }

                        if (showSortBottomSheet) {
                            SortBottomSheet(
                                title = stringResource(sharedR.string.action_sort_by_header),
                                options = NodeSortOption.getOptionsForSourceType(NodeSourceType.CLOUD_DRIVE),
                                sheetState = sortBottomSheetState,
                                selectedSort = SortBottomSheetResult(
                                    sortOptionItem = uiState.selectedSortConfiguration.sortOption,
                                    sortDirection = uiState.selectedSortConfiguration.sortDirection
                                ),
                                onSortOptionSelected = { result ->
                                    result?.let {
                                        onSortNodes(
                                            NodeSortConfiguration(
                                                sortOption = it.sortOptionItem,
                                                sortDirection = it.sortDirection
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
                }
            }
        }
    }
}

@Composable
private fun SearchTopAppBar(
    searchText: String,
    placeholderText: String,
    onQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    var isExiting by remember { mutableStateOf(false) }
    val localFocusManager = LocalFocusManager.current
    val localKeyboardController = LocalSoftwareKeyboardController.current

    MegaSearchTopAppBar(
        modifier = modifier,
        navigationType = AppBarNavigationType.Back(onBack),
        title = "",
        searchPlaceholder = placeholderText,
        query = searchText,
        onQueryChanged = {
            if (!isExiting) {
                onQueryChanged(it)
            }
        },
        onSearchingModeChanged = { isSearching ->
            if (!isSearching) {
                isExiting = true
                onBack()
            }
        },
        isSearchingMode = true,
        onSearchAction = {
            localFocusManager.clearFocus()
            localKeyboardController?.hide()
        },
        focusRequester = focusRequester
    )
}

@Composable
private fun SearchEmptyView(
    title: String,
    description: SpannableText,
    modifier: Modifier = Modifier,
) {
    val isLandscapeMode =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    EmptyStateView(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .conditional(!isLandscapeMode) {
                imePadding()
            },
        title = title,
        description = description,
        illustration = IconPackR.drawable.ic_search_02
    )
}

/**
 * Test tag for the loading view of select videos search screen
 */
const val SELECT_VIDEOS_SEARCH_LOADING_VIEW_TEST_TAG = "select_videos_search:view_loading"

/**
 * Test tag for the empty view of select videos search screen
 */
const val SELECT_VIDEOS_SEARCH_EMPTY_VIEW_TEST_TAG = "select_videos_search:view_empty"

/**
 * The test tag for search top app bar of select videos screen
 */
const val SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG = "select_videos_search:top_bar_search"

/**
 * The test tag for selection top app bar of select videos search screen
 */
const val SELECT_VIDEOS_SEARCH_SELECTION_TOP_APP_BAR_TAG = "select_videos_search:top_bar_selection"


/**
 * The test tag for list view of select videos search screen
 */
const val SELECT_VIDEOS_SEARCH_LIST_VIEW_TAG = "select_videos_search:list_view"

/**
 * The test tag for grid view of select videos search screen
 */
const val SELECT_VIDEOS_SEARCH_GRID_VIEW_TAG = "select_videos_search:grid_view"

/**
 * The test tag for bottom view of select videos search screen
 */
const val SELECT_VIDEOS_SEARCH_BOTTOM_VIEW_ROW_TEST_TAG =
    "select_videos_search_bottom_view:view_row"