package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun ParticipatingInACallDialog(onDismiss: () -> Unit = {}, onConfirm: () -> Unit = {}) {
    MegaAlertDialog(
        text = stringResource(id = R.string.ongoing_call_content),
        confirmButtonText = stringResource(id = sharedResR.string.general_ok),
        onDismiss = onDismiss,
        cancelButtonText = null,
        onConfirm = onConfirm,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewParticipatingInACallDialog() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ParticipatingInACallDialog()
    }
}