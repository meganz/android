package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * The dialog to show when it is trying to delete messages.
 */
@Composable
fun DeleteMessagesConfirmationDialog(
    messagesCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) = ConfirmationDialog(
    title = stringResource(
        id = if (messagesCount == 1) R.string.confirmation_delete_one_message
        else R.string.confirmation_delete_several_messages
    ),
    text = "",
    cancelButtonText = stringResource(id = R.string.button_cancel),
    confirmButtonText = stringResource(id = R.string.context_remove),
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    modifier = Modifier.testTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG)
)

@CombinedTextAndThemePreviews
@Composable
private fun DeleteMessagesConfirmationDialogPreview(
    @PreviewParameter(BooleanProvider::class) isMeeting: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ClearChatConfirmationDialog(
            isMeeting = isMeeting,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

internal const val TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG =
    "chat_view:dialog_chat_remove_messages_confirmation"