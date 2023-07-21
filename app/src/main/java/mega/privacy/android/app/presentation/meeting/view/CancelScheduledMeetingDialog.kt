package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Show cancel scheduled meeting dialog
 *
 * @param isChatHistoryEmpty    True if the chat room history is empty (only management messages) or false otherwise
 * @param chatTitle             Char room title
 * @param onConfirm             To be triggered when confirm button is pressed
 * @param onDismiss             To be triggered when dialog is hidden
 */
@Composable
fun CancelScheduledMeetingDialog(
    isChatHistoryEmpty: Boolean,
    chatTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MegaAlertDialog(
        title = stringResource(
            R.string.meetings_cancel_scheduled_meeting_dialog_title, chatTitle
        ),
        text = stringResource(
            if (isChatHistoryEmpty) {
                R.string.meetings_cancel_scheduled_meeting_chat_history_empty_dialog_message
            } else {
                R.string.meetings_cancel_scheduled_meeting_chat_history_not_empty_dialog_message
            }
        ),
        confirmButtonText = stringResource(
            if (isChatHistoryEmpty) {
                R.string.meetings_cancel_scheduled_meeting_chat_history_empty_dialog_confirm_button
            } else {
                R.string.meetings_cancel_scheduled_meeting_chat_history_not_empty_dialog_confirm_button
            }
        ),
        cancelButtonText = stringResource(id = R.string.meetings_cancel_scheduled_meeting_dialog_do_not_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * [CancelScheduledMeetingDialog] preview if chat room history is empty (only management messages)
 */
@Preview
@Composable
fun PreviewEmptyHistoryCancelScheduledMeetingDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CancelScheduledMeetingDialog(
            isChatHistoryEmpty = true,
            chatTitle = "Book Club - Breasts&Eggs",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

/**
 * [CancelScheduledMeetingDialog] preview if chat room history is not empty (only management messages)
 */
@Preview
@Composable
fun PreviewNoEmptyHistoryCancelScheduledMeetingDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CancelScheduledMeetingDialog(isChatHistoryEmpty = false,
            chatTitle = "Book Club - Breasts&Eggs",
            onConfirm = {},
            onDismiss = {})
    }
}