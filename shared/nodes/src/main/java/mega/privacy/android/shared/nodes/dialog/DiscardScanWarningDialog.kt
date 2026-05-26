package mega.privacy.android.shared.nodes.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog shown when the User no longer wants to proceed with uploading the scanned document/s.
 *
 * @param hasMultipleScans true when more than one scan will be discarded; switches the title and
 * description to the multi-scan variant.
 */
@Composable
fun DiscardScanWarningDialog(
    hasMultipleScans: Boolean,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier.testTag(DISCARD_SCAN_WARNING_DIALOG_TAG),
        title = stringResource(
            if (hasMultipleScans) {
                sharedR.string.scan_dialog_discard_all_confirmation_title
            } else {
                sharedR.string.scan_dialog_discard_confirmation_title
            }
        ),
        description = stringResource(
            if (hasMultipleScans) {
                sharedR.string.scan_dialog_discard_all_confirmation_body
            } else {
                sharedR.string.scan_dialog_discard_confirmation_body
            }
        ),
        positiveButtonText = stringResource(sharedR.string.scan_dialog_discard_confirmation_action),
        onPositiveButtonClicked = onDiscard,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onCancel,
        onDismiss = onCancel,
    )
}

internal const val DISCARD_SCAN_WARNING_DIALOG_TAG = "discard_scan_warning_dialog:basic_dialog"

@CombinedThemePreviews
@Composable
private fun DiscardScanWarningDialogPreview(
    @PreviewParameter(BooleanProvider::class) hasMultipleScans: Boolean,
) {
    AndroidThemeForPreviews {
        DiscardScanWarningDialog(
            hasMultipleScans = hasMultipleScans,
            onDiscard = {},
            onCancel = {},
        )
    }
}
