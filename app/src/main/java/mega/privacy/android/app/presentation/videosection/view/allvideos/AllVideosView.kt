package mega.privacy.android.app.presentation.videosection.view.allvideos

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewWithImage
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaNode

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun AllVideosView(
    items: List<VideoUIEntity>,
    accountType: AccountType?,
    progressBarShowing: Boolean,
    searchMode: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    selectedLocationFilterOption: LocationFilterOption,
    selectedDurationFilterOption: DurationFilterOption,
    onLocationFilterItemClicked: (LocationFilterOption) -> Unit,
    onDurationFilterItemClicked: (DurationFilterOption) -> Unit,
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
        val locationTitle =
            stringResource(id = sharedR.string.video_section_videos_location_filter_title)
        val durationTitle =
            stringResource(id = sharedR.string.video_section_videos_duration_filter_title)
        Column {
            val isAllLocations = selectedLocationFilterOption == LocationFilterOption.AllLocations
            val isAllDurations = selectedDurationFilterOption == DurationFilterOption.AllDurations

            VideosFilterButtonView(
                isLocationFilterSelected = isAllLocations.not(),
                isDurationFilterSelected = isAllDurations.not(),
                modifier = Modifier.testTag(VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG),
                onDurationFilterClicked = {
                    coroutineScope.launch {
                        durationModalSheetState.show()
                    }
                },
                onLocationFilterClicked = {
                    coroutineScope.launch {
                        locationModalSheetState.show()
                    }
                },
                locationDefaultText = locationTitle,
                durationDefaultText = durationTitle,
                locationFilterSelectText = if (isAllLocations) {
                    locationTitle
                } else {
                    stringResource(id = selectedLocationFilterOption.titleResId)
                },
                durationFilterSelectText = if (isAllDurations) {
                    durationTitle
                } else {
                    stringResource(id = selectedDurationFilterOption.titleResId)
                }
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

                items.isEmpty() -> LegacyMegaEmptyViewWithImage(
                    modifier = Modifier.testTag(VIDEOS_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = R.string.homepage_empty_hint_video),
                    imagePainter = painterResource(id = iconPackR.drawable.ic_video_section_empty_video)
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
                                icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                name = videoItem.name,
                                fileSize = formatFileSize(videoItem.size, LocalContext.current),
                                duration = videoItem.durationString,
                                isFavourite = videoItem.isFavourite,
                                isSelected = videoItem.isSelected,
                                isSharedWithPublicLink = videoItem.isSharedItems,
                                labelColor = if (videoItem.label != MegaNode.NODE_LBL_UNKNOWN)
                                    colorResource(
                                        id = MegaNodeUtil.getNodeLabelColor(
                                            videoItem.label
                                        )
                                    ) else null,
                                thumbnailData = ThumbnailRequest(videoItem.id),
                                nodeAvailableOffline = videoItem.nodeAvailableOffline,
                                onClick = { onClick(videoItem, it) },
                                onMenuClick = { onMenuClick(videoItem) },
                                onLongClick = { onLongClick(videoItem, it) },
                                modifier = Modifier
                                    .alpha(0.5f.takeIf {
                                        accountType?.isPaid == true && (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited)
                                    } ?: 1f),
                                isSensitive = accountType?.isPaid == true && (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited),
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
            title = locationTitle,
            options = LocationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = option == selectedLocationFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    locationModalSheetState.hide()
                }
                val locationOption =
                    if (item.id in LocationFilterOption.entries.indices) {
                        LocationFilterOption.entries.firstOrNull { it.ordinal == item.id }
                            ?: LocationFilterOption.AllLocations
                    } else {
                        LocationFilterOption.AllLocations
                    }
                onLocationFilterItemClicked(locationOption)
            }
        )

        VideosFilterBottomSheet(
            modifier = Modifier,
            modalSheetState = durationModalSheetState,
            coroutineScope = coroutineScope,
            title = durationTitle,
            options = DurationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = option == selectedDurationFilterOption
                )
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    durationModalSheetState.hide()
                }
                val durationOption =
                    if (item.id in DurationFilterOption.entries.indices) {
                        DurationFilterOption.entries.firstOrNull { it.ordinal == item.id }
                            ?: DurationFilterOption.AllDurations
                    } else {
                        DurationFilterOption.AllDurations
                    }
                onDurationFilterItemClicked(durationOption)
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AllVideosViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        AllVideosView(
            items = emptyList(),
            accountType = null,
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
            selectedLocationFilterOption = LocationFilterOption.AllLocations,
            selectedDurationFilterOption = DurationFilterOption.MoreThan20,
            onLocationFilterItemClicked = { },
            onDurationFilterItemClicked = { },
        )
    }
}

/**
 * Test tag for the videos progress bar.
 */
const val VIDEOS_PROGRESS_BAR_TEST_TAG = "all_videos:progress_bar"

/**
 * Test tag for the videos empty view.
 */
const val VIDEOS_EMPTY_VIEW_TEST_TAG = "all_videos:empty_view"

/**
 * Test tag for the videos filter button view.
 */
const val VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG = "all_videos:button_filter"

/**
 * Test tag for the videos list.
 */
const val VIDEOS_LIST_TEST_TAG = "all_videos:videos_list"