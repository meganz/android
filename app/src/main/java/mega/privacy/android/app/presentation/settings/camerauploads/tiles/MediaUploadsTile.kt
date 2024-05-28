package mega.privacy.android.app.presentation.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A [Composable] to enable or disable Secondary Media Uploads. This is also known as Secondary
 * Uploads
 *
 * @param isMediaUploadsEnabled true if Media Uploads is enabled
 * @param onItemClicked Lambda to execute when the Tile is clicked
 * @param modifier The [Modifier]
 */
@Composable
internal fun MediaUploadsTile(
    isMediaUploadsEnabled: Boolean,
    onItemClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        GenericTwoLineListItem(
            modifier = Modifier.testTag(MEDIA_UPLOADS_TILE),
            title = stringResource(
                if (isMediaUploadsEnabled) {
                    R.string.settings_secondary_upload_off
                } else {
                    R.string.settings_secondary_upload_on
                }
            ),
            onItemClicked = { onItemClicked.invoke(!isMediaUploadsEnabled) },
        )
        if (isMediaUploadsEnabled) {
            MegaDivider(
                modifier = Modifier.testTag(MEDIA_UPLOADS_TILE_DIVIDER),
                dividerType = DividerType.FullSize,
            )
        }
    }
}

/**
 * A Preview [Composable] for [MediaUploadsTile]
 *
 * @param isMediaUploadsEnabled true if Media Uploads is enabled
 */
@CombinedThemePreviews
@Composable
private fun MediaUploadsTilePreview(
    @PreviewParameter(BooleanProvider::class) isMediaUploadsEnabled: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaUploadsTile(
            isMediaUploadsEnabled = isMediaUploadsEnabled,
            onItemClicked = {},
        )
    }
}

/**
 * Test Tags for the Media Uploads Tile
 */
internal const val MEDIA_UPLOADS_TILE =
    "media_uploads_tile:generic_two_line_list_item"
internal const val MEDIA_UPLOADS_TILE_DIVIDER =
    "media_uploads_tile:mega_divider"