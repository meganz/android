package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * All contacts added dialog
 *
 */
@Composable
fun EnableGeolocationDialog(
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.title_activity_maps),
        text = stringResource(id = R.string.explanation_send_location),
        confirmButtonText = stringResource(id = R.string.button_continue),
        onDismiss = onDismiss,
        cancelButtonText = stringResource(id = R.string.button_cancel),
        onConfirm = {
            onConfirm()
            onDismiss()
        },
        modifier = Modifier.testTag(TEST_TAG_ENABLE_GEOLOCATION_DIALOG)
    )
}

@CombinedThemePreviews
@Composable
private fun EnableGeolocationDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        EnableGeolocationDialog()
    }
}

internal const val TEST_TAG_ENABLE_GEOLOCATION_DIALOG =
    "chat_view:attach_location:enable_geolocation_dialog"