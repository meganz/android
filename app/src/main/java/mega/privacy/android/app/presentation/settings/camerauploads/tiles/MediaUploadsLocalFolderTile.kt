package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A [Composable] that displays the Media Uploads (Secondary Folder) Local Folder Path. Clicking the
 * Tile allows the User to select a new Secondary Local Folder
 *
 * @param secondaryFolderPath The Media Uploads Local Secondary Folder Path
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun MediaUploadsLocalFolderTile(
    secondaryFolderPath: String,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(MEDIA_UPLOADS_LOCAL_FOLDER_TILE),
            title = stringResource(R.string.settings_local_secondary_folder),
            subtitle = secondaryFolderPath,
            showEntireSubtitle = true,
            onItemClicked = onItemClicked,
        )
        MegaDivider(
            modifier = Modifier.testTag(MEDIA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}

/**
 * A Preview [Composable] for [MediaUploadsLocalFolderTile]
 */
@CombinedThemePreviews
@Composable
private fun MediaUploadsLocalFolderTilePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaUploadsLocalFolderTile(
            secondaryFolderPath = "secondary/folder/path",
            onItemClicked = {},
        )
    }
}

/**
 * Test Tags for the Media Uploads Local Folder Tile
 */
internal const val MEDIA_UPLOADS_LOCAL_FOLDER_TILE =
    "media_uploads_local_folder_tile:generic_two_line_list_item"
internal const val MEDIA_UPLOADS_LOCAL_FOLDER_TILE_DIVIDER =
    "media_uploads_local_folder_tile:mega_divider"