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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SelectVideoGridView
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.view.SelectVideoListView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectVideosForPlaylistScreen(
    uiState: SelectVideosForPlaylistUiState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onItemClicked: (SelectVideoItemUiEntity) -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val dataState = uiState as? SelectVideosForPlaylistUiState.Data
    BackHandler(dataState?.isCloudDriveRoot != true) {
        onBackPressed()
    }
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaSearchTopAppBar(
                modifier = Modifier.testTag(SELECT_VIDEOS_SEARCH_TOP_APP_BAR_TAG),
                navigationType = AppBarNavigationType.Back(onBackPressed),
                title = if (dataState?.isCloudDriveRoot == true) {
                    stringResource(sharedR.string.video_section_video_selected_top_bar_title)
                } else {
                    dataState?.title?.text ?: ""
                },
                query = null,
                onQueryChanged = {
                    //TODO implement search functionality
                },
                onSearchingModeChanged = {
                    //TODO implement search functionality
                },
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
                            onChangeViewTypeClick = {
                                //TODO implement change view type functionality
                            },
                            onSortOrderClick = {
                                //TODO implement sort order functionality
                            },
                            onItemsClicked = onItemClicked,
                            isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                            showHiddenItems = uiState.showHiddenItems
                        )
                    } else {
                        SelectVideoGridView(
                            modifier = Modifier.testTag(SELECT_VIDEOS_GRID_VIEW_TAG),
                            items = uiState.items,
                            gridState = gridState,
                            onItemClicked = onItemClicked,
                            sortConfiguration = uiState.selectedSortConfiguration,
                            onChangeViewTypeClick = {
                                //TODO implement change view type functionality
                            },
                            onSortOrderClick = {
                                //TODO implement change view type functionality
                            },
                            isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                            showHiddenItems = uiState.showHiddenItems
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