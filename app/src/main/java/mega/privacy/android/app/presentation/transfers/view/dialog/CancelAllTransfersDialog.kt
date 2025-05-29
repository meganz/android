package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * All contacts added dialog
 *
 */
@Composable
fun CancelAllTransfersDialog(
    onCancelAllTransfers: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = BasicDialog(
    modifier = modifier.testTag(TEST_TAG_CANCEL_ALL_TRANSFERS_DIALOG),
    title = SpannableText(stringResource(id = R.string.cancel_all_transfer_confirmation)),
    buttons = listOf(
        BasicDialogButton(stringResource(id = R.string.general_dismiss), onClick = onDismiss),
        BasicDialogButton(stringResource(id = R.string.cancel_all_action), onClick = {
            onCancelAllTransfers()
            onDismiss()
        }),
    ).toImmutableList(),
    onDismissRequest = onDismiss,
)

@CombinedThemePreviews
@Composable
private fun CancelAllTransfersDialogPreview() {
    AndroidThemeForPreviews {
        Box(modifier = Modifier.fillMaxSize()) {
            CancelAllTransfersDialog(
                onCancelAllTransfers = {}, onDismiss = {})
        }
    }
}

internal const val TEST_TAG_CANCEL_ALL_TRANSFERS_DIALOG =
    "transfers_view:cancel_all_transfers_dialog"