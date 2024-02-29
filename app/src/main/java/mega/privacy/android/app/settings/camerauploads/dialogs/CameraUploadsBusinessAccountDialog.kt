package mega.privacy.android.app.settings.camerauploads.dialogs

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
 * Test Tag for the Camera Uploads Business Account Dialog
 */
internal const val CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG =
    "camera_uploads_business_account_dialog:mega_alert_dialog"

/**
 * A Composable Dialog informing the Business Account User that the Business Account Administrator
 * has access to his/her Camera Uploads content
 *
 * @param onAlertAcknowledged Lambda to execute when the User acknowledges that the Business Account
 * Administrator can access his/her Camera Uploads content
 * @param onAlertDismissed Lambda to execute when the User dismisses the Dialog
 * @param modifier The [Modifier] class
 */
@Composable
internal fun CameraUploadsBusinessAccountDialog(
    onAlertAcknowledged: () -> Unit,
    onAlertDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaAlertDialog(
        modifier = modifier.testTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG),
        title = stringResource(R.string.section_photo_sync),
        text = stringResource(R.string.camera_uploads_business_alert),
        confirmButtonText = stringResource(R.string.general_enable),
        cancelButtonText = stringResource(R.string.general_cancel),
        onConfirm = onAlertAcknowledged,
        onDismiss = onAlertDismissed,
    )
}

/**
 * A Preview Composable for [CameraUploadsBusinessAccountDialog]
 */
@CombinedThemePreviews
@Composable
private fun CameraUploadsBusinessAccountDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsBusinessAccountDialog(
            onAlertAcknowledged = {},
            onAlertDismissed = {},
        )
    }
}