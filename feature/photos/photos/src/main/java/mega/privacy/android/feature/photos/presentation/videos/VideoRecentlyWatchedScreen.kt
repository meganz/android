package mega.privacy.android.feature.photos.presentation.videos

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.NavigationEventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.empty.MegaEmptyView
import mega.android.core.ui.components.surface.ThemedSurface
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.VideoItemView
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.feature.photos.presentation.videos.view.VideoRecentlyWatchedClearMenuAction
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.resources.R as sharedR
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun VideoRecentlyWatchedRoute(
    onBack: () -> Unit,
    navigate: (NavKey) -> Unit,
    viewModel: VideoRecentlyWatchedViewModel = hiltViewModel(),
    snackBarQueue: SnackbarEventQueue = rememberSnackBarQueue(),
) {
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clearVideosRecentlyWatched by viewModel.clearRecentlyWatchedEvent.collectAsStateWithLifecycle()
    val navigateEvent by viewModel.navigateToVideoPlayerEvent.collectAsStateWithLifecycle()
    val nodeSourceTypeToViewTypeMapper = remember { NodeSourceTypeToViewTypeMapper() }

    EventEffect(
        event = clearVideosRecentlyWatched,
        onConsumed = viewModel::resetVideosRecentlyWatched,
        action = {
            snackBarQueue.queueMessage(
                resources.getString(sharedR.string.video_section_message_clear_recently_watched)
            )
        }
    )

    NavigationEventEffect(
        event = navigateEvent,
        onConsumed = viewModel::resetNavigateToVideoPlayer,
        action = { (item, uri) ->
            val dataState = uiState as? VideoRecentlyWatchedUiState.Data
            val items =
                dataState?.groupedVideoRecentlyWatchedItems?.values?.flatten() ?: emptyList()
            val navKey = LegacyMediaPlayerNavKey(
                nodeHandle = item.id.longValue,
                nodeContentUri = uri,
                nodeSourceType = nodeSourceTypeToViewTypeMapper(NodeSourceType.SEARCH),
                isFolderLink = false,
                fileName = item.name,
                parentHandle = item.parentId.longValue,
                fileHandle = item.id.longValue,
                fileTypeInfo = item.fileTypeInfo,
                searchedItems = items.map { it.id.longValue }
            )
            navigate(navKey)
        }
    )

    VideoRecentlyWatchedScreen(
        uiState = uiState,
        onBack = onBack,
        onClear = viewModel::clearVideosRecentlyWatched,
        onMenuClick = navigate,
        onClick = viewModel::onItemClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoRecentlyWatchedScreen(
    uiState: VideoRecentlyWatchedUiState,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onMenuClick: (NavKey) -> Unit,
    onClick: (VideoUiEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearMenuAction = remember { VideoRecentlyWatchedClearMenuAction() }
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            val isNotEmpty = (uiState as? VideoRecentlyWatchedUiState.Data)
                ?.groupedVideoRecentlyWatchedItems?.isNotEmpty() == true

            MegaTopAppBar(
                modifier = Modifier
                    .testTag(RECENTLY_WATCHED_SEARCH_TOP_APP_BAR_TAG),
                navigationType = AppBarNavigationType.Back(onBack),
                title = stringResource(id = sharedR.string.video_section_title_video_recently_watched),
                actions = if (isNotEmpty) {
                    listOf(clearMenuAction)
                } else {
                    emptyList()
                },
                onActionPressed = { action ->
                    if (action is VideoRecentlyWatchedClearMenuAction) {
                        onClear()
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (uiState) {
                is VideoRecentlyWatchedUiState.Loading -> {
                    NodesViewSkeleton(
                        modifier = Modifier.testTag(RECENTLY_WATCHED_LOADING_VIEW_TEST_TAG),
                        isListView = true,
                        contentPadding = PaddingValues()
                    )
                }

                is VideoRecentlyWatchedUiState.Data -> {
                    val group = uiState.groupedVideoRecentlyWatchedItems
                    if (group.isEmpty()) {
                        MegaEmptyView(
                            modifier = Modifier.testTag(RECENTLY_WATCHED_EMPTY_VIEW_TEST_TAG),
                            text = stringResource(id = sharedR.string.video_section_empty_hint_no_recently_activity),
                            imagePainter = painterResource(id = iconPackR.drawable.ic_clock_glass)
                        )
                    } else {
                        LazyColumn(
                            state = rememberLazyListState(),
                        ) {
                            group.forEach { (date, items) ->
                                stickyHeader {
                                    ThemedSurface {
                                        MegaText(
                                            text = formatRecentlyWatchedDate(date),
                                            textColor = TextColor.Secondary,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .testTag(RECENTLY_WATCHED_HEADER_TAG)
                                        )
                                    }
                                }

                                items(count = items.size, key = { items[it].id.longValue }) {
                                    val videoItem = items[it]
                                    VideoItemView(
                                        icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                        name = videoItem.name,
                                        description = videoItem.description?.replace("\n", " "),
                                        fileSize = formatFileSize(
                                            videoItem.size, LocalContext.current
                                        ),
                                        duration = videoItem.durationString,
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
                                        thumbnailData = ThumbnailRequest(videoItem.id),
                                        nodeAvailableOffline = videoItem.nodeAvailableOffline,
                                        onClick = { onClick(videoItem) },
                                        onMenuClick = {
                                            onMenuClick(
                                                NodeOptionsBottomSheetNavKey(
                                                    nodeHandle = videoItem.id.longValue,
                                                    nodeSourceType = NodeSourceType.VIDEO_RECENTLY_WATCHED
                                                )
                                            )
                                        },
                                        isSensitive = uiState.showHiddenItems &&
                                                (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun formatRecentlyWatchedDate(
    epochDay: Long,
    dateFormatPattern: String = "EEEE, d MMM yyyy"
): String {
    val locale = Locale.current.platformLocale
    val zoneId = remember { ZoneId.systemDefault() }

    val (targetDate, todayDate, yesterdayDate) = remember(epochDay, zoneId) {
        val today = LocalDate.now(zoneId)
        Triple(LocalDate.ofEpochDay(epochDay), today, today.minusDays(1))
    }

    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(
                locale,
                dateFormatPattern
            )
        ).withLocale(locale)
    }

    return when (targetDate) {
        todayDate -> {
            stringResource(sharedR.string.search_dropdown_chip_filter_type_date_today)
        }

        yesterdayDate -> {
            stringResource(sharedR.string.label_yesterday)
        }

        else -> {
            targetDate.format(dateTimeFormatter)
        }
    }
}


/**
 * Test tag for the loading view of recently watched screen
 */
internal const val RECENTLY_WATCHED_LOADING_VIEW_TEST_TAG = "recently_watched:view_loading"

/**
 * Test tag for the empty view of recently watched screen
 */
internal const val RECENTLY_WATCHED_EMPTY_VIEW_TEST_TAG = "recently_watched:view_empty"

/**
 * The test tag for top app bar of recently watched screen
 */
internal const val RECENTLY_WATCHED_SEARCH_TOP_APP_BAR_TAG = "recently_watched:top_bar"

/**
 * The test tag for header of recently watched screen
 */
internal const val RECENTLY_WATCHED_HEADER_TAG = "recently_watched:header"
