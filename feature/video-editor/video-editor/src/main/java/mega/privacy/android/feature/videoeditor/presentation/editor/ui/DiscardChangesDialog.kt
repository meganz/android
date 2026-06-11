package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Confirmation shown when the user tries to leave the editor with unsaved edits
 *
 * @param onDiscard leaves the editor, abandoning the current edits.
 * @param onDismiss keeps editing (Cancel button, scrim tap, or back press).
 */
@Composable
internal fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier,
        title = stringResource(sharedR.string.general_dialog_title_discard_changes),
        description = "If you leave now, your edits will be lost.", // TODO string resource
        positiveButtonText = stringResource(sharedR.string.general_dialog_discard_button),
        onPositiveButtonClicked = onDiscard,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun DiscardChangesDialogPreview() {
    AndroidThemeForPreviews {
        DiscardChangesDialog(
            onDiscard = {},
            onDismiss = {},
        )
    }
}
