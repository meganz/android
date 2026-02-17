package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.NavigationEventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.EditVideoPlaylistDialog
import mega.privacy.android.feature.photos.components.VideoItemView
import mega.privacy.android.feature.photos.components.VideoPlaylistDetailHeaderView
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistEditState
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailSelectionMenuAction
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistBottomSheet
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistRenameMenuAction
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistsTrashMenuAction
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.SelectVideosForPlaylistNavKey
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale

@Composable
fun VideoPlaylistDetailRoute(
    navigationHandler: NavigationHandler,
    viewModel: VideoPlaylistDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videoPlaylistEditState by viewModel.videoPlaylistEditState.collectAsStateWithLifecycle()
    val navigateEvent by viewModel.navigateToVideoPlayerEvent.collectAsStateWithLifecycle()

    NavigationEventEffect(
        event = navigateEvent,
        onConsumed = viewModel::resetNavigateToVideoPlayer
    ) {
        navigationHandler.navigate(it)
    }

    VideoPlaylistDetailScreen(
        uiState = uiState,
        videoPlaylistEditState = videoPlaylistEditState,
        showRenameVideoPlaylistDialog = viewModel::showUpdateVideoPlaylistDialog,
        updatedVideoPlaylistTitle = viewModel::updateVideoPlaylistTitle,
        resetErrorMessage = viewModel::resetEditVideoPlaylistErrorMessage,
        resetShowRenameVideoPlaylistDialog = viewModel::resetUpdateVideoPlaylistDialogEvent,
        resetUpdateTitleSuccessEvent = viewModel::resetUpdateTitleSuccessEvent,
        onDeleteButtonClicked = viewModel::removeVideoPlaylists,
        onConsumedPlaylistRemovedEvent = viewModel::resetPlaylistsRemovedEvent,
        onClick = viewModel::onItemClicked,
        onLongClick = viewModel::onItemLongClicked,
        selectAll = viewModel::selectAllVideos,
        clearSelection = viewModel::clearSelection,
        selectVideos = {
            navigationHandler.navigate(SelectVideosForPlaylistNavKey())
        },
        onBack = navigationHandler::back
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlaylistDetailScreen(
    uiState: VideoPlaylistDetailUiState,
    videoPlaylistEditState: VideoPlaylistEditState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showRenameVideoPlaylistDialog: () -> Unit = {},
    updatedVideoPlaylistTitle: (NodeId, String) -> Unit = { _, _ -> },
    resetErrorMessage: () -> Unit = {},
    resetShowRenameVideoPlaylistDialog: () -> Unit = {},
    resetUpdateTitleSuccessEvent: () -> Unit = {},
    onDeleteButtonClicked: (Set<VideoPlaylistUiEntity>) -> Unit = {},
    onConsumedPlaylistRemovedEvent: () -> Unit = {},
    onClick: (item: VideoUiEntity) -> Unit = {},
    onLongClick: (item: VideoUiEntity) -> Unit = {},
    selectAll: () -> Unit = {},
    clearSelection: () -> Unit = {},
    selectVideos: () -> Unit = {},
    multiNodeActionHandler: MultiNodeActionHandler = rememberMultiNodeActionHandler(),
    snackBarQueue: SnackbarEventQueue = rememberSnackBarQueue(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val lazyListState = rememberLazyListState()
    var showPlaylistBottomSheet by rememberSaveable { mutableStateOf(false) }
    val playlistBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRemovedDialog by rememberSaveable { mutableStateOf(false) }

    val dataState = uiState as? VideoPlaylistDetailUiState.Data
    val selectedNodes = dataState?.selectedTypedNodes ?: emptySet()
    val areAllVideosSelected = dataState?.areAllSelected ?: false
    val videoSelectedCount = dataState?.selectedCount ?: 0

    BackHandler(videoSelectedCount > 0) {
        clearSelection()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        floatingActionButton = {
            AddContentFab(
                modifier = Modifier.testTag(VIDEO_PLAYLIST_DETAIL_ADD_VIDEO_FAB_TEST_TAG),
                visible = selectedNodes.isEmpty(),
                onClick = selectVideos
            )
        },
        topBar = {
            MegaTopAppBar(
                modifier = Modifier
                    .testTag(VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG),
                navigationType = AppBarNavigationType.Back(onBack),
                title = if (videoSelectedCount > 0) {
                    String.format(Locale.ROOT, "%s", videoSelectedCount)
                } else {
                    dataState?.playlistDetail?.uiEntity?.title ?: ""
                },
                actions = buildList {
                    if (videoSelectedCount > 0) {
                        if (!areAllVideosSelected) {
                            add(NodeSelectionAction.SelectAll)
                        }
                    } else {
                        add(NodeSelectionAction.More)
                    }
                },
                onActionPressed = { action ->
                    when (action) {
                        is NodeSelectionAction.More -> showPlaylistBottomSheet = true
                        is NodeSelectionAction.SelectAll -> selectAll()
                    }
                }
            )
        },
        bottomBar = {
            SelectionModeBottomBar(
                visible = videoSelectedCount > 0,
                actions = dataState?.bottomBarActions ?: emptyList(),
                onActionPressed = { action ->
                    when (action) {
                        is VideoPlaylistDetailSelectionMenuAction.Hide -> {
                            multiNodeActionHandler(
                                HideMenuAction(),
                                selectedNodes.toList()
                            )
                            clearSelection()
                        }

                        is VideoPlaylistDetailSelectionMenuAction.Unhide -> {
                            multiNodeActionHandler(
                                UnhideMenuAction(),
                                selectedNodes.toList()
                            )
                            clearSelection()
                        }

                        is VideoPlaylistDetailSelectionMenuAction.RemoveFromPlaylist -> {
                            //TODO Will implement in another ticket
                            clearSelection()
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is VideoPlaylistDetailUiState.Loading -> NodesViewSkeleton(
                modifier = Modifier
                    .padding(innerPadding)
                    .testTag(VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG),
                isListView = true,
                contentPadding = PaddingValues()
            )

            is VideoPlaylistDetailUiState.Data -> {
                EventEffect(
                    event = videoPlaylistEditState.updateTitleSuccessEvent,
                    onConsumed = resetUpdateTitleSuccessEvent,
                    action = {
                        resetShowRenameVideoPlaylistDialog()
                    }
                )

                EventEffect(
                    event = videoPlaylistEditState.playlistsRemovedEvent,
                    onConsumed = {
                        showRemovedDialog = false
                        onConsumedPlaylistRemovedEvent()
                    },
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
                        onBack()
                    }
                )

                val playlistDetail = uiState.playlistDetail
                if (playlistDetail == null || playlistDetail.videos.isEmpty()
                ) {
                    VideoPlaylistDetailEmptyView(
                        title = playlistDetail?.uiEntity?.title,
                        totalDuration = playlistDetail?.uiEntity?.totalDuration,
                        numberOfVideos = playlistDetail?.uiEntity?.numberOfVideos,
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    val items = uiState.playlistDetail.videos
                    FastScrollLazyColumn(
                        state = lazyListState,
                        totalItems = items.size,
                        contentPadding = PaddingValues(
                            bottom = innerPadding.calculateBottomPadding() + 100.dp
                        ),
                        modifier = Modifier
                            .padding(
                                PaddingValues(
                                    top = innerPadding.calculateTopPadding(),
                                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                                )
                            )
                            .testTag(VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG)
                    ) {
                        item(key = "header") {
                            VideoPlaylistDetailHeaderView(
                                thumbnailList =
                                    playlistDetail.uiEntity.thumbnailList?.map { id ->
                                        ThumbnailRequest(id)
                                    },
                                title = playlistDetail.uiEntity.title,
                                totalDuration = playlistDetail.uiEntity.totalDuration,
                                numberOfVideos = playlistDetail.uiEntity.numberOfVideos,
                                modifier = Modifier.padding(16.dp),
                                onPlayAllClicked = {}
                            )
                        }

                        items(items = items, key = { it.id.longValue }) { videoItem ->
                            VideoItemView(
                                icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                name = videoItem.name,
                                description = videoItem.description?.replace("\n", " "),
                                fileSize = formatFileSize(videoItem.size, LocalContext.current),
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
                                onMenuClick = {},
                                onLongClick = {
                                    if (!playlistDetail.uiEntity.isSystemVideoPlayer) {
                                        onLongClick(videoItem)
                                    }
                                },
                                isSensitive = uiState.showHiddenItems &&
                                        (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited),
                            )
                        }
                    }
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
                                is VideoPlaylistRenameMenuAction -> showRenameVideoPlaylistDialog()
                                is VideoPlaylistsTrashMenuAction -> {
                                    showRemovedDialog = true
                                    showPlaylistBottomSheet = false
                                }
                            }
                        },
                        onDismissRequest = {
                            showPlaylistBottomSheet = false
                        },
                        modifier = Modifier.testTag(VIDEO_PLAYLIST_DETAIL_BOTTOM_SHEET_TEST_TAG)
                    )
                }

                if (videoPlaylistEditState.showUpdateVideoPlaylistDialog) {
                    EditVideoPlaylistDialog(
                        modifier = Modifier.testTag(
                            VIDEO_PLAYLIST_DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG
                        ),
                        handle = playlistDetail?.uiEntity?.id?.longValue ?: -1L,
                        title = stringResource(id = sharedR.string.context_rename),
                        positiveButtonText = stringResource(id = sharedR.string.context_rename),
                        onConfirm = { handle, title ->
                            updatedVideoPlaylistTitle(NodeId(handle), title)
                            showPlaylistBottomSheet = false
                        },
                        resetErrorMessage = resetErrorMessage,
                        onDismiss = {
                            resetShowRenameVideoPlaylistDialog()
                        },
                        initialInputText = playlistDetail?.uiEntity?.title ?: "",
                        errorText = videoPlaylistEditState.editVideoPlaylistErrorMessage,
                    )
                }

                if (showRemovedDialog) {
                    BasicDialog(
                        modifier = Modifier.testTag(
                            VIDEO_PLAYLIST_DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG
                        ),
                        title = stringResource(id = sharedR.string.video_section_playlists_delete_playlist_dialog_title),
                        description = null,
                        positiveButtonText = stringResource(sharedR.string.video_section_playlists_delete_playlist_dialog_delete_button),
                        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                        onPositiveButtonClicked = {
                            uiState.playlistDetail?.uiEntity?.let {
                                onDeleteButtonClicked(setOf(it))
                            }
                            showRemovedDialog = false
                        },
                        onNegativeButtonClicked = { showRemovedDialog = false },
                        onDismiss = { showRemovedDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlaylistDetailEmptyView(
    title: String?,
    totalDuration: String?,
    numberOfVideos: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG)) {
        VideoPlaylistDetailHeaderView(
            thumbnailList = null,
            title = title,
            totalDuration = totalDuration,
            numberOfVideos = numberOfVideos,
            modifier = Modifier.padding(16.dp),
            onPlayAllClicked = {}
        )

        MegaEmptyView(
            text = stringResource(id = sharedR.string.videos_tab_empty_hint_video),
            imagePainter = painterResource(id = iconPackR.drawable.ic_video_glass)
        )
    }
}

/**
 * Test tag for the video playlist detail loading view
 */
const val VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG = "video_playlist_detail:view_loading"

/**
 * Test tag for the video playlist detail videos empty view
 */
const val VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG =
    "video_playlist_detail:view_videos_empty"

/**
 * Test tag for the video playlist detail view
 */
const val VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG =
    "video_playlists_detail:view_playlist_detail"

/**
 * Test tag for the video playlist detail app bar
 */
const val VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG = "video_playlists_detail:view_app_bar"

/**
 * Test tag for RenameVideoPlaylistDialog
 */
const val VIDEO_PLAYLIST_DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG =
    "video_playlist_detail:dialog_rename_video_playlist"

/**
 * Test tag for VideoPlaylistBottomSheet
 */
const val VIDEO_PLAYLIST_DETAIL_BOTTOM_SHEET_TEST_TAG = "video_playlist_detail:bottom_sheet"

/**
 * Test tag for delete video playlist dialog
 */
const val VIDEO_PLAYLIST_DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG =
    "video_playlist_detail:dialog_delete_video_playlist"

/**
 * Test tag for adding video to playlist FAB
 */
const val VIDEO_PLAYLIST_DETAIL_ADD_VIDEO_FAB_TEST_TAG = "video_playlist_detail:add_video_fab"
