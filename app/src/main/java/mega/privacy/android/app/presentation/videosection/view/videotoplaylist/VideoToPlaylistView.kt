package mega.privacy.android.app.presentation.videosection.view.videotoplaylist

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistSetUiEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionLoadingView
import mega.privacy.android.app.presentation.videosection.view.playlist.CreateVideoPlaylistDialog
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyViewWithImage
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacySearchAppBar
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun VideoToPlaylistView(
    items: List<VideoPlaylistSetUiEntity>,
    isLoading: Boolean,
    isInputTitleValid: Boolean,
    showCreateVideoPlaylistDialog: Boolean,
    inputPlaceHolderText: String,
    setShouldCreateVideoPlaylist: (Boolean) -> Unit,
    onCreateDialogPositiveButtonClicked: (String) -> Unit,
    setInputValidity: (Boolean) -> Unit,
    setDialogInputPlaceholder: (String) -> Unit,
    searchState: SearchWidgetState,
    query: String?,
    hasSelectedItems: Boolean,
    modifier: Modifier = Modifier,
    onSearchTextChange: (String) -> Unit = {},
    onCloseClicked: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    onItemClicked: (Int, VideoPlaylistSetUiEntity) -> Unit = { _, _ -> },
    onDoneButtonClicked: () -> Unit = {},
    errorMessage: Int? = null,
) {
    val lazyListState = rememberLazyListState()

    MegaScaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            LegacySearchAppBar(
                modifier = Modifier.testTag(VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG),
                searchWidgetState = searchState,
                typedSearch = query ?: "",
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                onBackPressed = onBackPressed,
                onSearchClicked = onSearchClicked,
                elevation = false,
                title = stringResource(id = sharedR.string.video_to_playlist_top_bar_title),
                hintId = R.string.hint_action_search,
                isHideAfterSearch = true
            )
        }
    ) { paddingValues ->
        if (showCreateVideoPlaylistDialog) {
            CreateVideoPlaylistDialog(
                modifier = Modifier.testTag(VIDEO_TO_PLAYLIST_CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG),
                title = stringResource(id = sharedR.string.video_section_playlists_create_playlist_dialog_title),
                positiveButtonText = stringResource(id = R.string.general_create),
                inputPlaceHolderText = { inputPlaceHolderText },
                errorMessage = errorMessage,
                onDialogInputChange = setInputValidity,
                onDismissRequest = {
                    setShouldCreateVideoPlaylist(false)
                    setInputValidity(true)
                },
                onDialogPositiveButtonClicked = { titleOfNewVideoPlaylist ->
                    onCreateDialogPositiveButtonClicked(titleOfNewVideoPlaylist)
                },
            ) {
                isInputTitleValid
            }
        }

        Column(modifier = Modifier.padding(paddingValues)) {
            MegaText(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag(VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG)
                    .clickable {
                        val placeholderText = "New playlist"
                        setShouldCreateVideoPlaylist(true)
                        setDialogInputPlaceholder(placeholderText)
                    },
                text = stringResource(id = sharedR.string.video_to_playlist_new_playlist_text),
                textColor = TextColor.Accent
            )

            when {
                isLoading -> VideoSectionLoadingView()

                items.isEmpty() -> LegacyMegaEmptyViewWithImage(
                    modifier = Modifier.testTag(VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = sharedR.string.video_section_playlists_empty_hint_playlist),
                    imagePainter = painterResource(id = iconPackR.drawable.ic_homepage_empty_playlists)
                )

                else -> LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .testTag(VIDEO_TO_PLAYLIST_LIST_TEST_TAG)
                ) {
                    items(count = items.size, key = { items[it].id }) { index ->
                        val set = items[index]
                        GenericTwoLineListItem(
                            title = set.title,
                            trailingIcons = {
                                MegaCheckbox(
                                    modifier = Modifier.testTag(
                                        "$VIDEO_TO_PLAYLIST_ITEM_CHECK_BOX_TEST_TAG$index"
                                    ),
                                    checked = set.isSelected,
                                    rounded = false,
                                    onCheckedChange = {
                                        onItemClicked(index, set)
                                    }
                                )
                            }
                        )
                        MegaDivider(
                            dividerType = DividerType.SmallStartPadding,
                            modifier = Modifier.testTag("$VIDEO_TO_PLAYLIST_DIVIDER_TEST_TAG$index")
                        )
                    }
                }
            }

            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(VIDEO_TO_PLAYLIST_DONE_BUTTON_TEST_TAG),
                enabled = hasSelectedItems,
                textId = sharedR.string.video_to_playlist_done_button,
                onClick = onDoneButtonClicked
            )
        }

    }
}

@CombinedThemePreviews
@Composable
private fun VideoToPlaylistViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoToPlaylistView(
            items = provideTestItems(),
            searchState = SearchWidgetState.COLLAPSED,
            query = "",
            hasSelectedItems = true,
            isLoading = false,
            isInputTitleValid = true,
            showCreateVideoPlaylistDialog = false,
            inputPlaceHolderText = "",
            setShouldCreateVideoPlaylist = {},
            onCreateDialogPositiveButtonClicked = {},
            setInputValidity = {},
            setDialogInputPlaceholder = {}
        )
    }
}

private fun provideTestItems() = (0..20).map {
    VideoPlaylistSetUiEntity(
        id = it.toLong(),
        title = "Video Playlist Set $it",
        isSelected = it % 2 == 0
    )
}

/**
 *  Test tag for search top bar of the video to playlist
 */
const val VIDEO_TO_PLAYLIST_SEARCH_BAR_TEST_TAG = "video_to_playlist_view:top_bar_search"

/**
 * Test tag for the list of the video to playlist
 */
const val VIDEO_TO_PLAYLIST_LIST_TEST_TAG = "video_to_playlist_view:lazy_colum_list"

/**
 * Test tag for the divider of the video to playlist
 */
const val VIDEO_TO_PLAYLIST_DIVIDER_TEST_TAG = "video_to_playlist_item:divider"

/**
 * Test tag for the check box of the video to playlist
 */
const val VIDEO_TO_PLAYLIST_ITEM_CHECK_BOX_TEST_TAG = "video_to_playlist_item:check_box"

/**
 * Test tag for the done button of the video to playlist
 */
const val VIDEO_TO_PLAYLIST_DONE_BUTTON_TEST_TAG = "video_to_playlist_item:button_done"

/**
 * Test tag for the new playlist of the video to playlist
 */
const val VIDEO_TO_PLAYLIST_NEW_PLAYLIST_TEST_TAG = "video_to_playlist_item:text_new_playlist"

/**
 * Test tag for empty view
 */
const val VIDEO_TO_PLAYLIST_EMPTY_VIEW_TEST_TAG = "video_to_playlist_view:empty_view"

/**
 * Test tag for CreateVideoPlaylistDialog
 */
const val VIDEO_TO_PLAYLIST_CREATE_VIDEO_PLAYLIST_DIALOG_TEST_TAG =
    "video_to_playlist_view:dialog_create_video_playlist"
