package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
internal fun ParticipatingInACallDialog(onDismiss: () -> Unit = {}, onConfirm: () -> Unit = {}) {
    MegaAlertDialog(
        text = stringResource(id = R.string.ongoing_call_content),
        confirmButtonText = stringResource(id = R.string.general_ok),
        onDismiss = onDismiss,
        cancelButtonText = null,
        onConfirm = onConfirm,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewParticipatingInACallDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ParticipatingInACallDialog()
    }
}