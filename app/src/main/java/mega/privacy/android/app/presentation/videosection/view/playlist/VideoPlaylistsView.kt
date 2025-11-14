package mega.privacy.android.app.presentation.videosection.view.playlist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionLoadingView
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewWithImage
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.FastScrollLazyColumn
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.VideoPlaylistCreationButtonPressedEvent

@Composable
internal fun VideoPlaylistsView(
    items: List<VideoPlaylistUIEntity>,
    progressBarShowing: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    isInputTitleValid: Boolean,
    showDeleteVideoPlaylistDialog: Boolean,
    showRenameVideoPlaylistDialog: Boolean,
    showCreateVideoPlaylistDialog: Boolean,
    inputPlaceHolderText: String,
    onMenuClick: () -> Unit,
    modifier: Modifier,
    updateShowDeleteVideoPlaylist: (Boolean) -> Unit,
    onCreateDialogPositiveButtonClicked: (String) -> Unit,
    onRenameDialogPositiveButtonClicked: (playlistID: NodeId, newTitle: String) -> Unit,
    onDeleteDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit,
    onDeleteDialogNegativeButtonClicked: () -> Unit,
    onDeletePlaylistsDialogPositiveButtonClicked: () -> Unit,
    setInputValidity: (Boolean) -> Unit,
    onClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit,
    onSortOrderClick: () -> Unit,
    onDeletedMessageShown: () -> Unit,
    deletedVideoPlaylistTitles: List<String> = emptyList(),
    errorMessage: Int? = null,
    onLongClick: ((item: VideoPlaylistUIEntity, index: Int) -> Unit) = { _, _ -> },
    updateShowRenameVideoPlaylist: (Boolean) -> Unit,
    updateShowCreateVideoPlaylist: (Boolean) -> Unit,
) {
    LaunchedEffect(items) {
        if (scrollToTop) {
            lazyListState.scrollToItem(0)
        }
        if (showRenameVideoPlaylistDialog) {
            updateShowRenameVideoPlaylist(false)
        }
        if (showCreateVideoPlaylistDialog) {
            updateShowCreateVideoPlaylist(false)
        }
    }

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var clickedItem: Int by rememberSaveable { mutableIntStateOf(-1) }
    val resources = LocalResources.current

    LaunchedEffect(deletedVideoPlaylistTitles) {
        if (deletedVideoPlaylistTitles.isNotEmpty()) {
            val deletedMessage = if (deletedVideoPlaylistTitles.size == 1) {
                resources.getString(
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
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(deletedMessage)
            }
            onDeletedMessageShown()
        }
    }

    MegaScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState
    ) { paddingValue ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue)
        ) {
            if (showCreateVideoPlaylistDialog) {
                CreateVideoPlaylistDialog(
                    modifier = Modifier.testTag(CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                    title = stringResource(id = sharedR.string.video_section_playlists_create_playlist_dialog_title),
                    positiveButtonText = stringResource(id = R.string.general_create),
                    inputPlaceHolderText = { inputPlaceHolderText },
                    errorMessage = errorMessage,
                    onDialogInputChange = setInputValidity,
                    onDismissRequest = {
                        updateShowCreateVideoPlaylist(false)
                        setInputValidity(true)
                    },
                    onDialogPositiveButtonClicked = { titleOfNewVideoPlaylist ->
                        Analytics.tracker.trackEvent(VideoPlaylistCreationButtonPressedEvent)
                        onCreateDialogPositiveButtonClicked(titleOfNewVideoPlaylist)
                    },
                ) {
                    isInputTitleValid
                }
            }

            if (showRenameVideoPlaylistDialog) {
                CreateVideoPlaylistDialog(
                    modifier = Modifier.testTag(RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                    title = stringResource(id = sharedR.string.video_section_playlists_rename_playlist_dialog_title),
                    positiveButtonText = stringResource(id = sharedR.string.video_section_playlists_rename_playlist_dialog_title),
                    inputPlaceHolderText = { inputPlaceHolderText },
                    errorMessage = errorMessage,
                    onDialogInputChange = setInputValidity,
                    onDismissRequest = {
                        updateShowRenameVideoPlaylist(false)
                        setInputValidity(true)
                    },
                    initialInputText = {
                        if (clickedItem != -1) {
                            items[clickedItem].title
                        } else {
                            ""
                        }
                    },
                    onDialogPositiveButtonClicked = { newTitle ->
                        if (clickedItem != -1) {
                            onRenameDialogPositiveButtonClicked(items[clickedItem].id, newTitle)
                        }
                    },
                ) {
                    isInputTitleValid
                }
            }

            if (showDeleteVideoPlaylistDialog) {
                DeleteItemsDialog(
                    modifier = Modifier.testTag(DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                    title = stringResource(id = sharedR.string.video_section_playlists_delete_playlist_dialog_title),
                    text = null,
                    confirmButtonText = stringResource(id = sharedR.string.video_section_playlists_delete_playlist_dialog_delete_button),
                    onDeleteButtonClicked = {
                        if (clickedItem != -1) {
                            onDeleteDialogPositiveButtonClicked(items[clickedItem])
                        } else {
                            onDeletePlaylistsDialogPositiveButtonClicked()
                        }
                        clickedItem = -1
                    },
                    onDismiss = {
                        updateShowDeleteVideoPlaylist(false)
                        onDeleteDialogNegativeButtonClicked()
                        clickedItem = -1
                    }
                )
            }

            when {
                progressBarShowing -> VideoSectionLoadingView()

                items.isEmpty() -> LegacyMegaEmptyViewWithImage(
                    modifier = Modifier.testTag(VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = sharedR.string.video_section_playlists_empty_hint_playlist),
                    imagePainter = painterResource(id = iconPackR.drawable.ic_playlist_glass)
                )

                else -> {
                    FastScrollLazyColumn(
                        state = lazyListState,
                        totalItems = items.size,
                        modifier = modifier.semantics { testTagsAsResourceId = true },
                    ) {
                        item(key = "header") {
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
                                onClick = { onClick(videoPlaylistItem, it) },
                                onMenuClick = {
                                    clickedItem = it
                                    onMenuClick()
                                },
                                onLongClick = { onLongClick(videoPlaylistItem, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeleteItemsDialog(
    title: String,
    text: String?,
    confirmButtonText: String,
    onDeleteButtonClicked: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAlertDialog(
        modifier = modifier,
        title = title,
        text = text ?: "",
        confirmButtonText = confirmButtonText,
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onConfirm = onDeleteButtonClicked,
        onDismiss = onDismiss
    )
}

@CombinedThemePreviews
@Composable
private fun DeleteVideoPlaylistDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeleteItemsDialog(
            title = "Delete playlist?",
            text = "Do we need additional explanation to delete playlists?",
            confirmButtonText = "Delete",
            onDeleteButtonClicked = {},
            onDismiss = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeleteVideosDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeleteItemsDialog(
            title = "Remove from playlist?",
            text = null,
            confirmButtonText = "Remove",
            onDeleteButtonClicked = {},
            onDismiss = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(
            items = emptyList(),
            progressBarShowing = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            isInputTitleValid = true,
            showDeleteVideoPlaylistDialog = false,
            showRenameVideoPlaylistDialog = false,
            showCreateVideoPlaylistDialog = false,
            modifier = Modifier.fillMaxSize(),
            onClick = { _, _ -> },
            onSortOrderClick = {},
            inputPlaceHolderText = "New playlist",
            updateShowDeleteVideoPlaylist = {},
            onCreateDialogPositiveButtonClicked = {},
            onRenameDialogPositiveButtonClicked = { _, _ -> },
            onDeleteDialogPositiveButtonClicked = {},
            onDeletedMessageShown = {},
            setInputValidity = {},
            onDeletePlaylistsDialogPositiveButtonClicked = {},
            onDeleteDialogNegativeButtonClicked = {},
            updateShowRenameVideoPlaylist = {},
            onMenuClick = {},
            updateShowCreateVideoPlaylist = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewCreateDialogShownPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(
            items = emptyList(),
            progressBarShowing = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            isInputTitleValid = true,
            showDeleteVideoPlaylistDialog = true,
            showRenameVideoPlaylistDialog = false,
            showCreateVideoPlaylistDialog = true,
            modifier = Modifier.fillMaxSize(),
            onClick = { _, _ -> },
            onSortOrderClick = {},
            inputPlaceHolderText = "New playlist",
            updateShowDeleteVideoPlaylist = {},
            onCreateDialogPositiveButtonClicked = {},
            onRenameDialogPositiveButtonClicked = { _, _ -> },
            onDeleteDialogPositiveButtonClicked = {},
            setInputValidity = {},
            onDeletedMessageShown = {},
            onDeletePlaylistsDialogPositiveButtonClicked = {},
            onDeleteDialogNegativeButtonClicked = {},
            updateShowRenameVideoPlaylist = {},
            onMenuClick = {},
            updateShowCreateVideoPlaylist = {}
        )
    }
}

/**
 * Test tag for CreateVideoPlaylistDialog
 */
const val CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG = "video_playlists:dialog_create_video_playlist"

/**
 * Test tag for RenameVideoPlaylistDialog
 */
const val RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG = "video_playlists:dialog_rename_video_playlist"

/**
 * Test tag for DeleteVideoPlaylistDialog
 */
const val DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG = "video_playlists:dialog_delete_video_playlist"

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG = "video_playlists:empty_view"