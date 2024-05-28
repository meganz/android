package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun CanNotOpenFileDialog(
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    MegaAlertDialog(
        title = stringResource(id = R.string.dialog_cannot_open_file_title),
        text = stringResource(id = R.string.dialog_cannot_open_file_text),
        confirmButtonText = stringResource(id = R.string.context_download),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = onDownloadClick,
        onDismiss = onDismiss
    )
}

@CombinedThemePreviews
@Composable
private fun CanNotOpenFileDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CanNotOpenFileDialog({}, {})
    }
}