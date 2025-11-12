package mega.privacy.android.app.presentation.videosection.view.playlist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun VideoPlaylistBottomSheet(
    sheetState: SheetState,
    onRenameVideoPlaylistClicked: () -> Unit,
    onDeleteVideoPlaylistClicked: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
        onDismissRequest = onDismissRequest,
        content = {
            VideoPlaylistBottomSheetBody(
                onRenameVideoPlaylistClicked = onRenameVideoPlaylistClicked,
                onDeleteVideoPlaylistClicked = onDeleteVideoPlaylistClicked
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
        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Pen2),
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
        icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash),
        onActionClicked = onActionClicked,
    )
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistBottomSheetBodyPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistBottomSheetBody(
            onRenameVideoPlaylistClicked = {},
            onDeleteVideoPlaylistClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RenameVideoPlaylistBottomSheetTilePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RenameVideoPlaylistBottomSheetTile(onActionClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun DeleteVideoPlaylistBottomSheetTilePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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
