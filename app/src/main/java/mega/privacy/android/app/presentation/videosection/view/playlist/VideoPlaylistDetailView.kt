package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideoItemView
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewWithImage
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_050_grey_800
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import nz.mega.sdk.MegaNode

/**
 * Video playlist detail view
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlaylistDetailView(
    playlist: VideoPlaylistUIEntity?,
    selectedSize: Int,
    accountType: AccountType?,
    isHideMenuActionVisible: Boolean,
    isUnhideMenuActionVisible: Boolean,
    isInputTitleValid: Boolean,
    numberOfAddedVideos: Int,
    numberOfRemovedItems: Int,
    addedMessageShown: () -> Unit,
    removedMessageShown: () -> Unit,
    inputPlaceHolderText: String,
    setInputValidity: (Boolean) -> Unit,
    onRenameDialogPositiveButtonClicked: (playlistID: NodeId, newTitle: String) -> Unit,
    onDeleteDialogPositiveButtonClicked: (List<VideoPlaylistUIEntity>) -> Unit,
    onDeleteVideosDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit,
    onAddElementsClicked: () -> Unit,
    onPlayAllClicked: () -> Unit,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: ((item: VideoUIEntity, index: Int) -> Unit),
    onBackPressed: () -> Unit,
    onMenuActionClick: (VideoSectionMenuAction?) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: Int? = null,
) {
    val items = playlist?.videos ?: emptyList()
    val lazyListState = rememberLazyListState()

    val isInFirstItem by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex != 0
        }
    }
    var playlistTitle by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(isInFirstItem) {
        playlistTitle = if (isInFirstItem) playlist?.title ?: "" else ""
    }

    val snackBarHostState = remember { SnackbarHostState() }
    val isLight = MaterialTheme.colors.isLight

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )
    val scrollNotInProgress by remember {
        derivedStateOf { !lazyListState.isScrollInProgress }
    }

    val context = LocalContext.current
    LaunchedEffect(numberOfAddedVideos) {
        if (numberOfAddedVideos > 0) {
            val message = context.resources.getQuantityString(
                sharedR.plurals.video_section_playlist_detail_add_videos_message,
                numberOfAddedVideos,
                numberOfAddedVideos,
                playlist?.title
            )
            coroutineScope.launch {
                snackBarHostState.showAutoDurationSnackbar(message)
            }
            addedMessageShown()
        }
    }

    var showDeleteVideosDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteVideoPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameVideoPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(playlist?.title) {
        if (showRenameVideoPlaylistDialog) {
            showRenameVideoPlaylistDialog = false
        }
    }

    LaunchedEffect(numberOfRemovedItems) {
        if (numberOfRemovedItems > 0) {
            val message = context.resources.getQuantityString(
                sharedR.plurals.video_section_playlist_detail_remove_videos_message,
                numberOfRemovedItems,
                numberOfRemovedItems,
                playlist?.title
            )
            coroutineScope.launch {
                snackBarHostState.showAutoDurationSnackbar(message)
            }
            removedMessageShown()
        }
    }

    BackHandler(modalSheetState.isVisible) {
        if (modalSheetState.isVisible) {
            coroutineScope.launch {
                modalSheetState.hide()
            }
        }
    }

    BackHandler(selectedSize > 0) {
        if (selectedSize > 0) {
            onBackPressed()
        }
    }

    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        scaffoldState = rememberScaffoldState(),
        topBar = {
            VideoPlaylistDetailTopBar(
                title = playlistTitle,
                isActionMode = selectedSize > 0,
                selectedSize = selectedSize,
                isHideMenuActionVisible = isHideMenuActionVisible,
                isUnhideMenuActionVisible = isUnhideMenuActionVisible,
                onMenuActionClick = { action ->
                    when (action) {
                        is VideoSectionMenuAction.VideoSectionMoreAction -> {
                            coroutineScope.launch {
                                modalSheetState.show()
                            }
                        }

                        is VideoSectionMenuAction.VideoSectionRemoveAction ->
                            showDeleteVideosDialog = true

                        else -> onMenuActionClick(action)
                    }
                },
                onBackPressed = onBackPressed
            )
        },
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
            CreateVideoPlaylistFabButton(
                showFabButton = scrollNotInProgress,
                onCreateVideoPlaylistClick = onAddElementsClicked
            )
        }
    ) { paddingValue ->
        playlist?.let {
            if (showRenameVideoPlaylistDialog) {
                CreateVideoPlaylistDialog(
                    modifier = Modifier.testTag(DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                    title = stringResource(id = sharedR.string.video_section_playlists_rename_playlist_dialog_title),
                    positiveButtonText = stringResource(id = sharedR.string.video_section_playlists_rename_playlist_dialog_title),
                    inputPlaceHolderText = { inputPlaceHolderText },
                    errorMessage = errorMessage,
                    onDialogInputChange = setInputValidity,
                    onDismissRequest = {
                        showRenameVideoPlaylistDialog = false
                        setInputValidity(true)
                        coroutineScope.launch { modalSheetState.hide() }
                    },
                    initialInputText = { playlist.title },
                    onDialogPositiveButtonClicked = { newTitle ->
                        onRenameDialogPositiveButtonClicked(playlist.id, newTitle)
                    },
                ) {
                    isInputTitleValid
                }
            }

            if (showDeleteVideoPlaylistDialog) {
                DeleteItemsDialog(
                    modifier = Modifier.testTag(DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                    title = stringResource(id = sharedR.string.video_section_playlists_delete_playlist_dialog_title),
                    text = null,
                    confirmButtonText = stringResource(id = sharedR.string.video_section_playlists_delete_playlist_dialog_delete_button),
                    onDeleteButtonClicked = {
                        showDeleteVideoPlaylistDialog = false
                        onDeleteDialogPositiveButtonClicked(listOf(playlist))
                    },
                    onDismiss = {
                        showDeleteVideoPlaylistDialog = false
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
            }

            if (showDeleteVideosDialog) {
                DeleteItemsDialog(
                    modifier = Modifier.testTag(DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG),
                    title = stringResource(id = sharedR.string.video_section_playlist_detail_remove_videos_dialog_title),
                    text = null,
                    confirmButtonText = stringResource(id = sharedR.string.video_section_playlist_detail_remove_videos_dialog_remove_button),
                    onDeleteButtonClicked = {
                        showDeleteVideosDialog = false
                        onDeleteVideosDialogPositiveButtonClicked(playlist)
                    },
                    onDismiss = {
                        showDeleteVideosDialog = false
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
            }
        }

        when {
            items.isEmpty() -> VideoPlaylistEmptyView(
                thumbnailList = playlist?.thumbnailList,
                title = playlist?.title,
                totalDuration = playlist?.totalDuration,
                numberOfVideos = playlist?.numberOfVideos,
                onPlayAllClicked = {},
                modifier = Modifier.testTag(
                    VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG
                )
            )

            else -> {
                LazyColumn(state = lazyListState, modifier = modifier.padding(paddingValue)) {
                    item(
                        key = "header"
                    ) {
                        Column {
                            VideoPlaylistHeaderView(
                                thumbnailList = playlist?.thumbnailList?.map { ThumbnailRequest(it) },
                                title = playlist?.title,
                                totalDuration = playlist?.totalDuration,
                                numberOfVideos = playlist?.numberOfVideos,
                                modifier = Modifier.padding(16.dp),
                                onPlayAllClicked = onPlayAllClicked
                            )
                            MegaDivider(
                                dividerType = DividerType.Centered,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                    items(count = items.size, key = { items[it].id.longValue }) {
                        when {
                            else -> {
                                val videoItem = items[it]
                                VideoItemView(
                                    icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                    name = videoItem.name,
                                    fileSize = formatFileSize(videoItem.size, LocalContext.current),
                                    duration = videoItem.durationString,
                                    isFavourite = videoItem.isFavourite,
                                    isSelected = videoItem.isSelected,
                                    thumbnailData = ThumbnailRequest(videoItem.id),
                                    isSharedWithPublicLink = videoItem.isSharedItems,
                                    labelColor = if (videoItem.label != MegaNode.NODE_LBL_UNKNOWN)
                                        colorResource(
                                            id = MegaNodeUtil.getNodeLabelColor(
                                                videoItem.label
                                            )
                                        ) else null,
                                    nodeAvailableOffline = videoItem.nodeAvailableOffline,
                                    onClick = { onClick(videoItem, it) },
                                    onMenuClick = { onMenuClick(videoItem) },
                                    onLongClick = { onLongClick(videoItem, it) },
                                    modifier = Modifier
                                        .alpha(0.5f.takeIf {
                                            accountType?.isPaid == true && (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited)
                                        } ?: 1f),
                                    isSensitive = accountType?.isPaid == true && (videoItem.isMarkedSensitive || videoItem.isSensitiveInherited),
                                )
                            }
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
                showDeleteVideoPlaylistDialog = true
            }
        )
    }
}

@Composable
internal fun VideoPlaylistEmptyView(
    thumbnailList: List<Any?>?,
    title: String?,
    totalDuration: String?,
    numberOfVideos: Int?,
    onPlayAllClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        VideoPlaylistHeaderView(
            thumbnailList = thumbnailList,
            title = title,
            totalDuration = totalDuration,
            numberOfVideos = numberOfVideos,
            modifier = Modifier.padding(16.dp),
            onPlayAllClicked = onPlayAllClicked
        )
        MegaDivider(
            dividerType = DividerType.Centered,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LegacyMegaEmptyViewWithImage(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(id = sharedR.string.video_section_playlist_detail_empty_hint_videos),
            imagePainter = painterResource(id = iconPackR.drawable.ic_video_section_empty_video)
        )
    }
}

@Composable
internal fun VideoPlaylistHeaderView(
    thumbnailList: List<Any?>?,
    title: String?,
    totalDuration: String?,
    numberOfVideos: Int?,
    onPlayAllClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val thumbnailModifier = Modifier
                .width(126.dp)
                .aspectRatio(1.77f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colors.grey_050_grey_800)

            ThumbnailListView(
                emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
                noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
                modifier = thumbnailModifier,
                thumbnailList = thumbnailList
            )

            VideoPlaylistInfoView(
                title = title ?: "",
                totalDuration = totalDuration ?: "00:00:00",
                numberOfVideos = numberOfVideos ?: 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
        PlayAllButtonView(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
                .clickable { onPlayAllClicked() }
                .testTag(DETAIL_PLAY_ALL_BUTTON_TEST_TAG)
        )
    }
}

@Composable
internal fun VideoPlaylistInfoView(
    title: String,
    totalDuration: String,
    numberOfVideos: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(start = 16.dp)
    ) {
        MegaText(
            modifier = modifier
                .fillMaxSize()
                .weight(1.5f)
                .testTag(PLAYLIST_TITLE_TEST_TAG),
            text = title,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1,
            overflow = LongTextBehaviour.Clip(2)
        )

        MegaText(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(PLAYLIST_TOTAL_DURATION_TEST_TAG),
            text = totalDuration,
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption
        )

        MegaText(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG),
            text = if (numberOfVideos == 0) {
                stringResource(id = sharedR.string.video_section_playlist_detail_empty_hint_videos).lowercase()
            } else {
                pluralStringResource(
                    id = sharedR.plurals.video_section_playlist_detail_video_number,
                    count = numberOfVideos,
                    numberOfVideos
                )
            },
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
internal fun PlayAllButtonView(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(5.dp)
            )
    ) {
        Image(
            painter = painterResource(id = iconPackR.drawable.ic_playlist_play_all),
            contentDescription = "play all",
            modifier = Modifier
                .padding(start = 20.dp, end = 5.dp)
                .size(12.dp)
                .align(Alignment.CenterVertically)
        )

        MegaText(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 20.dp),
            text = stringResource(id = sharedR.string.video_section_playlist_detail_play_all_button),
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.caption
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistDetailViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistDetailView(
            playlist = null,
            selectedSize = 0,
            accountType = AccountType.FREE,
            isHideMenuActionVisible = true,
            isUnhideMenuActionVisible = true,
            isInputTitleValid = true,
            inputPlaceHolderText = "",
            setInputValidity = {},
            onRenameDialogPositiveButtonClicked = { _, _ -> },
            onDeleteDialogPositiveButtonClicked = {},
            onAddElementsClicked = {},
            addedMessageShown = {},
            numberOfAddedVideos = 0,
            onDeleteVideosDialogPositiveButtonClicked = {},
            removedMessageShown = {},
            numberOfRemovedItems = 0,
            onPlayAllClicked = {},
            onBackPressed = {},
            onClick = { _, _ -> },
            onLongClick = { _, _ -> },
            onMenuClick = {},
            onMenuActionClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistDetailViewUnderActionModePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistDetailView(
            playlist = null,
            selectedSize = 2,
            accountType = AccountType.FREE,
            isHideMenuActionVisible = true,
            isUnhideMenuActionVisible = true,
            isInputTitleValid = true,
            inputPlaceHolderText = "",
            setInputValidity = {},
            onRenameDialogPositiveButtonClicked = { _, _ -> },
            onDeleteDialogPositiveButtonClicked = {},
            onAddElementsClicked = {},
            addedMessageShown = {},
            numberOfAddedVideos = 0,
            onDeleteVideosDialogPositiveButtonClicked = {},
            removedMessageShown = {},
            numberOfRemovedItems = 0,
            onPlayAllClicked = {},
            onBackPressed = {},
            onClick = { _, _ -> },
            onLongClick = { _, _ -> },
            onMenuClick = {},
            onMenuActionClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistHeaderViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistHeaderView(
            modifier = Modifier,
            thumbnailList = listOf(null),
            title = "New Playlist",
            totalDuration = "00:00:00",
            numberOfVideos = 0,
            onPlayAllClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PlayAllButtonViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PlayAllButtonView()
    }
}

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG = "video_playlist_detail_empty_view_test_tag"

/**
 * Test tag for playlist title
 */
const val PLAYLIST_TITLE_TEST_TAG = "playlist_title_test_tag"

/**
 * Test tag for playlist total duration
 */
const val PLAYLIST_TOTAL_DURATION_TEST_TAG = "playlist_total_duration_test_tag"

/**
 * Test tag for playlist number of videos
 */
const val PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG = "playlist_number_of_videos_test_tag"

/**
 * Test tag for RenameVideoPlaylistDialog in detail page
 */
const val DETAIL_RENAME_VIDEO_PLAYLIST_DIALOG_TEST_TAG =
    "detail_rename_video_playlist_dialog_test_tag"

/**
 * Test tag for delete video playlist in detail page
 */
const val DETAIL_DELETE_VIDEO_PLAYLIST_DIALOG_TEST_TAG =
    "detail_delete_video_playlist_dialog_test_tag"

/**
 * Test tag for delete videos dialog in detail page
 */
const val DETAIL_DELETE_VIDEOS_DIALOG_TEST_TAG = "detail_delete_videos_dialog_test_tag"

/**
 * Test tag for play all button in detail page
 */
const val DETAIL_PLAY_ALL_BUTTON_TEST_TAG = "detail_play_all_button_test_tag"


internal const val videoPlaylistDetailRoute = "videoSection/video_playlist/detail"