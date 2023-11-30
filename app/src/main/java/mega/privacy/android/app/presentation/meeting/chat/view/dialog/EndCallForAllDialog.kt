package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

@Composable
internal fun EndCallForAllDialog(onDismiss: () -> Unit = {}, onConfirm: () -> Unit = {}) {
    ConfirmationDialog(
        title = stringResource(id = R.string.meetings_chat_screen_dialog_title_end_call_for_all),
        text = stringResource(id = R.string.meetings_chat_screen_dialog_description_end_call_for_all),
        confirmButtonText = stringResource(id = R.string.meetings_chat_screen_dialog_positive_button_end_call_for_all),
        cancelButtonText = stringResource(id = R.string.meetings_chat_screen_dialog_negative_button_end_call_for_all),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewEndCallForAllDialog() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        EndCallForAllDialog()
    }
}