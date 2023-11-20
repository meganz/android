package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * The dialog to choose different options of mute push notification.
 */
@Composable
fun MutePushNotificationDialog(
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
) =
    ConfirmationDialogWithRadioButtons(
        titleText = "mute chat",
        confirmButtonText = stringResource(id = R.string.general_ok),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        radioOptions = listOf("1", "2", "3", "4", "5"),
        onDismissRequest = onCancel,
        onConfirmRequest = onConfirm,
        onOptionSelected = {},
    )

@CombinedThemePreviews
@Composable
private fun MutePushNotificationDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MutePushNotificationDialog()
    }
}
