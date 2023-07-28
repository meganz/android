package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Show confirmation dialog to cancel unique scheduled meeting occurrence
 *
 * @param isChatHistoryEmpty    True if the chat room history is empty (only management messages) or false otherwise
 * @param onConfirm             To be triggered when confirm button is pressed
 * @param onDismiss             To be triggered when dialog is hidden
 */
@Composable
fun CancelOccurrenceAndMeetingDialog(
    isChatHistoryEmpty: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MegaAlertDialog(
        title = stringResource(id = R.string.meetings_cancel_scheduled_meeting_last_occurrence_dialog_title),
        text = stringResource(
            if (isChatHistoryEmpty) {
                R.string.meetings_cancel_scheduled_meeting_last_occurrence_chat_history_empty_dialog_message
            } else {
                R.string.meetings_cancel_scheduled_meeting_last_occurrence_chat_history_not_empty_dialog_message
            }
        ),
        confirmButtonText = stringResource(R.string.meetings_cancel_scheduled_meeting_chat_history_not_empty_dialog_confirm_button),
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
fun PreviewEmptyHistoryCancelOccurrenceAndMeetingDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CancelOccurrenceAndMeetingDialog(
            isChatHistoryEmpty = true,
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
fun PreviewNoEmptyHistoryCancelOccurrenceAndMeetingDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CancelOccurrenceAndMeetingDialog(
            isChatHistoryEmpty = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}