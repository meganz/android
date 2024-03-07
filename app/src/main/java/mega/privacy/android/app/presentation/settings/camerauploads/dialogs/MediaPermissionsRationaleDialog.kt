package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for the Media Permissions Rationale Dialog
 */
internal const val MEDIA_PERMISSIONS_RATIONALE_DIALOG =
    "media_permissions_rationale_dialog:mega_alert_dialog"

/**
 * A Composable that displays a Rationale as to why Media Permissions need to be enabled in order to
 * run Camera Uploads
 *
 * @param onMediaAccessGranted Lambda to execute when the User grants Media access
 * @param onMediaAccessDenied Lambda to execute when the User does not grant Media access
 * @param modifier The [Modifier] class
 */
@Composable
internal fun MediaPermissionsRationaleDialog(
    onMediaAccessGranted: () -> Unit,
    onMediaAccessDenied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAlertDialog(
        modifier = modifier.testTag(MEDIA_PERMISSIONS_RATIONALE_DIALOG),
        text = stringResource(R.string.settings_camera_uploads_grant_media_permissions_body),
        confirmButtonText = stringResource(R.string.settings_camera_uploads_grant_media_permissions_positive_button),
        cancelButtonText = stringResource(R.string.settings_camera_uploads_grant_media_permissions_negative_button),
        onConfirm = onMediaAccessGranted,
        onDismiss = onMediaAccessDenied,
    )
}

/**
 * A Composable Preview for [MediaPermissionsRationaleDialog]
 */
@CombinedThemePreviews
@Composable
private fun MediaPermissionsRationaleDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MediaPermissionsRationaleDialog(
            onMediaAccessGranted = {},
            onMediaAccessDenied = {},
        )
    }
}