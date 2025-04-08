package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Cancel transfer dialog.
 *
 */
@Composable
fun CancelPreviewDownloadDialog(
    onCancelTransfer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialog(
    modifier = modifier.testTag(TEST_TAG_CANCEL_PREVIEW_DOWNLOAD_DIALOG),
    title = stringResource(sharedR.string.transfers_cancel_preview_download_warning_title),
    text = stringResource(sharedR.string.transfers_cancel_preview_download_warning_text),
    confirmButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
    onConfirm = onCancelTransfer,
    cancelButtonText = stringResource(id = R.string.general_dismiss),
    onDismiss = onDismiss,
)

@CombinedThemePreviews
@Composable
private fun CancelTransferDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CancelPreviewDownloadDialog(
            onCancelTransfer = {},
            onDismiss = {})
    }
}

internal const val TEST_TAG_CANCEL_PREVIEW_DOWNLOAD_DIALOG =
    "transfers_view:cancel_preview_download_dialog"