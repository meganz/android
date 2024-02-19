package mega.privacy.android.app.presentation.videosection.view.playlist

import android.annotation.SuppressLint
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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.white_black
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for creating video playlist fab button
 */
const val FAB_BUTTON_TEST_TAG = "fab_button_test_tag"

/**
 * Test tag for CreateVideoPlaylistDialog
 */
const val CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG = "create_video_playlist_dialog_test_tag"

/**
 * Test tag for progressBar
 */
const val PROGRESS_BAR_TEST_TAG = "progress_bar_test_tag"

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG = "video_playlists_empty_view_test_tag"

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
internal fun VideoPlaylistsView(
    items: List<VideoPlaylistUIEntity>,
    progressBarShowing: Boolean,
    searchMode: Boolean,
    scrollToTop: Boolean,
    lazyListState: LazyListState,
    sortOrder: String,
    isInputTitleValid: Boolean,
    showCreateVideoPlaylistDialog: Boolean,
    inputPlaceHolderText: String,
    modifier: Modifier,
    setShowCreateVideoPlaylistDialog: (Boolean) -> Unit,
    setDialogInputPlaceholder: (String) -> Unit,
    onDialogPositiveButtonClicked: (title: String) -> Unit,
    setInputValidity: (Boolean) -> Unit,
    onClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit,
    onMenuClick: (VideoPlaylistUIEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    errorMessage: Int? = null,
    onLongClick: ((item: VideoPlaylistUIEntity, index: Int) -> Unit) = { _, _ -> },
) {
    LaunchedEffect(items) {
        if (scrollToTop) {
            lazyListState.scrollToItem(0)
        }
    }

    val scaffoldState = rememberScaffoldState()
    val isLight = MaterialTheme.colors.isLight

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { snackbarHostState ->
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
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
                    setShowCreateVideoPlaylistDialog(true)
                    setDialogInputPlaceholder(placeholderText)
                }
            )
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            if (showCreateVideoPlaylistDialog) {
                CreateVideoPlaylistDialog(
                    modifier = Modifier.testTag(CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                    title = "Enter playlist name",
                    positiveButtonTextResID = R.string.general_create,
                    inputPlaceHolderText = { inputPlaceHolderText },
                    errorMessage = errorMessage,
                    onDialogInputChange = setInputValidity,
                    onDismissRequest = {
                        setShowCreateVideoPlaylistDialog(false)
                        setInputValidity(true)
                    },
                    onDialogPositiveButtonClicked = { titleOfNewVideoPlaylist ->
                        onDialogPositiveButtonClicked(titleOfNewVideoPlaylist)
                    },
                ) {
                    isInputTitleValid
                }
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
                    modifier = modifier.testTag(VIDEO_PLAYLISTS_EMPTY_VIEW_TEST_TAG),
                    text = "[B]No[/B] [A]playlists[/A] [B]found[/B]",
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
                                thumbnailList = videoPlaylistItem.thumbnailList,
                                totalDuration = videoPlaylistItem.totalDuration,
                                onClick = { onClick(videoPlaylistItem, it) },
                                onMenuClick = { onMenuClick(videoPlaylistItem) },
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
private fun CreateVideoPlaylistFabButton(
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

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(
            items = emptyList(),
            progressBarShowing = false,
            searchMode = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            isInputTitleValid = true,
            showCreateVideoPlaylistDialog = false,
            modifier = Modifier.fillMaxSize(),
            onClick = { _, _ -> },
            onMenuClick = {},
            onSortOrderClick = {},
            inputPlaceHolderText = "New playlist",
            setDialogInputPlaceholder = {},
            setShowCreateVideoPlaylistDialog = {},
            onDialogPositiveButtonClicked = {},
            setInputValidity = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewCreateDialogShownPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(
            items = emptyList(),
            progressBarShowing = false,
            searchMode = false,
            scrollToTop = false,
            lazyListState = LazyListState(),
            sortOrder = "Sort by name",
            isInputTitleValid = true,
            showCreateVideoPlaylistDialog = true,
            modifier = Modifier.fillMaxSize(),
            onClick = { _, _ -> },
            onMenuClick = {},
            onSortOrderClick = {},
            inputPlaceHolderText = "New playlist",
            setDialogInputPlaceholder = {},
            setShowCreateVideoPlaylistDialog = {},
            onDialogPositiveButtonClicked = {},
            setInputValidity = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FabButtonPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CreateVideoPlaylistFabButton(onCreateVideoPlaylistClick = {})
    }
}