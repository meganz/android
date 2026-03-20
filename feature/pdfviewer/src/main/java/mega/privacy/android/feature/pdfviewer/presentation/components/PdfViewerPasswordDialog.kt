package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.PasswordInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Password dialog for opening password-protected PDF documents.
 *
 * @param password Current password input value
 * @param errorText Error message to show (e.g. invalid password)
 * @param onPasswordChange Called when the user edits the password field
 * @param onConfirm Called when the user taps Continue
 * @param onDismiss Called when the dialog is dismissed (Cancel or outside tap)
 * @param modifier Modifier for the dialog
 */
@Composable
fun PdfViewerPasswordDialog(
    password: String,
    errorText: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PasswordInputDialog(
        modifier = modifier.testTag(PDF_VIEWER_PASSWORD_DIALOG_TAG),
        title = stringResource(sharedR.string.pdf_viewer_dialog_title_enter_password),
        description = stringResource(sharedR.string.pdf_viewer_dialog_text_enter_password),
        positiveButtonText = stringResource(sharedR.string.button_continue),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        inputValue = password,
        onValueChange = onPasswordChange,
        errorText = errorText,
    )
}

@CombinedThemePreviews
@Composable
private fun PdfViewerPasswordDialogPreview() {
    AndroidThemeForPreviews {
        PdfViewerPasswordDialog(
            password = "",
            errorText = null,
            onPasswordChange = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PdfViewerPasswordDialogWithErrorPreview() {
    AndroidThemeForPreviews {
        PdfViewerPasswordDialog(
            password = "wrong",
            errorText = stringResource(sharedR.string.pdf_viewer_dialog_error_incorrect_password),
            onPasswordChange = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val PDF_VIEWER_PASSWORD_DIALOG_TAG = "pdf_viewer_password_dialog"
