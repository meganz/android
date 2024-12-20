package mega.privacy.android.app.presentation.documentscanner.dialogs

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A Composable Dialog shown when the User attempts to back away from the Save Scanned Document Screen
 *
 * @param canSelectScanFileType true if the User can select either scan type due to having only
 * one scan
 * @param onWarningAcknowledged Lambda when the User wants to leave the screen
 * @param onWarningDismissed Lambda when the User does not want to leave the screen
 */
@Composable
internal fun ExitSaveScannedDocumentsScreenWarningDialog(
    canSelectScanFileType: Boolean,
    onWarningAcknowledged: () -> Unit,
    onWarningDismissed: () -> Unit,
) {
    MegaAlertDialog(
        modifier = Modifier.testTag(EXIT_SAVE_SCANNED_DOCUMENTS_SCREEN_WARNING_DIALOG),
        title = stringResource(
            if (canSelectScanFileType) {
                R.string.scan_dialog_discard_title
            } else {
                R.string.scan_dialog_discard_all_title
            }
        ),
        text = stringResource(
            if (canSelectScanFileType) {
                R.string.scan_dialog_discard_body
            } else {
                R.string.scan_dialog_discard_all_body
            }
        ),
        confirmButtonText = stringResource(R.string.scan_dialog_discard_action),
        cancelButtonText = stringResource(SharedR.string.general_dialog_cancel_button),
        onConfirm = onWarningAcknowledged,
        onDismiss = onWarningDismissed,
    )
}

/**
 * A Preview Composable for [ExitSaveScannedDocumentsScreenWarningDialog]
 *
 * @param canSelectScanFileType true if the User can select either scan type due to having only
 * one scan
 */
@CombinedThemePreviews
@Composable
private fun ExitSaveScannedDocumentsScreenWarningDialogPreview(
    @PreviewParameter(BooleanProvider::class) canSelectScanFileType: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ExitSaveScannedDocumentsScreenWarningDialog(
            canSelectScanFileType = canSelectScanFileType,
            onWarningAcknowledged = {},
            onWarningDismissed = {},
        )
    }
}

internal const val EXIT_SAVE_SCANNED_DOCUMENTS_SCREEN_WARNING_DIALOG =
    "exit_save_scanned_documents_screen_warning_dialog:mega_alert_dialog"