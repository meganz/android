package mega.privacy.android.feature.transfers.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.ProgressBarIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR

@Composable
fun CameraUploadsActiveTransferItem(
    tag: Int,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    progressPercentageString: String,
    progressSizeString: String,
    progress: Float,
    speed: String?,
    modifier: Modifier = Modifier,
    isOverQuota: Boolean = false,
    isDownload: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .fillMaxWidth()
            .testTag(TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM + "_$tag")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val subTitle = listOf(
                progressPercentageString,
                progressSizeString,
                speed,
            ).joinToString(" · ")

            TransferImage(
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_IMAGE),
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
                    modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_NAME),
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LeadingIndicator(
                        modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_TYPE_ICON),
                        isDownload = isDownload,
                        hasIssues = isOverQuota,
                    )
                    MegaText(
                        text = subTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = AppTheme.typography.bodySmall,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_SUBTITLE),
                    )
                }
            }
        }

        ProgressBarIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            progressPercentage = progress * 100f,
            supportColor = if (isOverQuota) SupportColor.Warning else SupportColor.Success
        )
    }
}

@Composable
fun CameraUploadsInQueueTransferItem(
    tag: Int,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    modifier: Modifier = Modifier,
) =
    Row(
        modifier = modifier
            .height(68.dp)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp)
            .testTag(TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM + "_$tag"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransferImage(
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_IMAGE),
        )

        MegaText(
            text = fileName,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.W400),
            textColor = TextColor.Primary,
            modifier = Modifier
                .padding(start = 12.dp, end = 8.dp)
                .weight(1f)
                .testTag(TEST_TAG_ACTIVE_TRANSFER_NAME),
        )
    }

@CombinedThemePreviews
@Composable
private fun CameraUploadsActiveTransferItemPreview() {
    AndroidThemeForPreviews {
        CameraUploadsActiveTransferItem(
            tag = 1,
            fileTypeResId = iconPackR.drawable.ic_video_medium_solid,
            previewUri = null,
            fileName = "Video name.mp4",
            progressSizeString = "6MB of 10MB",
            progressPercentageString = "60%",
            progress = 0.6f,
            speed = "4.2MB/s",
            isOverQuota = false,
            isDownload = false,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsInQueueTransferItemPreview() {
    AndroidThemeForPreviews {
        CameraUploadsInQueueTransferItem(
            tag = 1,
            fileTypeResId = iconPackR.drawable.ic_video_medium_solid,
            previewUri = null,
            fileName = "Video name.mp4"
        )
    }
}

/**
 * Tag for the Camera Uploads active transfer item.
 */
const val TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM =
    "camera_uploads_transfers_view:active_transfer_item"

/**
 * Tag for the Camera Uploads in queue transfer item.
 */
const val TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM =
    "camera_uploads_transfers_view:in_queue_transfer_item"