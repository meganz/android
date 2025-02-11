package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun CanNotOpenFileDialog(
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    MegaAlertDialog(
        title = stringResource(id = R.string.dialog_cannot_open_file_title),
        text = stringResource(id = R.string.dialog_cannot_open_file_text),
        confirmButtonText = stringResource(id = R.string.context_download),
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onConfirm = onDownloadClick,
        onDismiss = onDismiss
    )
}

@CombinedThemePreviews
@Composable
private fun CanNotOpenFileDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CanNotOpenFileDialog({}, {})
    }
}