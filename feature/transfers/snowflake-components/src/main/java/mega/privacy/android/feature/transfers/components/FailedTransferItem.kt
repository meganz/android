package mega.privacy.android.feature.transfers.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R

/**
 * Core component for a failed or canceled transfer item.
 */
@Composable
fun FailedTransferItem(
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    error: String?,
    isSelected: Boolean?,
    modifier: Modifier = Modifier,
    onMoreClicked: () -> Unit = {},
) = CompletedTransferItem(
    isDownload = isDownload,
    fileTypeResId = fileTypeResId,
    previewUri = previewUri,
    fileName = fileName,
    location = null,
    sizeString = null,
    date = null,
    error = error,
    isSelected = isSelected,
    modifier = modifier,
    onMoreClicked = onMoreClicked
)

@CombinedThemePreviews
@Composable
private fun FailedTransferItemPreview(
    @PreviewParameter(FailedTransferItemProvider::class) completedTransferUI: CompletedTransferUI,
) {
    AndroidThemeForPreviews {
        with(completedTransferUI) {
            FailedTransferItem(
                isDownload = isDownload,
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                fileName = fileName,
                error = error,
                isSelected = isSelected,
            )
        }
    }
}

private class FailedTransferItemProvider : PreviewParameterProvider<CompletedTransferUI> {
    private val name = "File name.pdf"
    private val error = "Failed"
    private val sizeString = "7 MB"
    private val date = "10 Aug 2024 19:09"

    override val values = listOf(
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = error,
            sizeString = sizeString,
            date = date,
        ),
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = null,
            sizeString = sizeString,
            date = date,
        ),
        CompletedTransferUI(
            isDownload = false,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = error,
            sizeString = sizeString,
            date = date,
        ),
        CompletedTransferUI(
            isDownload = false,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = null,
            sizeString = sizeString,
            date = date,
            isSelected = true,
        )
    ).asSequence()
}