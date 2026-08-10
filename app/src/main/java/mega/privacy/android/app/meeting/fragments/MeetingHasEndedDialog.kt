package mega.privacy.android.app.meeting.fragments

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Compose replacement for `MeetingHasEndedDialogFragment`.
 *
 * Renders [MegaAlertDialog] with the meeting-ended message. For the moderator
 * variant ([isFromGuest] = false) it exposes both a leave action and a "view
 * meeting chat" action; for the guest variant it shows only the dismiss action.
 *
 * @param isFromGuest true when the caller is a guest joining via link.
 * @param onLeave Invoked when the user dismisses without viewing the chat.
 * @param onViewMeetingChat Invoked when the user chooses to view the meeting chat. Unused when [isFromGuest] is true.
 * @param onDismissRequest Called whenever the dialog should be removed (back press, outside tap, or after either button).
 */
@Composable
fun MeetingHasEndedDialog(
    isFromGuest: Boolean,
    onLeave: () -> Unit,
    onViewMeetingChat: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    MegaAlertDialog(
        text = stringResource(sharedR.string.meeting_has_ended_dialog_title),
        confirmButtonText = if (isFromGuest) {
            stringResource(sharedR.string.general_ok)
        } else {
            stringResource(sharedR.string.general_dialog_cancel_button)
        },
        cancelButtonText = if (isFromGuest) {
            null
        } else {
            stringResource(sharedR.string.meeting_has_ended_dialog_view_chat_option)
        },
        onConfirm = {
            onLeave()
            onDismissRequest()
        },
        onCancel = {
            onViewMeetingChat()
            onDismissRequest()
        },
        onDismiss = onDismissRequest,
        dismissOnClickOutside = false,
    )
}

@CombinedThemeComponentPreviews
@Composable
@Preview
private fun MeetingHasEndedDialogGuestPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MeetingHasEndedDialog(
            isFromGuest = true,
            onLeave = {},
            onViewMeetingChat = {},
            onDismissRequest = {},
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
@Preview
private fun MeetingHasEndedDialogModeratorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MeetingHasEndedDialog(
            isFromGuest = false,
            onLeave = {},
            onViewMeetingChat = {},
            onDismissRequest = {},
        )
    }
}
