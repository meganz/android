package mega.privacy.android.feature.chat.managehistory.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Confirms that every message in the conversation is about to be deleted.
 *
 * @param isMeeting True when the conversation is a meeting, which changes the wording.
 * @param onConfirm Called when the user confirms the deletion.
 * @param onDismiss Called when the dialog is dismissed without deleting anything.
 * @param modifier [Modifier]
 */
@Composable
internal fun ClearChatHistoryDialog(
    isMeeting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier.testTag(CLEAR_CHAT_HISTORY_DIALOG_TAG),
        title = if (isMeeting) {
            stringResource(sharedR.string.chat_manage_history_clear_meeting_confirmation_title)
        } else {
            stringResource(sharedR.string.title_properties_chat_clear)
        },
        description = if (isMeeting) {
            stringResource(sharedR.string.chat_manage_history_clear_meeting_confirmation_message)
        } else {
            stringResource(sharedR.string.confirmation_clear_chat_history)
        },
        positiveButtonText = stringResource(sharedR.string.general_clear),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

internal const val CLEAR_CHAT_HISTORY_DIALOG_TAG = "clear_chat_history_dialog:dialog"

@CombinedThemePreviews
@Composable
private fun ClearChatHistoryDialogPreview() {
    AndroidThemeForPreviews {
        ClearChatHistoryDialog(
            isMeeting = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
