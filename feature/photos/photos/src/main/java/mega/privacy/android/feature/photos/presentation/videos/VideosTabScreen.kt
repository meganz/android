package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.TagsRow
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
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
    )
}

@Composable
internal fun VideosTabScreen(
    uiState: VideosTabUiState,
    onClick: (item: VideoUiEntity, index: Int) -> Unit,
    onMenuClick: (VideoUiEntity) -> Unit,
    onLongClick: (item: VideoUiEntity, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightText: String = "",
) {
    val lazyListState = rememberLazyListState()
    val durationInSecondsTextMapper = remember { DurationInSecondsTextMapper() }
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
                                    highlightText = highlightText,
                                    addSpacing = true,
                                )
                            }
                        },
                        thumbnailData = ThumbnailRequest(videoItem.id),
                        nodeAvailableOffline = videoItem.nodeAvailableOffline,
                        highlightText = highlightText,
                        onClick = { onClick(videoItem, it) },
                        onMenuClick = { onMenuClick(videoItem) },
                        onLongClick = { onLongClick(videoItem, it) },
                        isSensitive = false,
                    )
                }
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