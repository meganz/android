package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Maps [PdfViewerError] and network state to the dialog body text shown in [PdfViewerErrorDialog].
 * PasswordProtected and InvalidPassword belongs on [PdfViewerPasswordDialog], not here.
 * Handled here as fallback only.
 *
 * @param error The current error
 * @param isOnline Whether the device has network
 * @return Localised string to display in the error dialog
 */
@Composable
private fun pdfViewerErrorMessage(
    error: PdfViewerError,
    isOnline: Boolean,
): String = when (error) {
    is PdfViewerError.LoadError ->
        if (isOnline) stringResource(sharedR.string.pdf_viewer_error_corrupt_or_missing)
        else stringResource(sharedR.string.pdf_viewer_error_no_network)

    is PdfViewerError.NetworkError ->
        stringResource(sharedR.string.pdf_viewer_error_no_network)

    is PdfViewerError.FileNotFound ->
        stringResource(sharedR.string.general_error_file_not_found)

    is PdfViewerError.StreamingError ->
        stringResource(sharedR.string.pdf_viewer_error_streaming)

    is PdfViewerError.PasswordProtected,
    is PdfViewerError.InvalidPassword
        -> {
        // Note: Handled as fallback per documentation
        stringResource(sharedR.string.pdf_viewer_dialog_error_incorrect_password)
    }

    is PdfViewerError.Generic ->
        stringResource(sharedR.string.general_request_failed_message)
}

/**
 * Error dialog for the PDF viewer.
 *
 * @param error The error to display
 * @param isOnline Whether the device has network.
 * @param onDismiss Invoked for OK and any dialog dismiss action
 * @param modifier Modifier for the dialog root
 */
@Composable
internal fun PdfViewerErrorDialog(
    error: PdfViewerError,
    isOnline: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = pdfViewerErrorMessage(error = error, isOnline = isOnline)
    val okLabel = stringResource(sharedR.string.general_ok_only)

    BasicDialog(
        modifier = modifier.testTag(PDF_VIEWER_ERROR_DIALOG_TAG),
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismiss = onDismiss,
        description = message,
        positiveButtonText = okLabel,
        onPositiveButtonClicked = onDismiss,
    )
}

private class PdfErrorPreviewProvider : PreviewParameterProvider<Pair<PdfViewerError, Boolean>> {
    override val values = sequenceOf(
        PdfViewerError.LoadError(null) to true,
        PdfViewerError.LoadError(null) to false,
        PdfViewerError.FileNotFound to false,
        PdfViewerError.StreamingError(null) to false,
        PdfViewerError.Generic(Throwable("generic error demo")) to false,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewPdfViewerErrorDialog(
    @PreviewParameter(PdfErrorPreviewProvider::class) params: Pair<PdfViewerError, Boolean>,
) {
    val (error, isOnline) = params
    AndroidThemeForPreviews {
        PdfViewerErrorDialog(
            error = error,
            isOnline = isOnline,
            onDismiss = {},
        )
    }
}

internal const val PDF_VIEWER_ERROR_DIALOG_TAG = "pdf_viewer:error_dialog"
