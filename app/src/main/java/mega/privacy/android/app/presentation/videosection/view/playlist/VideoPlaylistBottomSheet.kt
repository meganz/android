package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun VideoPlaylistBottomSheet(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onRenameVideoPlaylistClicked: () -> Unit,
    onDeleteVideoPlaylistClicked: () -> Unit,
) {
    BottomSheet(modalSheetState = modalSheetState,
        sheetBody = {
            VideoPlaylistBottomSheetBody(
                onRenameVideoPlaylistClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onRenameVideoPlaylistClicked()
                },
                onDeleteVideoPlaylistClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onDeleteVideoPlaylistClicked()
                }
            )
        }
    )
}

@Composable
internal fun VideoPlaylistBottomSheetBody(
    onRenameVideoPlaylistClicked: () -> Unit,
    onDeleteVideoPlaylistClicked: () -> Unit,
) {
    Column {
        RenameVideoPlaylistBottomSheetTile(onRenameVideoPlaylistClicked)
        DeleteVideoPlaylistBottomSheetTile(onDeleteVideoPlaylistClicked)
    }
}

@Composable
internal fun RenameVideoPlaylistBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(VIDEO_PLAYLIST_RENAME_BOTTOM_SHEET_TILE_TEST_TAG),
        dividerType = null,
        text = stringResource(id = R.string.video_section_playlists_rename_playlist_dialog_title),
        icon = painterResource(id = iconPackR.drawable.ic_pen_02_medium_regular_outline),
        onActionClicked = onActionClicked,
    )
}

@Composable
internal fun DeleteVideoPlaylistBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        modifier = Modifier.testTag(VIDEO_PLAYLIST_DELETE_BOTTOM_SHEET_TILE_TEST_TAG),
        dividerType = null,
        text = stringResource(id = R.string.video_section_playlist_bottom_sheet_option_title_delete),
        icon = painterResource(id = iconPackR.drawable.ic_trash_medium_regular_outline),
        onActionClicked = onActionClicked,
    )
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistBottomSheetBodyPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistBottomSheetBody(
            onRenameVideoPlaylistClicked = {},
            onDeleteVideoPlaylistClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RenameVideoPlaylistBottomSheetTilePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RenameVideoPlaylistBottomSheetTile(onActionClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun DeleteVideoPlaylistBottomSheetTilePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DeleteVideoPlaylistBottomSheetTile(onActionClicked = {})
    }
}

/**
 * Test tag for rename bottom Sheet tile
 */
const val VIDEO_PLAYLIST_RENAME_BOTTOM_SHEET_TILE_TEST_TAG =
    "video_playlist:bottom_sheet_tile_rename"

/**
 * Test tag for delete bottom Sheet tile
 */
const val VIDEO_PLAYLIST_DELETE_BOTTOM_SHEET_TILE_TEST_TAG =
    "video_playlist:bottom_sheet_tile_delete"
