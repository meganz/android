package mega.privacy.android.feature.transfers.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Core component for a completed transfer item.
 */
@Composable
fun CompletedTransferItem(
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    location: String,
    sizeString: String?,
    date: String?,
    isSelected: Boolean?,
    modifier: Modifier = Modifier,
    onMoreClicked: () -> Unit = {},
) = CompletedTransferItem(
    isDownload = isDownload,
    fileTypeResId = fileTypeResId,
    previewUri = previewUri,
    fileName = fileName,
    location = location,
    sizeString = sizeString,
    date = date,
    error = null,
    modifier = modifier,
    isSelected = isSelected,
    onMoreClicked = onMoreClicked
)

@Composable
internal fun CompletedTransferItem(
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    location: String?,
    sizeString: String?,
    date: String?,
    error: String?,
    modifier: Modifier = Modifier,
    isSelected: Boolean? = null,
    onMoreClicked: () -> Unit = {},
) = Box {
    Row(
        modifier = modifier
            .testTag(TEST_TAG_COMPLETED_TRANSFER_ITEM)
            .heightIn(min = 68.dp)
            .padding(vertical = 12.dp)
            .padding(start = 12.dp, end = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val info = if (location != null) {
            joinInfoText(sizeString, date)
        } else {
            error ?: stringResource(id = sharedR.string.transfers_section_cancelled)
        }
        TransferImage(
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE),
        )
        Column(
            Modifier
                .padding(start = 12.dp, end = 8.dp)
                .weight(1f)
        ) {
            MegaText(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.W400),
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_NAME),
            )
            if (location != null) {
                MegaText(
                    text = location,
                    style = AppTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.StartEllipsis,
                    textColor = if (error != null) TextColor.Error else TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_RESULT),
                )
            }
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LeadingIndicator(
                    modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_TYPE_ICON),
                    isDownload = isDownload,
                    isError = error != null,
                )
                MegaText(
                    text = info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.bodySmall,
                    textColor = if (error != null) TextColor.Error else TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_RESULT),
                )
            }
        }
        if (isSelected == null) {
            IconButton(
                onClick = onMoreClicked,
                modifier = Modifier.size(24.dp)
            ) {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                    contentDescription = "",
                    tint = IconColor.Secondary,
                )
            }
        } else {
            SelectedTransferIcon(isSelected)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CompletedTransferItemPreview(
    @PreviewParameter(CompletedTransferItemProvider::class) completedTransferUI: CompletedTransferUI,
) {
    AndroidThemeForPreviews {
        with(completedTransferUI) {
            CompletedTransferItem(
                isDownload = isDownload,
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                fileName = fileName,
                location = location ?: "Cloud Drive",
                sizeString = sizeString,
                date = date,
                isSelected = isSelected,
            )
        }
    }
}

internal data class CompletedTransferUI(
    val isDownload: Boolean,
    val fileTypeResId: Int?,
    val previewUri: Uri?,
    val fileName: String,
    val location: String?,
    val error: String?,
    val sizeString: String?,
    val date: String?,
    val isSelected: Boolean? = null,
)

private class CompletedTransferItemProvider : PreviewParameterProvider<CompletedTransferUI> {
    private val name = "File name.pdf"
    private val sizeString = "7 MB"
    private val date = "10 Aug 2024 19:09"

    override val values = listOf(
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = "/storage/emulated/0/Downloads",
            error = null,
            sizeString = sizeString,
            date = date,
            isSelected = null,
        ),
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = "Cloud Drive",
            error = null,
            sizeString = sizeString,
            date = date,
            isSelected = false,
        ),
        CompletedTransferUI(
            isDownload = false,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = "Cloud Drive",
            error = null,
            sizeString = sizeString,
            date = date,
            isSelected = true,
        ),
    ).asSequence()
}

internal fun joinInfoText(sizeString: String?, date: String?) =
    listOfNotNull(sizeString, date).joinToString(" · ")

/**
 * Tag for the completed transfer item.
 */
const val TEST_TAG_COMPLETED_TRANSFER_ITEM =
    "transfers_view:tab_completed:transfer_item"

/**
 * Tag for the completed transfer image.
 */
const val TEST_TAG_COMPLETED_TRANSFER_IMAGE =
    "transfers_view:tab_completed:transfer_image"

/**
 * Tag for the completed transfer name.
 */
const val TEST_TAG_COMPLETED_TRANSFER_NAME =
    "transfers_view:tab_completed:transfer_name"

/**
 * Tag for the completed transfer speed.
 */
const val TEST_TAG_COMPLETED_TRANSFER_RESULT =
    "transfers_view:tab_completed:transfer_result"
