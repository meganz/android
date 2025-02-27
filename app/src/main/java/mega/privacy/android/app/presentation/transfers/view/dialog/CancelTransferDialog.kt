package mega.privacy.android.app.presentation.transfers.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Cancel transfer dialog.
 *
 */
@Composable
fun CancelTransferDialog(
    title: String,
    onCancelTransfer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialog(
    modifier = modifier.testTag(TEST_TAG_CANCEL_TRANSFER_DIALOG),
    title = title,
    text = pluralStringResource(id = R.plurals.cancel_selected_transfers, 1),
    confirmButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
    onConfirm = onCancelTransfer,
    cancelButtonText = stringResource(id = R.string.general_dismiss),
    onDismiss = onDismiss,
)

@CombinedThemePreviews
@Composable
private fun CancelTransferDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CancelTransferDialog(title = "Cancel transfer?", onCancelTransfer = {}, onDismiss = {})
    }
}

internal const val TEST_TAG_CANCEL_TRANSFER_DIALOG =
    "transfers_view:cancel_transfer_dialog"