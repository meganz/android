package mega.privacy.android.shared.original.core.ui.controls.transfers

import mega.privacy.android.icon.pack.R as iconPackR
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Core component for a completed transfer item.
 */
@Composable
fun CompletedTransferItem(
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    location: String?,
    error: String?,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .testTag(TEST_TAG_COMPLETED_TRANSFER_ITEM)
        .height(72.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    TransferImage(
        isDownload = isDownload,
        fileTypeResId = fileTypeResId,
        previewUri = previewUri,
        modifier = Modifier
            .testTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE)
            .padding(start = 12.dp),
    )
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .weight(1f)
    ) {
        MegaText(
            text = fileName,
            style = MaterialTheme.typography.subtitle1,
            textColor = TextColor.Primary,
            modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_NAME),
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            location?.let {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON),
                    painter = painterResource(id = iconPackR.drawable.ic_check_circle_medium_regular_outline),
                    contentDescription = null,
                    tint = MegaOriginalTheme.colors.support.success,
                )
            }
            MegaText(
                text = location ?: error ?: stringResource(id = R.string.transfer_cancelled),
                style = MaterialTheme.typography.subtitle2,
                textColor = if (error != null) TextColor.Error else TextColor.Secondary,
                modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER_RESULT),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CompletedTransferItemPreview(
    @PreviewParameter(CompletedTransferItemProvider::class) completedTransferUI: CompletedTransferUI,
) {
    val isDark = isSystemInDarkTheme()
    OriginalTempTheme(isDark = isDark) {
        with(completedTransferUI) {
            CompletedTransferItem(
                isDownload = isDownload,
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                fileName = fileName,
                location = location,
                error = error,
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
)

private class CompletedTransferItemProvider : PreviewParameterProvider<CompletedTransferUI> {
    private val name = "File name.pdf"
    private val error = "Failed"

    override val values = listOf(
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = "/storage/emulated/0/Downloads",
            error = null,
        ),
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = error,
        ),
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = null,
        ),
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = "Cloud Drive",
            error = null,
        ),
        CompletedTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = error,
        ),
        CompletedTransferUI(
            isDownload = false,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            location = null,
            error = null,
        )
    ).asSequence()
}

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

/**
 * Tag for the completed transfer queued icon.
 */
const val TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON =
    "transfers_view:tab_completed:transfer_success_icon"
