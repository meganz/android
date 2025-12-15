package mega.privacy.android.feature.chat.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R

/**
 * Dialog to show when the user tries to open a meeting that has ended
 *
 * @param onDismiss dismiss callback of dialog
 */
@Composable
fun MeetingHasEndedDialog(
    doesChatExist: Boolean,
    onDismiss: () -> Unit,
    onShowChat: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(MEETING_HAS_ENDED_DIALOG_TAG),
        description = stringResource(id = R.string.meeting_has_ended_dialog_title),
        positiveButtonText = stringResource(id = if (doesChatExist) R.string.meeting_has_ended_dialog_view_chat_option else R.string.general_ok),
        negativeButtonText = if (doesChatExist) stringResource(R.string.general_dismiss_dialog) else null,
        onPositiveButtonClicked = {
            onDismiss()
            onShowChat()
        },
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        dismissOnClickOutside = false,
    )
}

@CombinedThemePreviews
@Composable
private fun CannotVerifyContactDialogPreview() {
    AndroidThemeForPreviews {
        MeetingHasEndedDialog(doesChatExist = false, {}, {})
    }
}


@CombinedThemePreviews
@Composable
private fun CannotVerifyContactDialogGuestPreview() {
    AndroidThemeForPreviews {
        MeetingHasEndedDialog(doesChatExist = true, {}, {})
    }
}

internal const val MEETING_HAS_ENDED_DIALOG_TAG = "meetingHasEndedDialog"