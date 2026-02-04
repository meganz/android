package mega.privacy.android.feature.clouddrive.presentation.rubbishbin.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * M3 Compose dialog for clearing rubbish bin
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onClearRubbishBin Callback when user confirms clearing the rubbish bin
 */
@Composable
fun ClearRubbishBinDialog(
    onDismiss: () -> Unit,
    onClearRubbishBin: () -> Unit,
) {
    BasicDialog(
        title = stringResource(sharedR.string.empty_rubbish_bin_menu),
        description = stringResource(sharedR.string.remove_all_rubbish_bin_confirmation),
        positiveButtonText = stringResource(sharedR.string.general_clear),
        onPositiveButtonClicked = {
            onDismiss()
            onClearRubbishBin()
        },
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun ClearRubbishBinDialogPreview() {
    AndroidThemeForPreviews {
        ClearRubbishBinDialog(
            onDismiss = {},
            onClearRubbishBin = {},
        )
    }
}
