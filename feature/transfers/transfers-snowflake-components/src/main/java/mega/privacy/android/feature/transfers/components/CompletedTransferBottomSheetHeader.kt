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
    size: String,
    date: String,
    fileTypeResId: Int?,
    previewUri: Uri?,
    modifier: Modifier = Modifier,
) = CompletedTransferBottomSheetHeader(
    fileName = fileName,
    info1 = size,
    info2 = date,
    fileTypeResId = fileTypeResId,
    previewUri = previewUri,
    modifier = modifier,
    isError = false
)

@Composable
internal fun CompletedTransferBottomSheetHeader(
    fileName: String,
    info1: String,
    info2: String?,
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
            MegaText(
                text = joinInfoText(info1, info2),
                textColor = if (isError) TextColor.Error else TextColor.Secondary,
                style = AppTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CompletedTransferBottomSheetHeaderPreview() {
    AndroidThemeForPreviews {
        with(CompletedTransferHeaderUI.default) {
            CompletedTransferBottomSheetHeader(
                fileName = fileName,
                size = size,
                date = date,
                fileTypeResId = fileTypeResId,
                previewUri = null,
            )
        }
    }
}

internal data class CompletedTransferHeaderUI(
    val fileTypeResId: Int?,
    val previewUri: Uri?,
    val fileName: String,
    val size: String,
    val date: String,
) {
    companion object {
        val default = CompletedTransferHeaderUI(
            fileTypeResId = R.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = "File name.pdf",
            size = "10MB",
            date = "10 Aug 2024 19:09",
        )
    }
}

/**
 * Tag for the completed transfer item sheet header.
 */
internal const val TEST_TAG_COMPLETED_TRANSFER_SHEET_HEADER =
    "transfers_view:tab_completed:transfer_item_sheet_header"
