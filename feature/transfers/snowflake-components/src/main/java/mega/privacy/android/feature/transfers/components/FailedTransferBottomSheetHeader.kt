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
 * Component for a completed transfer bottom sheet header.
 */
@Composable
fun FailedTransferBottomSheetHeader(
    fileName: String,
    info: String,
    fileTypeResId: Int?,
    previewUri: Uri?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) = CompletedTransferBottomSheetHeader(
    fileName = fileName,
    info1 = info,
    info2 = null,
    fileTypeResId = fileTypeResId,
    previewUri = previewUri,
    modifier = modifier,
    isError = isError
)

@CombinedThemePreviews
@Composable
private fun FailedTransferBottomSheetHeaderPreview(
    @PreviewParameter(FailedTransferBottomSheetHeaderProvider::class) completedTransferHeaderUI: FailedTransferHeaderUI,
) {
    AndroidThemeForPreviews {
        with(completedTransferHeaderUI) {
            FailedTransferBottomSheetHeader(
                fileName = fileName,
                info = info,
                isError = isError,
                fileTypeResId = fileTypeResId,
                previewUri = null,
            )
        }
    }
}

internal data class FailedTransferHeaderUI(
    val isError: Boolean,
    val fileTypeResId: Int?,
    val previewUri: Uri?,
    val fileName: String,
    val info: String,
)

private class FailedTransferBottomSheetHeaderProvider :
    PreviewParameterProvider<FailedTransferHeaderUI> {
    private val name = "File name.pdf"

    override val values = listOf(
        FailedTransferHeaderUI(
            isError = true,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            info = "Failed: write error"
        ),
        FailedTransferHeaderUI(
            isError = false,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            info = "Cancelled",
        )
    ).asSequence()
}