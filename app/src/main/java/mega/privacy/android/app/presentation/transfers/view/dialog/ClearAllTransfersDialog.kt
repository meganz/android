package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_TRANSFERS_VIEW
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * All contacts added dialog
 *
 */
@Composable
fun ClearAllTransfersDialog(
    onClearAllTransfers: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialog(
    modifier = modifier.testTag(TEST_TAG_CLEAR_ALL_TRANSFERS_DIALOG),
    text = stringResource(id = R.string.option_to_clear_transfers),
    confirmButtonText = stringResource(id = R.string.general_clear),
    onDismiss = onDismiss,
    cancelButtonText = stringResource(id = R.string.general_dismiss),
    onConfirm = {
        onClearAllTransfers()
        onDismiss()
    },
)

@CombinedThemePreviews
@Composable
private fun ClearAllTransfersDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ClearAllTransfersDialog(
            onClearAllTransfers = {}, onDismiss = {})
    }
}

internal const val TEST_TAG_CLEAR_ALL_TRANSFERS_DIALOG =
    "$TEST_TAG_TRANSFERS_VIEW:clear_all_transfers_dialog"