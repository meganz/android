package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * Dialog to confirm cancel selected transfers
 */
@Composable
fun CancelTransfersConfirmationDialog(
    selectedAmount: Int,
    onCancelTransfers: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        title = stringResource(R.string.option_cancel_transfer),
        description = pluralStringResource(R.plurals.cancel_selected_transfers, selectedAmount),
        buttons = listOf(
            BasicDialogButton(stringResource(id = R.string.general_dismiss), onClick = onDismiss),
            BasicDialogButton(stringResource(id = R.string.button_continue), onClick = {
                onCancelTransfers()
                onDismiss()
            }),
        ).toImmutableList(),
        onDismissRequest = onDismiss,
        modifier = modifier,
    )
}

@CombinedThemePreviews
@Composable
private fun ClearAllTransfersDialogPreview() {
    AndroidThemeForPreviews {
        Box(modifier = Modifier.fillMaxSize()) {
            CancelTransfersConfirmationDialog(
                selectedAmount = 3,
                onCancelTransfers = {},
                onDismiss = {}
            )
        }
    }
}