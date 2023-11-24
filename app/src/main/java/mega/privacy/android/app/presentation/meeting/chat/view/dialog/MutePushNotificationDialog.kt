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
    isMeeting: Boolean = false,
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
) =
    ConfirmationDialogWithRadioButtons(
        titleText = getTitle(isMeeting),
        confirmButtonText = stringResource(id = R.string.general_ok),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        radioOptions = listOf("1", "2", "3", "4", "5"),
        initialSelectedOption = null,
        onDismissRequest = onCancel,
        onConfirmRequest = { println("selection = $it") },
        onOptionSelected = {},
    )

@Composable
private fun getTitle(isMeeting: Boolean) =
    if (isMeeting)
        stringResource(id = R.string.meetings_mute_notifications_dialog_title)
    else
        stringResource(
            id = R.string.title_dialog_mute_chatroom_notifications
        )

@CombinedThemePreviews
@Composable
private fun MutePushNotificationDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MutePushNotificationDialog()
    }
}
