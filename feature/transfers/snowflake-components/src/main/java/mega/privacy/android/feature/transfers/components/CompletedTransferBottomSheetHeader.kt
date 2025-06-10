package mega.privacy.android.feature.transfers.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R

/**
 * Component for a completed transfer bottom sheet header.
 */
@Composable
fun CompletedTransferBottomSheetHeader(
    fileName: String,
    info: String,
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Row(
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_COMPLETED_TRANSFER_SHEET_HEADER)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.x16)
    ) {
        TransferImage(
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE),
        )
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f),
        ) {
            MegaText(
                text = fileName,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_NAME),
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LeadingIndicator(
                    modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_ICON_TYPE),
                    isDownload = isDownload,
                    isError = isError,
                )
                MegaText(
                    text = info,
                    textColor = if (isError) TextColor.Error else TextColor.Secondary,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CompletedTransferBottomSheetHeaderPreview(
    @PreviewParameter(CompletedTransferBottomSheetHeaderProvider::class) completedTransferHeaderUI: CompletedTransferHeaderUI,
) {
    AndroidThemeForPreviews {
        with(completedTransferHeaderUI) {
            CompletedTransferBottomSheetHeader(
                fileName = fileName,
                info = info,
                isDownload = isDownload,
                isError = isError,
                fileTypeResId = fileTypeResId,
                previewUri = null,
            )
        }
    }
}

internal data class CompletedTransferHeaderUI(
    val isDownload: Boolean,
    val isError: Boolean,
    val fileTypeResId: Int?,
    val previewUri: Uri?,
    val fileName: String,
    val info: String,
)

private class CompletedTransferBottomSheetHeaderProvider :
    PreviewParameterProvider<CompletedTransferHeaderUI> {
    private val name = "File name.pdf"

    override val values = listOf(
        CompletedTransferHeaderUI(
            isDownload = true,
            isError = false,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            info = "/storage/emulated/0/Downloads",
        ),
        CompletedTransferHeaderUI(
            isDownload = false,
            isError = false,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            info = "Cloud Drive",
        ),
        CompletedTransferHeaderUI(
            isDownload = true,
            isError = true,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            info = "Failed",
        ),
        CompletedTransferHeaderUI(
            isDownload = false,
            isError = false,
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            info = "Cancelled",
        ),
    ).asSequence()
}

/**
 * Tag for the completed transfer item sheet header.
 */
const val TEST_TAG_COMPLETED_TRANSFER_SHEET_HEADER =
    "transfers_view:tab_completed:transfer_item_sheet_header"

const val TEST_TAG_COMPLETED_TRANSFER_ICON_TYPE =
    "$TEST_TAG_COMPLETED_TRANSFER_SHEET_HEADER:icon_type"
