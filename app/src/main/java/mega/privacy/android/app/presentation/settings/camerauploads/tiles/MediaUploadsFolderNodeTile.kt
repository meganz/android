package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A [Composable] that displays the Media Uploads (Secondary Folder) Folder Name from Cloud Drive.
 * Clicking the Tile allows the User to select a new Secondary Cloud Drive Folder
 *
 * @param secondaryFolderName The Media Uploads Cloud Drive Folder name
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun MediaUploadsFolderNodeTile(
    secondaryFolderName: String,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(MEDIA_UPLOADS_FOLDER_NODE_TILE),
            title = stringResource(R.string.settings_mega_secondary_folder),
            subtitle = secondaryFolderName,
            showEntireSubtitle = true,
            onItemClicked = onItemClicked,
        )
    }
}

/**
 * A Preview [Composable] for [MediaUploadsFolderNodeTile]
 */
@CombinedThemePreviews
@Composable
private fun MediaUploadsFolderNodeTileTest() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaUploadsFolderNodeTile(
            secondaryFolderName = "Media Uploads",
            onItemClicked = {},
        )
    }
}

/**
 * Test Tag for the Media Uploads Folder Node Tile
 */
internal const val MEDIA_UPLOADS_FOLDER_NODE_TILE =
    "media_uploads_folder_node_tile:generic_two_line_list_item"