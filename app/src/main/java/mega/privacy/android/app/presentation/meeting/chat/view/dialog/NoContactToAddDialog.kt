package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * The dialog to show when there is no contact to add into a chat.
 */
@Composable
fun NoContactToAddDialog(
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.chat_add_participants_no_contacts_title),
        text = stringResource(id = R.string.chat_add_participants_no_contacts_message),
        cancelButtonText = stringResource(id = R.string.button_cancel),
        confirmButtonText = stringResource(id = R.string.contact_invite),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@CombinedThemePreviews
@Composable
private fun NoContactToAddDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        NoContactToAddDialog()
    }
}