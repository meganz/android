package mega.privacy.android.app.presentation.node.dialogs.cannotopenfile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

internal const val TEST_TAG_CANNOT_OPEN_FILE_DIALOG_TAG = "cannot_open_file_dialog_tag"

@Composable
internal fun CannotOpenFileDialog(
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    ConfirmationDialog(
        modifier = Modifier.testTag(TEST_TAG_CANNOT_OPEN_FILE_DIALOG_TAG),
        title = stringResource(id = R.string.dialog_cannot_open_file_title),
        text = stringResource(id = R.string.dialog_cannot_open_file_text),
        confirmButtonText = stringResource(id = R.string.context_download),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = {
            onDismiss()
            onDownload()
        },
        onDismiss = { onDismiss() },
    )
}

@Composable
@CombinedTextAndThemePreviews
private fun CannotOpenFileDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CannotOpenFileDialog(onDismiss = { }, onDownload = { })
    }
}