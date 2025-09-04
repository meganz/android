package mega.privacy.android.app.presentation.videosection.view.allvideos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionLoadingView
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewWithImage
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.layouts.FastScrollLazyColumn
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DurationFilterAllDurationsClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterBetween10and60SecondsClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterBetween1and4MinutesClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterBetween4and20MinutesClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterLessThan10SecondsClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterMoreThan20MinutesClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterAllLocationsClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterCameraUploadClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterCloudDriveClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterSharedItemClickedEvent
import nz.mega.sdk.MegaNode

@Composable
internal fun AllVideosView(
    items: List<VideoUIEntity>,
    shouldApplySensitiveMode: Boolean,
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
    addToPlaylistsTitles: List<String>?,
    clearAddToPlaylistsTitles: () -> Unit,
    retryActionCallback: () -> Unit,
    onLongClick: ((item: VideoUIEntity, index: Int) -> Unit) = { _, _ -> },
    highlightText: String = ""
) {
    val coroutineScope = rememberCoroutineScope()
    val locationModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )

    val durationModalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(addToPlaylistsTitles) {
        addToPlaylistsTitles?.let { titles ->
            if (titles.isNotEmpty()) {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    context.resources.getQuantityString(
                        sharedR.plurals.video_section_playlists_add_to_playlists_successfully_message,
                        titles.size,
                        if (titles.size == 1) titles.first() else titles.size
                    )
                )
            } else {
                val result = scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    message = context.getString(sharedR.string.video_section_playlists_add_to_playlists_failed_message),
                    actionLabel = context.getString(R.string.message_option_retry)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    retryActionCallback()
                }
            }
            clearAddToPlaylistsTitles()
        }
    }

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

    MegaScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
    ) {
        val locationTitle =
            stringResource(id = sharedR.string.video_section_videos_location_filter_title)
        val durationTitle =
            stringResource(id = sharedR.string.video_section_videos_duration_filter_title)
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
        ) {
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
                progressBarShowing -> VideoSectionLoadingView()

                items.isEmpty() -> LegacyMegaEmptyViewWithImage(
                    modifier = Modifier.testTag(VIDEOS_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = R.string.homepage_empty_hint_video),
                    imagePainter = painterResource(id = iconPackR.drawable.ic_video_glass)
                )

                else -> {
                    FastScrollLazyColumn(
                        state = lazyListState,
                        totalItems = items.size,
                        modifier = modifier
                            .testTag(VIDEOS_LIST_TEST_TAG),
                    ) {
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

                        items(count = items.size, key = { items[it].id.longValue }) {
                            val videoItem = items[it]
                            VideoItemView(
                                icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                name = videoItem.name,
                                description = videoItem.description?.replace("\n", " "),
                                tags = videoItem.tags,
                                fileSize = formatFileSize(videoItem.size, LocalContext.current),
                                duration = videoItem.duration,
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
                                highlightText = highlightText,
                                onClick = { onClick(videoItem, it) },
                                onMenuClick = { onMenuClick(videoItem) },
                                onLongClick = { onLongClick(videoItem, it) },
                                isSensitive = shouldApplySensitiveMode && (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited),
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
                when (locationOption) {
                    LocationFilterOption.AllLocations ->
                        Analytics.tracker.trackEvent(LocationFilterAllLocationsClickedEvent)

                    LocationFilterOption.CloudDrive ->
                        Analytics.tracker.trackEvent(LocationFilterCloudDriveClickedEvent)

                    LocationFilterOption.CameraUploads ->
                        Analytics.tracker.trackEvent(LocationFilterCameraUploadClickedEvent)

                    LocationFilterOption.SharedItems ->
                        Analytics.tracker.trackEvent(LocationFilterSharedItemClickedEvent)
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

                when (durationOption) {
                    DurationFilterOption.AllDurations ->
                        Analytics.tracker.trackEvent(DurationFilterAllDurationsClickedEvent)

                    DurationFilterOption.LessThan10Seconds ->
                        Analytics.tracker.trackEvent(DurationFilterLessThan10SecondsClickedEvent)

                    DurationFilterOption.Between10And60Seconds ->
                        Analytics.tracker.trackEvent(DurationFilterBetween10and60SecondsClickedEvent)

                    DurationFilterOption.Between1And4 ->
                        Analytics.tracker.trackEvent(DurationFilterBetween1and4MinutesClickedEvent)

                    DurationFilterOption.Between4And20 ->
                        Analytics.tracker.trackEvent(DurationFilterBetween4and20MinutesClickedEvent)

                    DurationFilterOption.MoreThan20 ->
                        Analytics.tracker.trackEvent(DurationFilterMoreThan20MinutesClickedEvent)
                }
                onDurationFilterItemClicked(durationOption)
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AllVideosViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AllVideosView(
            items = emptyList(),
            shouldApplySensitiveMode = false,
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
            addToPlaylistsTitles = null,
            clearAddToPlaylistsTitles = { },
            retryActionCallback = { }
        )
    }
}

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