package mega.privacy.android.app.presentation.documentscanner.dialogs

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.presentation.documentscanner.model.DocumentScanningError
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A Composable Dialog shown when an error occurs during Document Scanning
 *
 * @param documentScanningError The specific Document Scanning Error. If it's value is null, the
 * Dialog is not shown
 * @param onErrorAcknowledged Lambda to execute upon clicking the Button
 * @param onErrorDismissed Lambda to execute upon clicking outside the Dialog bounds
 */
@Composable
internal fun DocumentScanningErrorDialog(
    documentScanningError: DocumentScanningError?,
    onErrorAcknowledged: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    documentScanningError?.let { errorType ->
        MegaAlertDialog(
            modifier = Modifier.testTag(DOCUMENT_SCANNING_ERROR_DIALOG),
            title = stringResource(SharedR.string.document_scanning_error_dialog_title),
            body = stringResource(errorType.textRes),
            confirmButtonText = stringResource(SharedR.string.document_scanning_error_dialog_confirm_button),
            cancelButtonText = null,
            onConfirm = onErrorAcknowledged,
            onDismiss = onErrorDismissed,
        )
    }
}

/**
 * A Preview [Composable] for [DocumentScanningErrorDialog]
 *
 * @param documentScanningError The specific Document Scanning Error
 */
@CombinedThemePreviews
@Composable
private fun DocumentScanningErrorDialogPreview(
    @PreviewParameter(DocumentScanningErrorParameterProvider::class) documentScanningError: DocumentScanningError,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        DocumentScanningErrorDialog(
            documentScanningError = documentScanningError,
            onErrorAcknowledged = {},
            onErrorDismissed = {},
        )
    }
}

private class DocumentScanningErrorParameterProvider :
    PreviewParameterProvider<DocumentScanningError> {
    override val values: Sequence<DocumentScanningError>
        get() = DocumentScanningError.entries.asSequence()
}

/**
 * Test Tag for the Document Scanning Error Dialog
 */
internal const val DOCUMENT_SCANNING_ERROR_DIALOG =
    "document_scanning_error_dialog:mega_alert_dialog"