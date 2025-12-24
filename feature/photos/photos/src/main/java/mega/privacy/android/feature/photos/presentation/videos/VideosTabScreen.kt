package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.NavigationEventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.list.NodeHeaderItem
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.TagsRow
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.VideoItemView
import mega.privacy.android.feature.photos.components.VideosFilterButtonView
import mega.privacy.android.feature.photos.presentation.videos.model.DurationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.FilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.feature.photos.presentation.videos.view.VideosFilterBottomSheet
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
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

@Composable
fun VideosTabRoute(
    navigationHandler: NavigationHandler,
    viewModel: VideosTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateEvent by viewModel.navigateToVideoPlayerEvent.collectAsStateWithLifecycle()

    NavigationEventEffect(
        event = navigateEvent,
        onConsumed = viewModel::resetNavigateToVideoPlayer
    ) {
        navigationHandler.navigate(it)
    }

    VideosTabScreen(
        uiState = uiState,
        onClick = viewModel::onItemClicked,
        onLongClick = viewModel::onItemLongClicked,
        locationOptionSelected = viewModel::setLocationSelectedFilterOption,
        durationOptionSelected = viewModel::setDurationSelectedFilterOption,
        onSortNodes = viewModel::setCloudSortOrder,
        navigationHandler = navigationHandler
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideosTabScreen(
    uiState: VideosTabUiState,
    onClick: (item: VideoUiEntity) -> Unit,
    onLongClick: (item: VideoUiEntity) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    navigationHandler: NavigationHandler,
    modifier: Modifier = Modifier,
    locationOptionSelected: (LocationFilterOption) -> Unit = {},
    durationOptionSelected: (DurationFilterOption) -> Unit = {},
) {
    val lazyListState = rememberLazyListState()
    val durationInSecondsTextMapper = remember { DurationInSecondsTextMapper() }
    val coroutineScope = rememberCoroutineScope()

    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showLocationBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showDurationBottomSheet by rememberSaveable { mutableStateOf(false) }
    val locationModalSheetState = rememberModalBottomSheetState(true)
    val durationModalSheetState = rememberModalBottomSheetState(true)

    var selectedLocationFilterOption by remember { mutableStateOf(LocationFilterOption.AllLocations) }
    var selectedDurationFilterOption by remember { mutableStateOf(DurationFilterOption.AllDurations) }
    var isAllLocations by rememberSaveable { mutableStateOf(true) }
    var isAllDurations by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier) {
        VideosFilterButtonView(
            isLocationFilterSelected = isAllLocations.not(),
            isDurationFilterSelected = isAllDurations.not(),
            modifier = Modifier.testTag(VIDEO_TAB_VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG),
            onDurationFilterClicked = {
                coroutineScope.launch {
                    showDurationBottomSheet = true
                    durationModalSheetState.show()
                }
            },
            onLocationFilterClicked = {
                coroutineScope.launch {
                    showLocationBottomSheet = true
                    locationModalSheetState.show()
                }
            },
            locationText = stringResource(id = selectedLocationFilterOption.titleResId),
            durationText = stringResource(id = selectedDurationFilterOption.titleResId),
        )

        when (uiState) {
            is VideosTabUiState.Loading -> NodesViewSkeleton(
                modifier = Modifier.testTag(VIDEO_TAB_LOADING_VIEW_TEST_TAG),
                isListView = true,
                contentPadding = PaddingValues()
            )

            is VideosTabUiState.Data -> {
                if (uiState.allVideoEntities.isEmpty()) {
                    MegaEmptyView(
                        modifier = Modifier.testTag(VIDEO_TAB_EMPTY_VIEW_TEST_TAG),
                        text = stringResource(id = sharedR.string.videos_tab_empty_hint_video),
                        imagePainter = painterResource(id = iconPackR.drawable.ic_video_glass)
                    )
                } else {
                    val items = uiState.allVideoEntities
                    FastScrollLazyColumn(
                        state = lazyListState,
                        totalItems = items.size,
                        modifier = Modifier.testTag(VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG)
                    ) {
                        item(key = "header") {
                            NodeHeaderItem(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .padding(bottom = 8.dp),
                                onSortOrderClick = { showSortBottomSheet = true },
                                onChangeViewTypeClick = {},
                                onEnterMediaDiscoveryClick = {},
                                sortConfiguration = uiState.selectedSortConfiguration,
                                isListView = true,
                                showSortOrder = true,
                                showChangeViewType = false,
                            )
                        }

                        items(count = items.size, key = { items[it].id.longValue }) {
                            val videoItem = items[it]
                            VideoItemView(
                                icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                name = videoItem.name,
                                description = videoItem.description?.replace("\n", " "),
                                fileSize = formatFileSize(videoItem.size, LocalContext.current),
                                duration = durationInSecondsTextMapper(videoItem.duration),
                                isFavourite = videoItem.isFavourite,
                                isSelected = videoItem.isSelected,
                                isSharedWithPublicLink = videoItem.isSharedItems,
                                labelView = {
                                    videoItem.nodeLabel?.let { label ->
                                        NodeLabelCircle(
                                            modifier = Modifier.padding(start = 10.dp),
                                            label = label
                                        )
                                    }
                                },
                                tagsRow = {
                                    if (!videoItem.tags.isNullOrEmpty()) {
                                        TagsRow(
                                            tags = videoItem.tags,
                                            highlightText = uiState.highlightText,
                                            addSpacing = true,
                                        )
                                    }
                                },
                                thumbnailData = ThumbnailRequest(videoItem.id),
                                nodeAvailableOffline = videoItem.nodeAvailableOffline,
                                highlightText = uiState.highlightText,
                                onClick = { onClick(videoItem) },
                                onMenuClick = {
                                    navigationHandler.navigate(
                                        NodeOptionsBottomSheetNavKey(
                                            nodeHandle = videoItem.id.longValue,
                                            nodeSourceType = NodeSourceType.VIDEOS
                                        )
                                    )
                                },
                                onLongClick = { onLongClick(videoItem) },
                                isSensitive = uiState.showHiddenItems &&
                                        (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited),
                            )
                        }
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

        if (showLocationBottomSheet) {
            VideosFilterBottomSheet(
                modifier = Modifier.testTag(
                    VIDEO_TAB_VIDEOS_LOCATION_FILTER_BOTTOM_SHEET_TEST_TAG
                ),
                sheetState = locationModalSheetState,
                title = stringResource(id = sharedR.string.video_section_videos_location_filter_title),
                selectedFilterOption = selectedLocationFilterOption,
                options = LocationFilterOption.entries,
                onDismissRequest = {
                    coroutineScope.launch {
                        showLocationBottomSheet = false
                        locationModalSheetState.hide()
                    }
                },
                onItemSelected = { option ->
                    coroutineScope.launch {
                        showLocationBottomSheet = false
                        locationModalSheetState.hide()
                    }
                    val locationOption = convertToLocationFilterOption(option)
                    selectedLocationFilterOption = locationOption
                    isAllLocations = locationOption == LocationFilterOption.AllLocations
                    locationOptionSelected(locationOption)
                }
            )
        }

        if (showDurationBottomSheet) {
            VideosFilterBottomSheet(
                modifier = Modifier.testTag(VIDEO_TAB_VIDEOS_DURATION_FILTER_BOTTOM_SHEET_TEST_TAG),
                sheetState = durationModalSheetState,
                selectedFilterOption = selectedDurationFilterOption,
                title = stringResource(id = sharedR.string.video_section_videos_duration_filter_title),
                options = DurationFilterOption.entries,
                onDismissRequest = {
                    coroutineScope.launch {
                        showDurationBottomSheet = false
                        durationModalSheetState.hide()
                    }
                },
                onItemSelected = { option ->
                    coroutineScope.launch {
                        showDurationBottomSheet = false
                        durationModalSheetState.hide()
                    }
                    val durationOption = convertToDurationFilterOption(option)
                    selectedDurationFilterOption = durationOption
                    isAllDurations = durationOption == DurationFilterOption.AllDurations
                    durationOptionSelected(durationOption)
                }
            )
        }
    }
}

private fun convertToLocationFilterOption(option: FilterOption) =
    when (option) {
        LocationFilterOption.AllLocations -> {
            Analytics.tracker.trackEvent(LocationFilterAllLocationsClickedEvent)
            LocationFilterOption.AllLocations
        }

        LocationFilterOption.CloudDrive -> {
            Analytics.tracker.trackEvent(LocationFilterCloudDriveClickedEvent)
            LocationFilterOption.CloudDrive
        }


        LocationFilterOption.CameraUploads -> {
            Analytics.tracker.trackEvent(LocationFilterCameraUploadClickedEvent)
            LocationFilterOption.CameraUploads
        }

        LocationFilterOption.SharedItems -> {
            Analytics.tracker.trackEvent(LocationFilterSharedItemClickedEvent)
            LocationFilterOption.SharedItems
        }

        else -> {
            Analytics.tracker.trackEvent(LocationFilterAllLocationsClickedEvent)
            LocationFilterOption.AllLocations
        }
    }

private fun convertToDurationFilterOption(option: FilterOption) =
    when (option) {
        DurationFilterOption.AllDurations -> {
            Analytics.tracker.trackEvent(DurationFilterAllDurationsClickedEvent)
            DurationFilterOption.AllDurations
        }

        DurationFilterOption.LessThan10Seconds -> {
            Analytics.tracker.trackEvent(DurationFilterLessThan10SecondsClickedEvent)
            DurationFilterOption.LessThan10Seconds
        }

        DurationFilterOption.Between10And60Seconds -> {
            Analytics.tracker.trackEvent(DurationFilterBetween10and60SecondsClickedEvent)
            DurationFilterOption.Between10And60Seconds
        }

        DurationFilterOption.Between1And4 -> {
            Analytics.tracker.trackEvent(DurationFilterBetween1and4MinutesClickedEvent)
            DurationFilterOption.Between1And4
        }

        DurationFilterOption.Between4And20 -> {
            Analytics.tracker.trackEvent(DurationFilterBetween4and20MinutesClickedEvent)
            DurationFilterOption.Between4And20
        }

        DurationFilterOption.MoreThan20 -> {
            Analytics.tracker.trackEvent(DurationFilterMoreThan20MinutesClickedEvent)
            DurationFilterOption.MoreThan20
        }

        else -> {
            Analytics.tracker.trackEvent(DurationFilterAllDurationsClickedEvent)
            DurationFilterOption.AllDurations
        }
    }

/**
 * Test tag for the video tab loading view
 */
const val VIDEO_TAB_LOADING_VIEW_TEST_TAG = "video_tab:view_loading"

/**
 * Test tag for the video tab empty view
 */
const val VIDEO_TAB_EMPTY_VIEW_TEST_TAG = "video_tab:view_empty"

/**
 * Test tag for the video tab all videos view
 */
const val VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG = "video_tab:view_all_videos"

/**
 * Test tag for the videos filter button view.
 */
const val VIDEO_TAB_VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG = "video_tab:button_filter"

/**
 * Test tag for the video tab location filter bottom sheet.
 */
const val VIDEO_TAB_VIDEOS_LOCATION_FILTER_BOTTOM_SHEET_TEST_TAG =
    "video_tab:bottom_sheet_location_filter"

/**
 * Test tag for the video tab duration filter bottom sheet.
 */
const val VIDEO_TAB_VIDEOS_DURATION_FILTER_BOTTOM_SHEET_TEST_TAG =
    "video_tab:bottom_sheet_duration_filter"

/**
 * Test tag for the video tab sort bottom sheet
 */
const val VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG = "video_tab:sort_bottom_sheet"