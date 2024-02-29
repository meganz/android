package mega.privacy.android.app.presentation.videosection.view.allvideos

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for the videos progress bar.
 */
const val VIDEOS_PROGRESS_BAR_TEST_TAG = "videos_progress_bar_test_tag"

/**
 * Test tag for the videos empty view.
 */
const val VIDEOS_EMPTY_VIEW_TEST_TAG = "videos_empty_view_test_tag"

/**
 * Test tag for the videos filter button view.
 */
const val VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG = "videos_filter_button_view_test_tag"

/**
 * Test tag for the videos list.
 */
const val VIDEOS_LIST_TEST_TAG = "videos_list_test_tag"

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun AllVideosView(
    items: List<VideoUIEntity>,
    progressBarShowing: Boolean,
    searchMode: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    selectedLocationFilterOption: LocationFilterOption?,
    selectedDurationFilterOption: DurationFilterOption?,
    onLocationFilterItemClicked: (LocationFilterOption?) -> Unit,
    onDurationFilterItemClicked: (DurationFilterOption?) -> Unit,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: VideoUIEntity, index: Int) -> Unit) = { _, _ -> },
) {
    val coroutineScope = rememberCoroutineScope()
    val locationModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )

    val durationModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )

    LaunchedEffect(items) {
        if (scrollToTop) {
            lazyListState.scrollToItem(0)
        }
    }

    BackHandler(enabled = locationModalSheetState.isVisible || durationModalSheetState.isVisible) {
        coroutineScope.launch {
            if (locationModalSheetState.isVisible) {
                locationModalSheetState.hide()
            }

            if (durationModalSheetState.isVisible) {
                durationModalSheetState.hide()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        scaffoldState = rememberScaffoldState(),
    ) { paddingValue ->
        Column {
            val locationText = "Location"
            val durationText = "Duration"

            VideosFilterButtonView(
                isLocationFilterSelected = selectedLocationFilterOption != null,
                isDurationFilterSelected = selectedDurationFilterOption != null,
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                    .testTag(VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG),
                onDurationFilterClicked = {
                    if (selectedDurationFilterOption == null) {
                        coroutineScope.launch {
                            durationModalSheetState.show()
                        }
                    } else {
                        onDurationFilterItemClicked(null)
                    }
                },
                onLocationFilterClicked = {
                    if (selectedLocationFilterOption == null) {
                        coroutineScope.launch {
                            locationModalSheetState.show()
                        }
                    } else {
                        onLocationFilterItemClicked(null)
                    }
                },
                locationDefaultText = locationText,
                durationDefaultText = durationText,
                locationFilterSelectText = selectedLocationFilterOption?.title ?: locationText,
                durationFilterSelectText = selectedDurationFilterOption?.title ?: durationText
            )

            when {
                progressBarShowing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp)
                            .testTag(VIDEOS_PROGRESS_BAR_TEST_TAG),
                        contentAlignment = Alignment.TopCenter,
                        content = {
                            MegaCircularProgressIndicator(
                                modifier = Modifier
                                    .size(50.dp),
                                strokeWidth = 4.dp,
                            )
                        },
                    )
                }

                items.isEmpty() -> LegacyMegaEmptyView(
                    modifier = Modifier.testTag(VIDEOS_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = R.string.homepage_empty_hint_video),
                    imagePainter = painterResource(id = R.drawable.ic_homepage_empty_video)
                )

                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .padding(paddingValue)
                            .testTag(VIDEOS_LIST_TEST_TAG)
                    ) {
                        if (!searchMode) {
                            item(
                                key = "header"
                            ) {
                                HeaderViewItem(
                                    modifier = Modifier.padding(
                                        vertical = 10.dp,
                                        horizontal = 8.dp
                                    ),
                                    onSortOrderClick = onSortOrderClick,
                                    onChangeViewTypeClick = {},
                                    onEnterMediaDiscoveryClick = {},
                                    sortOrder = sortOrder,
                                    isListView = true,
                                    showSortOrder = true,
                                    showChangeViewType = false,
                                    showMediaDiscoveryButton = false,
                                )
                            }
                        }

                        items(count = items.size, key = { items[it].id.longValue }) {
                            val videoItem = items[it]
                            VideoItemView(
                                icon = iconPackR.drawable.ic_video_list,
                                name = videoItem.name,
                                fileSize = formatFileSize(videoItem.size, LocalContext.current),
                                duration = videoItem.durationString,
                                isFavourite = videoItem.isFavourite,
                                isSelected = videoItem.isSelected,
                                thumbnailData = if (videoItem.thumbnail?.exists() == true) {
                                    videoItem.thumbnail
                                } else {
                                    ThumbnailRequest(videoItem.id)
                                },
                                nodeAvailableOffline = videoItem.nodeAvailableOffline,
                                onClick = { onClick(videoItem, it) },
                                onMenuClick = { onMenuClick(videoItem) },
                                onLongClick = { onLongClick(videoItem, it) }
                            )
                        }
                    }
                }
            }
        }
        VideosFilterBottomSheet(
            modifier = Modifier,
            modalSheetState = locationModalSheetState,
            coroutineScope = coroutineScope,
            title = "Location",
            options = LocationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    option.ordinal,
                    option.name,
                    option == selectedLocationFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    locationModalSheetState.hide()
                }
                val locationOption =
                    if (item.id in LocationFilterOption.entries.indices) {
                        LocationFilterOption.entries.firstOrNull { it.ordinal == item.id }
                    } else {
                        null
                    }
                onLocationFilterItemClicked(locationOption)
            }
        )

        VideosFilterBottomSheet(
            modifier = Modifier,
            modalSheetState = durationModalSheetState,
            coroutineScope = coroutineScope,
            title = "Duration",
            options = DurationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    option.ordinal,
                    option.title,
                    option == selectedDurationFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    durationModalSheetState.hide()
                }
                val durationOption =
                    if (item.id in DurationFilterOption.entries.indices) {
                        DurationFilterOption.entries.firstOrNull { it.ordinal == item.id }
                    } else {
                        null
                    }
                onDurationFilterItemClicked(durationOption)
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AllVideosViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AllVideosView(
            items = emptyList(),
            progressBarShowing = false,
            searchMode = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            modifier = Modifier,
            onClick = { _, _ -> },
            onMenuClick = { },
            onSortOrderClick = { },
            onLongClick = { _, _ -> },
            selectedLocationFilterOption = null,
            selectedDurationFilterOption = DurationFilterOption.MoreThan20,
            onLocationFilterItemClicked = { },
            onDurationFilterItemClicked = { }
        )
    }
}