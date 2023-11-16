package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog

/**
 * The dialog to show when it is trying to clear the chat history.
 */
@Composable
fun ClearChatConfirmationDialog(
    isMeeting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) = ConfirmationDialog(
    title = stringResource(
        id = if (isMeeting) R.string.meetings_clear_history_confirmation_dialog_title
        else R.string.title_properties_chat_clear
    ),
    text = stringResource(
        id = if (isMeeting) R.string.meetings_clear_history_confirmation_dialog_message
        else R.string.confirmation_clear_chat_history
    ),
    cancelButtonText = stringResource(id = R.string.button_cancel),
    confirmButtonText = stringResource(id = R.string.general_clear),
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    modifier = Modifier.testTag(TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG)
)

internal const val TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG = "chat_view:dialog_chat_clear:history"