package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * All contacts added dialog
 *
 */
@Composable
fun CancelAllTransfersDialog(
    onCancelAllTransfers: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialog(
    modifier = modifier.testTag(TEST_TAG_CANCEL_ALL_TRANSFERS_DIALOG),
    text = stringResource(id = R.string.cancel_all_transfer_confirmation),
    confirmButtonText = stringResource(id = R.string.cancel_all_action),
    onDismiss = onDismiss,
    cancelButtonText = stringResource(id = R.string.general_dismiss),
    onConfirm = {
        onCancelAllTransfers()
        onDismiss()
    },
)

@CombinedThemePreviews
@Composable
private fun CancelAllTransfersDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CancelAllTransfersDialog(
            onCancelAllTransfers = {}, onDismiss = {})
    }
}

internal const val TEST_TAG_CANCEL_ALL_TRANSFERS_DIALOG =
    "transfers_view:cancel_all_transfers_dialog"