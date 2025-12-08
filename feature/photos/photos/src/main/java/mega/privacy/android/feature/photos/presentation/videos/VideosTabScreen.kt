package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.list.NodeHeaderItem
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.TagsRow
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.VideoItemView
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun VideosTabRoute(
    viewModel: VideosTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VideosTabScreen(
        uiState = uiState,
        onClick = { _, _ -> },
        onMenuClick = {},
        onLongClick = { _, _ -> },
        onSortNodes = viewModel::setCloudSortOrder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideosTabScreen(
    uiState: VideosTabUiState,
    onClick: (item: VideoUiEntity, index: Int) -> Unit,
    onMenuClick: (VideoUiEntity) -> Unit,
    onLongClick: (item: VideoUiEntity, index: Int) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val durationInSecondsTextMapper = remember { DurationInSecondsTextMapper() }

    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (uiState) {
        is VideosTabUiState.Loading -> NodesViewSkeleton(
            modifier = modifier.testTag(VIDEO_TAB_LOADING_VIEW_TEST_TAG),
            isListView = true,
            contentPadding = PaddingValues()
        )

        is VideosTabUiState.Data -> if (uiState.allVideos.isEmpty()) {
            MegaEmptyView(
                modifier = modifier.testTag(VIDEO_TAB_EMPTY_VIEW_TEST_TAG),
                text = stringResource(id = sharedR.string.videos_tab_empty_hint_video),
                imagePainter = painterResource(id = iconPackR.drawable.ic_video_glass)
            )
        } else {
            val items = uiState.allVideos
            FastScrollLazyColumn(
                state = lazyListState,
                totalItems = items.size,
                modifier = modifier.testTag(VIDEO_TAB_ALL_VIDEOS_VIEW_TEST_TAG)
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
                        onClick = { onClick(videoItem, it) },
                        onMenuClick = { onMenuClick(videoItem) },
                        onLongClick = { onLongClick(videoItem, it) },
                        isSensitive = false,
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
 * Test tag for the video tab sort bottom sheet
 */
const val VIDEO_TAB_SORT_BOTTOM_SHEET_TEST_TAG = "video_tab:sort_bottom_sheet"