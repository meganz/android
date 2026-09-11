package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Error state shown in place of the document when the PDF cannot be loaded.
 *
 * The copy is deliberately fixed and generic: a failure's specific cause is carried by a dialog
 * layered on top of this screen, never by this text.
 *
 * @param onRetry Invoked when the user asks to load the document again
 * @param modifier Modifier for the content root
 */
@Composable
internal fun PdfViewerErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyStateView(
        imagePainter = painterResource(id = iconPackR.drawable.glass_error_circle),
        title = stringResource(sharedR.string.file_preview_error_title),
        modifier = modifier.testTag(PDF_VIEWER_ERROR_CONTENT_TAG),
        description = SpannableText(stringResource(sharedR.string.file_preview_error_description)),
        primaryAction = {
            SecondaryFilledButton(
                modifier = Modifier.testTag(PDF_VIEWER_ERROR_RETRY_BUTTON_TAG),
                text = stringResource(sharedR.string.general_try_again),
                onClick = onRetry,
            )
        },
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewPdfViewerErrorContent() {
    AndroidThemeForPreviews {
        PdfViewerErrorContent(onRetry = {})
    }
}

internal const val PDF_VIEWER_ERROR_CONTENT_TAG = "pdf_viewer:error_content"
internal const val PDF_VIEWER_ERROR_RETRY_BUTTON_TAG = "pdf_viewer:error_retry_button"
