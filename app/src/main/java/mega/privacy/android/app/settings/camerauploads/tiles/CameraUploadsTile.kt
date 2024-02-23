package mega.privacy.android.app.settings.camerauploads.tiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test Tags for the Camera Uploads Tile
 */
internal const val CAMERA_UPLOADS_TILE = "camera_uploads_option:menu_action_list_tile"
internal const val CAMERA_UPLOADS_TILE_SWITCH = "camera_uploads_option:mega_switch"

/**
 * A [Composable] that displays a [MegaSwitch] to enable or disable Camera Uploads
 *
 * @param isChecked true if the [MegaSwitch] is checked
 * @param onCheckedChange Lambda to execute when the [MegaSwitch] checked state has changed
 */
@Composable
internal fun CameraUploadsTile(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        MenuActionListTile(
            modifier = Modifier.testTag(CAMERA_UPLOADS_TILE),
            text = stringResource(R.string.section_photo_sync),
            addSeparator = isChecked,
            dividerPadding = 0.dp,
            addIconPadding = false,
        ) {
            MegaSwitch(
                modifier = Modifier.testTag(CAMERA_UPLOADS_TILE_SWITCH),
                checked = isChecked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

/**
 * A [Composable] Preview for [CameraUploadsTile]
 *
 * @param isChecked [PreviewParameter] that controls the [MegaSwitch] checked state
 */
@CombinedThemePreviews
@Composable
private fun CameraUploadsOptionPreview(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsTile(
            isChecked = isChecked,
            onCheckedChange = {},
        )
    }
}