package mega.privacy.android.feature.photos.presentation.playlists

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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.privacy.android.core.nodecomponents.list.NodeHeaderItem
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.VideoPlaylistItemView
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistBottomSheet
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistRenameMenuAction
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistsTrashMenuAction
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.VideoPlaylistDetailNavKey
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun VideoPlaylistsTabRoute(
    showVideoPlaylistRemovedDialog: Boolean,
    dismissVideoPlaylistRemovedDialog: () -> Unit,
    navigateToVideoPlaylistDetail: (VideoPlaylistDetailNavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoPlaylistsTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VideoPlaylistsTabScreen(
        uiState = uiState,
        showVideoPlaylistRemovedDialog = showVideoPlaylistRemovedDialog,
        modifier = modifier,
        onSortNodes = viewModel::setCloudSortOrder,
        onClick = viewModel::onItemClicked,
        onLongClick = viewModel::onItemLongClicked,
        onConsumedPlaylistRemovedEvent = viewModel::resetPlaylistsRemovedEvent,
        onDeleteButtonClicked = viewModel::removeVideoPlaylists,
        onRemovedDialogDismiss = dismissVideoPlaylistRemovedDialog,
        onNavigateToDetail = {
            navigateToVideoPlaylistDetail(VideoPlaylistDetailNavKey(it.id.longValue, it.type))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoPlaylistsTabScreen(
    uiState: VideoPlaylistsTabUiState,
    showVideoPlaylistRemovedDialog: Boolean,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (VideoPlaylistUiEntity) -> Unit = {},
    onLongClick: (VideoPlaylistUiEntity) -> Unit = {},
    onConsumedPlaylistRemovedEvent: () -> Unit = {},
    onDeleteButtonClicked: (Set<VideoPlaylistUiEntity>) -> Unit = {},
    onRemovedDialogDismiss: () -> Unit = {},
    snackBarQueue: SnackbarEventQueue = rememberSnackBarQueue(),
    onNavigateToDetail: (VideoPlaylistUiEntity) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val resources = LocalResources.current

    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showPlaylistBottomSheet by rememberSaveable { mutableStateOf(false) }
    val playlistBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedVideoPlaylist by remember { mutableStateOf<VideoPlaylistUiEntity?>(null) }

    var showRemovedDialog by rememberSaveable(showVideoPlaylistRemovedDialog) {
        mutableStateOf(showVideoPlaylistRemovedDialog)
    }

    when (uiState) {
        is VideoPlaylistsTabUiState.Loading -> NodesViewSkeleton(
            modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG),
            isListView = true,
            contentPadding = PaddingValues()
        )

        is VideoPlaylistsTabUiState.Data -> {
            EventEffect(
                event = uiState.playlistsRemovedEvent,
                onConsumed = onConsumedPlaylistRemovedEvent,
                action = { deletedVideoPlaylistTitles ->
                    if (deletedVideoPlaylistTitles.isNotEmpty()) {
                        val deletedMessage = if (deletedVideoPlaylistTitles.size == 1) {
                            context.getString(
                                sharedR.string.video_section_playlists_delete_playlists_message_singular,
                                deletedVideoPlaylistTitles[0]
                            )
                        } else {
                            resources.getQuantityString(
                                sharedR.plurals.video_section_playlists_delete_playlists_message,
                                deletedVideoPlaylistTitles.size,
                                deletedVideoPlaylistTitles.size
                            )
                        }
                        snackBarQueue.queueMessage(deletedMessage)
                    }
                }
            )
            if (uiState.videoPlaylistEntities.isEmpty()) {
                MegaEmptyView(
                    modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = sharedR.string.video_section_playlists_empty_hint_playlist),
                    imagePainter = painterResource(id = iconPackR.drawable.ic_playlist_glass)
                )
            } else {
                val items = uiState.videoPlaylistEntities
                FastScrollLazyColumn(
                    state = lazyListState,
                    totalItems = items.size,
                    modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG),
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
                        val videoPlaylistItem = items[it]
                        VideoPlaylistItemView(
                            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
                            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
                            title = videoPlaylistItem.title,
                            numberOfVideos = videoPlaylistItem.numberOfVideos,
                            thumbnailList = videoPlaylistItem.thumbnailList?.map { id ->
                                ThumbnailRequest(id)
                            },
                            totalDuration = videoPlaylistItem.totalDuration,
                            isSelected = videoPlaylistItem.isSelected,
                            isSystemVideoPlaylist = videoPlaylistItem.isSystemVideoPlayer,
                            onClick = {
                                if (uiState.selectedPlaylists.isEmpty()) {
                                    onNavigateToDetail(videoPlaylistItem)
                                } else {
                                    onClick(videoPlaylistItem)
                                }
                            },
                            onMenuClick = {
                                showPlaylistBottomSheet = true
                                selectedVideoPlaylist = videoPlaylistItem
                            },
                            onLongClick = { onLongClick(videoPlaylistItem) }
                        )
                    }
                }
                if (showSortBottomSheet) {
                    SortBottomSheet(
                        modifier = Modifier.testTag(VIDEO_PLAYLISTS_TAB_SORT_BOTTOM_SHEET_TEST_TAG),
                        title = stringResource(sharedR.string.action_sort_by_header),
                        options = NodeSortOption.getOptionsForSourceType(NodeSourceType.VIDEO_PLAYLISTS),
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

                if (showRemovedDialog) {
                    VideoPlaylistsRemovedDialog(
                        onDeleteButtonClicked = {
                            val playlists = uiState.selectedPlaylists.ifEmpty {
                                selectedVideoPlaylist?.let(::setOf).orEmpty()
                            }
                            onDeleteButtonClicked(playlists)
                            if (selectedVideoPlaylist != null) {
                                selectedVideoPlaylist = null
                            }
                        },
                        onRemovedDialogDismiss = {
                            onRemovedDialogDismiss()
                            showRemovedDialog = false
                        }
                    )
                }

                if (showPlaylistBottomSheet) {
                    VideoPlaylistBottomSheet(
                        actions = listOf(
                            VideoPlaylistRenameMenuAction(),
                            VideoPlaylistsTrashMenuAction()
                        ),
                        sheetState = playlistBottomSheetState,
                        onActionClicked = { action ->
                            when (action) {
                                is VideoPlaylistRenameMenuAction -> {
                                    //TODO rename functionality will be finished in another ticket
                                }

                                is VideoPlaylistsTrashMenuAction -> {
                                    showRemovedDialog = true
                                    showPlaylistBottomSheet = false
                                }
                            }
                        },
                        onDismissRequest = {
                            showPlaylistBottomSheet = false
                            selectedVideoPlaylist = null
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPlaylistsRemovedDialog(
    onDeleteButtonClicked: () -> Unit,
    onRemovedDialogDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(
            VIDEO_PLAYLISTS_TAB_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
        ),
        title = stringResource(id = sharedR.string.video_section_playlists_delete_playlist_dialog_title),
        description = null,
        positiveButtonText = stringResource(sharedR.string.video_section_playlists_delete_playlist_dialog_delete_button),
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = {
            onDeleteButtonClicked()
            onRemovedDialogDismiss()
        },
        onNegativeButtonClicked = onRemovedDialogDismiss,
        onDismiss = onRemovedDialogDismiss
    )
}

/**
 * Test tag for loading view.
 */
const val VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG = "video_playlists_tab:loading_view"

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG = "video_playlists_tab:empty_view"

/**
 * Test tag for the video playlist tab all video playlists
 */
const val VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG = "video_playlists_tab:view_all_playlists"

/**
 * Test tag for the video playlist tab sort bottom sheet
 */
const val VIDEO_PLAYLISTS_TAB_SORT_BOTTOM_SHEET_TEST_TAG = "video_playlists_tab:sort_bottom_sheet"

/**
 * Test tag for DeleteVideoPlaylistDialog
 */
const val VIDEO_PLAYLISTS_TAB_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG =
    "video_playlists_tab:dialog_delete_video_playlist"