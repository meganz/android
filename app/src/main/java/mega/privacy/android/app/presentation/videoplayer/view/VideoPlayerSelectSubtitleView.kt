package mega.privacy.android.app.presentation.videoplayer.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.inputfields.SearchInputField
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerSubtitleUiState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.feature.clouddrive.presentation.search.view.SearchEmptyView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun VideoPlayerSelectSubtitleView(
    uiState: VideoPlayerSubtitleUiState,
    onLoadSubtitleList: suspend () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onItemClicked: (SubtitleFileInfo) -> Unit,
    onClearSelectedItem: () -> Unit,
    onAddSubtitle: (SubtitleFileInfo?) -> Unit,
    onBackPressed: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onLoadSubtitleList()
    }

    BackHandler {
        when {
            uiState.selectedSubtitleFileInfo != null -> onClearSelectedItem()
            else -> onBackPressed()
        }
    }

    SelectSubtitleView(
        uiState = uiState,
        onSearchTextChange = onSearchTextChange,
        itemClicked = onItemClicked,
        onAddSubtitle = onAddSubtitle,
        onBackPressed = onBackPressed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectSubtitleView(
    uiState: VideoPlayerSubtitleUiState,
    onSearchTextChange: (String) -> Unit,
    itemClicked: (SubtitleFileInfo) -> Unit,
    onAddSubtitle: (SubtitleFileInfo?) -> Unit,
    onBackPressed: () -> Unit,
) {
    val isLoading = uiState.isLoading
    val items = uiState.items
    val query = uiState.query
    val selectedSubtitleFileInfo = uiState.selectedSubtitleFileInfo

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_BAR_TEST_TAG),
                navigationType = AppBarNavigationType.Back(onBackPressed),
                title = stringResource(R.string.media_player_video_select_subtitle_file_title),
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    MegaDivider(dividerType = DividerType.FullSize)

                    PrimaryFilledButton(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(VIDEO_PLAYER_SELECT_SUBTITLE_ADD_BUTTON_TEST_TAG),
                        text = stringResource(id = sharedR.string.video_player_subtitles_add_subtitles_button),
                        onClick = { onAddSubtitle(selectedSubtitleFileInfo) },
                        enabled = selectedSubtitleFileInfo != null
                    )

                    TextOnlyButton(
                        modifier = Modifier
                            .padding(bottom = 48.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(VIDEO_PLAYER_SELECT_SUBTITLE_CANCEL_BUTTON_TEST_TAG),
                        text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                        onClick = onBackPressed,
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            if (items.isNotEmpty() || query?.isNotEmpty() == true) {
                SearchInputField(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_INPUT_TEST_TAG),
                    text = query ?: "",
                    placeHolderText = stringResource(id = R.string.hint_action_search),
                    onValueChanged = onSearchTextChange,
                    capitalization = KeyboardCapitalization.None,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                        content = {
                            MegaCircularProgressIndicator(
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag(VIDEO_PLAYER_SELECT_SUBTITLE_PROGRESS_TEST_TAG),
                            )
                        },
                    )

                    items.isEmpty() && query.isNullOrEmpty() -> EmptyStateView(
                        modifier = Modifier.padding(horizontal = 36.dp).testTag(
                            VIDEO_PLAYER_SELECT_SUBTITLE_EMPTY_LIST_TEST_TAG
                        ),
                        title = stringResource(id = R.string.media_player_video_select_subtitle_file_empty_message),
                        description = SpannableText(stringResource(sharedR.string.video_player_subtitles_empty_hint_description)),
                        imagePainter = painterResource(id = iconPackR.drawable.ic_playlist_glass)
                    )

                    items.isEmpty() -> SearchEmptyView(
                        modifier = Modifier.testTag(
                            VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_EMPTY_TEST_TAG
                        )
                    )

                    else -> SubtitleFileInfoListView(
                        modifier = Modifier
                            .testTag(VIDEO_PLAYER_SELECT_SUBTITLE_FILES_TEST_TAG)
                            .fillMaxSize(),
                        subtitleInfoList = items,
                        hiddenNodesEnabled = uiState.hiddenNodesEnabled,
                        onClicked = itemClicked,
                    )
                }
            }
        }
    }
}

/** Test tag for the loading progress indicator */
const val VIDEO_PLAYER_SELECT_SUBTITLE_PROGRESS_TEST_TAG =
    "video_player_select_subtitle:progress"

/** Test tag for the empty state view shown when no subtitle files are available */
const val VIDEO_PLAYER_SELECT_SUBTITLE_EMPTY_LIST_TEST_TAG =
    "video_player_select_subtitle:empty_list"

/** Test tag for the subtitle file list */
const val VIDEO_PLAYER_SELECT_SUBTITLE_FILES_TEST_TAG =
    "video_player_select_subtitle:files"

/** Test tag for the top app bar */
const val VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_BAR_TEST_TAG =
    "video_player_select_subtitle:search_bar"

/** Test tag for the search input field */
const val VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_INPUT_TEST_TAG =
    "video_player_select_subtitle:search_input"

/** Test tag for the add subtitles button */
const val VIDEO_PLAYER_SELECT_SUBTITLE_ADD_BUTTON_TEST_TAG =
    "video_player_select_subtitle:add_button"

/** Test tag for the cancel button */
const val VIDEO_PLAYER_SELECT_SUBTITLE_CANCEL_BUTTON_TEST_TAG =
    "video_player_select_subtitle:cancel_button"

/** Test tag for the empty state view shown when search yields no results */
const val VIDEO_PLAYER_SELECT_SUBTITLE_SEARCH_EMPTY_TEST_TAG =
    "video_player_select_subtitle:search_empty"

/** Test tag for the checkbox on a selected subtitle item */
const val VIDEO_PLAYER_SELECT_SUBTITLE_ITEM_CHECKBOX_TEST_TAG =
    "video_player_select_subtitle:item_checkbox"

private val previewSubtitleFileInfo = SubtitleFileInfo(
    id = 1L,
    name = "subtitle.srt",
    url = null,
    parentName = "Movies",
    isMarkedSensitive = false,
    isSensitiveInherited = false,
)

@CombinedThemePreviews
@Composable
private fun SelectSubtitleViewLoadingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SelectSubtitleView(
            uiState = VideoPlayerSubtitleUiState(isLoading = true),
            onSearchTextChange = {},
            itemClicked = {},
            onAddSubtitle = {},
            onBackPressed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SelectSubtitleViewEmptyPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SelectSubtitleView(
            uiState = VideoPlayerSubtitleUiState(isLoading = false),
            onSearchTextChange = {},
            itemClicked = {},
            onAddSubtitle = {},
            onBackPressed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SelectSubtitleViewSearchEmptyPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SelectSubtitleView(
            uiState = VideoPlayerSubtitleUiState(isLoading = false, query = "no match"),
            onSearchTextChange = {},
            itemClicked = {},
            onAddSubtitle = {},
            onBackPressed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SelectSubtitleViewWithItemsPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SelectSubtitleView(
            uiState = VideoPlayerSubtitleUiState(
                isLoading = false,
                items = listOf(
                    SubtitleFileInfoItem(subtitleFileInfo = previewSubtitleFileInfo),
                    SubtitleFileInfoItem(
                        subtitleFileInfo = previewSubtitleFileInfo.copy(
                            id = 2L,
                            name = "subtitles_extended.srt",
                            parentName = "Downloads",
                        ),
                        selected = true,
                    ),
                ),
                selectedSubtitleFileInfo = previewSubtitleFileInfo,
            ),
            onSearchTextChange = {},
            itemClicked = {},
            onAddSubtitle = {},
            onBackPressed = {},
        )
    }
}

