package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_black
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun VideoPlaylistsView(
    items: List<VideoPlaylistUIEntity>,
    progressBarShowing: Boolean,
    searchMode: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    isInputTitleValid: Boolean,
    showDeleteVideoPlaylistDialog: Boolean,
    inputPlaceHolderText: String,
    modifier: Modifier,
    updateShowDeleteVideoPlaylist: (Boolean) -> Unit,
    setDialogInputPlaceholder: (String) -> Unit,
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
) {
    var showCreateVideoPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameVideoPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(items) {
        if (scrollToTop) {
            lazyListState.scrollToItem(0)
        }
        if (showRenameVideoPlaylistDialog) {
            showRenameVideoPlaylistDialog = false
        }
        if (showCreateVideoPlaylistDialog) {
            showCreateVideoPlaylistDialog = false
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    val isLight = MaterialTheme.colors.isLight

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )
    var clickedItem: Int by rememberSaveable { mutableIntStateOf(-1) }

    val context = LocalContext.current

    LaunchedEffect(deletedVideoPlaylistTitles) {
        if (deletedVideoPlaylistTitles.isNotEmpty()) {
            val deletedMessage = if (deletedVideoPlaylistTitles.size == 1) {
                context.resources.getString(
                    sharedR.string.video_section_playlists_delete_playlists_message_singular,
                    deletedVideoPlaylistTitles[0]
                )
            } else {
                context.resources.getQuantityString(
                    sharedR.plurals.video_section_playlists_delete_playlists_message,
                    deletedVideoPlaylistTitles.size,
                    deletedVideoPlaylistTitles.size
                )
            }
            coroutineScope.launch {
                snackBarHostState.showAutoDurationSnackbar(deletedMessage)
            }
            onDeletedMessageShown()
        }
    }

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch {
            modalSheetState.hide()
        }
    }

    Scaffold(
        modifier = modifier,
        scaffoldState = rememberScaffoldState(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        backgroundColor = black.takeIf { isLight } ?: white,
                    )
                }
            )
        },
        floatingActionButton = {
            val placeholderText = "New playlist"
            val scrollNotInProgress by remember {
                derivedStateOf { !lazyListState.isScrollInProgress }
            }
            CreateVideoPlaylistFabButton(
                showFabButton = scrollNotInProgress,
                onCreateVideoPlaylistClick = {
                    showCreateVideoPlaylistDialog = true
                    setDialogInputPlaceholder(placeholderText)
                }
            )
        }
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
                        showCreateVideoPlaylistDialog = false
                        setInputValidity(true)
                    },
                    onDialogPositiveButtonClicked = { titleOfNewVideoPlaylist ->
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
                        showRenameVideoPlaylistDialog = false
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
                progressBarShowing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp)
                            .testTag(PROGRESS_BAR_TEST_TAG),
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
                    modifier = Modifier.testTag(VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = sharedR.string.video_section_playlists_empty_hint_playlist),
                    imagePainter = painterResource(id = R.drawable.ic_homepage_empty_playlists)
                )

                else -> {
                    LazyColumn(state = lazyListState, modifier = modifier) {
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
                            val videoPlaylistItem = items[it]
                            VideoPlaylistItemView(
                                icon = R.drawable.ic_playlist_item_empty,
                                title = videoPlaylistItem.title,
                                numberOfVideos = videoPlaylistItem.numberOfVideos,
                                thumbnailList = videoPlaylistItem.thumbnailList?.map { id ->
                                    ThumbnailRequest(id)
                                },
                                totalDuration = videoPlaylistItem.totalDuration,
                                isSelected = videoPlaylistItem.isSelected,
                                onClick = { onClick(videoPlaylistItem, it) },
                                onMenuClick = {
                                    clickedItem = it
                                    coroutineScope.launch { modalSheetState.show() }
                                },
                                onLongClick = { onLongClick(videoPlaylistItem, it) }
                            )
                        }
                    }
                }
            }
        }
        VideoPlaylistBottomSheet(
            modalSheetState = modalSheetState,
            coroutineScope = coroutineScope,
            onRenameVideoPlaylistClicked = {
                showRenameVideoPlaylistDialog = true
            },
            onDeleteVideoPlaylistClicked = {
                updateShowDeleteVideoPlaylist(true)
            }
        )
    }
}

@Composable
internal fun CreateVideoPlaylistFabButton(
    onCreateVideoPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFabButton: Boolean = true,
) {
    AnimatedVisibility(
        visible = showFabButton,
        enter = scaleIn(),
        exit = scaleOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            modifier = modifier.testTag(FAB_BUTTON_TEST_TAG),
            onClick = onCreateVideoPlaylistClick
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create new video playlist",
                tint = MaterialTheme.colors.white_black
            )
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
        cancelButtonText = stringResource(id = R.string.button_cancel),
        onConfirm = onDeleteButtonClicked,
        onDismiss = onDismiss
    )
}

@CombinedThemePreviews
@Composable
private fun DeleteVideoPlaylistDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(
            items = emptyList(),
            progressBarShowing = false,
            searchMode = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            isInputTitleValid = true,
            showDeleteVideoPlaylistDialog = false,
            modifier = Modifier.fillMaxSize(),
            onClick = { _, _ -> },
            onSortOrderClick = {},
            inputPlaceHolderText = "New playlist",
            setDialogInputPlaceholder = {},
            updateShowDeleteVideoPlaylist = {},
            onCreateDialogPositiveButtonClicked = {},
            onRenameDialogPositiveButtonClicked = { _, _ -> },
            onDeleteDialogPositiveButtonClicked = {},
            onDeletedMessageShown = {},
            setInputValidity = {},
            onDeletePlaylistsDialogPositiveButtonClicked = {},
            onDeleteDialogNegativeButtonClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewCreateDialogShownPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(
            items = emptyList(),
            progressBarShowing = false,
            searchMode = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            isInputTitleValid = true,
            showDeleteVideoPlaylistDialog = true,
            modifier = Modifier.fillMaxSize(),
            onClick = { _, _ -> },
            onSortOrderClick = {},
            inputPlaceHolderText = "New playlist",
            setDialogInputPlaceholder = {},
            updateShowDeleteVideoPlaylist = {},
            onCreateDialogPositiveButtonClicked = {},
            onRenameDialogPositiveButtonClicked = { _, _ -> },
            onDeleteDialogPositiveButtonClicked = {},
            setInputValidity = {},
            onDeletedMessageShown = {},
            onDeletePlaylistsDialogPositiveButtonClicked = {},
            onDeleteDialogNegativeButtonClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FabButtonPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CreateVideoPlaylistFabButton(onCreateVideoPlaylistClick = {})
    }
}

/**
 * Test tag for creating video playlist fab button
 */
const val FAB_BUTTON_TEST_TAG = "video_playlists:fab_button_create_video_playlist"

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
 * Test tag for progressBar
 */
const val PROGRESS_BAR_TEST_TAG = "video_playlists:progress_bar"

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG = "video_playlists:empty_view"