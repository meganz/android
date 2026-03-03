package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SelectVideoGridView
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SelectVideoListView
import mega.privacy.android.feature.photos.presentation.videos.VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.SelectVideosForPlaylistNavKey
import mega.privacy.android.shared.resources.R as sharedR


@Composable
fun SelectVideosForPlaylistRoute(
    isNewlyCreated: Boolean,
    playlistHandle: Long,
    onNavigateToFolder: (NavKey) -> Unit,
    returnResult: (String, Int) -> Unit,
    onBack: () -> Unit,
    navigateAndClearTo: () -> Unit,
    viewModel: SelectVideosForPlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateToFolderEvent by viewModel.navigateToFolderEvent.collectAsStateWithLifecycle()
    val numberOfAddedVideosEvent by viewModel.numberOfAddedVideosEvent.collectAsStateWithLifecycle()

    val dataState = uiState as? SelectVideosForPlaylistUiState.Data

    EventEffect(
        event = navigateToFolderEvent,
        onConsumed = viewModel::resetNavigateToFolderEvent
    ) { item ->
        onNavigateToFolder(
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

    SelectVideosForPlaylistScreen(
        uiState = uiState,
        searchQuery = dataState?.query,
        updateSearchQuery = viewModel::searchQuery,
        onSortNodes = viewModel::setCloudSortOrder,
        onChangeViewTypeClick = viewModel::changeViewTypeClicked,
        onItemClicked = viewModel::itemClicked,
        clearSelection = viewModel::clearSelection,
        confirmAddVideos = {
            dataState?.selectItemHandles?.takeIf { it.isNotEmpty() }?.let { handles ->
                viewModel.addVideosToPlaylist(videoIDs = handles.map { NodeId(it) })
            }
        },
        onBackPressed = {
            if (isNewlyCreated && dataState?.isCloudDriveRoot == true) {
                navigateAndClearTo()
            } else {
                onBack()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectVideosForPlaylistScreen(
    uiState: SelectVideosForPlaylistUiState,
    searchQuery: String?,
    updateSearchQuery: (String?) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    onChangeViewTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onItemClicked: (SelectVideoItemUiEntity) -> Unit = {},
    confirmAddVideos: () -> Unit = {},
    clearSelection: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val dataState = uiState as? SelectVideosForPlaylistUiState.Data

    BackHandler(onBack = onBackPressed)

    var isSearchBarVisible by rememberSaveable { mutableStateOf(false) }
    BackHandler(isSearchBarVisible) {
        if (isSearchBarVisible) {
            isSearchBarVisible = false
            updateSearchQuery(null)
        }
    }

    BackHandler(dataState?.selectItemHandles?.isNotEmpty() == true) {
        clearSelection()
    }

    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        floatingActionButton = {
            if (dataState?.selectItemHandles?.isNotEmpty() == true) {
                MegaFab(
                    onClick = confirmAddVideos,
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
                )
            }
        },
        topBar = {
            MegaSearchTopAppBar(
                modifier = Modifier.testTag(SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG),
                navigationType = AppBarNavigationType.Back(onBackPressed),
                title = when {
                    dataState?.selectItemHandles?.isNotEmpty() == true ->
                        dataState.selectItemHandles.size.toString()

                    dataState?.isCloudDriveRoot == true -> stringResource(sharedR.string.video_section_video_selected_top_bar_title)
                    else -> dataState?.title?.text ?: ""
                },
                query = searchQuery,
                onQueryChanged = updateSearchQuery,
                onSearchingModeChanged = { isSearching ->
                    isSearchBarVisible = isSearching
                    if (!isSearching) {
                        updateSearchQuery(null)
                    }
                },
                isSearchingMode = isSearchBarVisible,
                actions = emptyList()
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (uiState) {
                is SelectVideosForPlaylistUiState.Loading -> NodesViewSkeleton(
                    modifier = Modifier.testTag(SELECT_VIDEOS_LOADING_VIEW_TEST_TAG),
                    isListView = true,
                    contentPadding = PaddingValues()
                )

                is SelectVideosForPlaylistUiState.Data -> if (uiState.items.isEmpty()) {
                    MegaEmptyView(
                        modifier = Modifier.testTag(SELECT_VIDEOS_EMPTY_VIEW_TEST_TAG),
                        text = stringResource(id = sharedR.string.videos_tab_empty_hint_video),
                        imagePainter = painterResource(id = iconPackR.drawable.ic_video_glass)
                    )
                } else {
                    if (uiState.currentViewType == ViewType.LIST) {
                        SelectVideoListView(
                            modifier = Modifier.testTag(SELECT_VIDEOS_LIST_VIEW_TAG),
                            items = uiState.items,
                            listState = listState,
                            sortConfiguration = uiState.selectedSortConfiguration,
                            onChangeViewTypeClick = onChangeViewTypeClick,
                            onSortOrderClick = { showSortBottomSheet = true },
                            onItemsClicked = onItemClicked,
                            isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                            showHiddenItems = uiState.showHiddenItems,
                            listContentPadding = PaddingValues(
                                bottom = innerPadding.calculateSafeBottomPadding()
                            )
                        )
                    } else {
                        SelectVideoGridView(
                            modifier = Modifier.testTag(SELECT_VIDEOS_GRID_VIEW_TAG),
                            items = uiState.items,
                            gridState = gridState,
                            onItemClicked = onItemClicked,
                            sortConfiguration = uiState.selectedSortConfiguration,
                            onChangeViewTypeClick = onChangeViewTypeClick,
                            onSortOrderClick = { showSortBottomSheet = true },
                            isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                            showHiddenItems = uiState.showHiddenItems,
                            listContentPadding = PaddingValues(
                                bottom = innerPadding.calculateSafeBottomPadding()
                            )
                        )
                    }

                    if (showSortBottomSheet) {
                        SortBottomSheet(
                            modifier = Modifier.testTag(VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG),
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

/**
 * Test tag for the loading view of select videos screen
 */
const val SELECT_VIDEOS_LOADING_VIEW_TEST_TAG = "select_videos:view_loading"

/**
 * Test tag for the empty view of select videos screen
 */
const val SELECT_VIDEOS_EMPTY_VIEW_TEST_TAG = "select_videos:view_empty"

/**
 * The test tag for search top app bar of select videos screen
 */
const val SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG = "select_videos:top_bar_search"

/**
 * The test tag for list view of select videos screen
 */
const val SELECT_VIDEOS_LIST_VIEW_TAG = "select_videos:list_view"

/**
 * The test tag for grid view of select videos screen
 */
const val SELECT_VIDEOS_GRID_VIEW_TAG = "select_videos:grid_view"