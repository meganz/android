package mega.privacy.android.shared.original.core.ui.controls.transfers

import mega.privacy.android.icon.pack.R as iconPackR
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
 * Core component for a in-progress transfer item.
 */
@Composable
fun InProgressTransferItem(
    tag: Int,
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    progress: String,
    speed: String?,
    isPaused: Boolean,
    isQueued: Boolean,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .testTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag")
        .height(72.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        painter = painterResource(id = iconPackR.drawable.ic_queue_line_small_regular_outline),
        contentDescription = "Reorder icon",
        tint = MegaOriginalTheme.colors.icon.secondary,
        modifier = Modifier
            .padding(start = 8.dp)
            .testTag(TEST_TAG_QUEUE_ICON)
    )
    TransferImage(
        isDownload = isDownload,
        fileTypeResId = fileTypeResId,
        previewUri = previewUri,
        modifier = Modifier.testTag(TEST_TAG_IN_PROGRESS_TRANSFER_IMAGE),
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
            modifier = Modifier.testTag(TEST_TAG_IN_PROGRESS_TRANSFER_NAME),
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isQueued && areTransfersPaused.not()) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(TEST_TAG_IN_PROGRESS_TRANSFER_QUEUED_ICON),
                    painter = painterResource(id = iconPackR.drawable.ic_circle_big_medium_regular_outline),
                    contentDescription = null,
                    tint = MegaOriginalTheme.colors.support.warning,
                )
            } else {
                MegaText(
                    text = progress,
                    style = MaterialTheme.typography.subtitle2,
                    textColor = when {
                        isOverQuota -> TextColor.Warning
                        isDownload -> TextColor.Success
                        else -> TextColor.Info
                    },
                    modifier = Modifier.testTag(TEST_TAG_IN_PROGRESS_TRANSFER_PROGRESS),
                )
            }
            speed?.let {
                MegaText(
                    text = speed,
                    style = MaterialTheme.typography.subtitle2,
                    textColor = TextColor.Secondary,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .testTag(TEST_TAG_IN_PROGRESS_TRANSFER_SPEED)
                )
            }
        }
    }
    IconButton(
        onClick = onPlayPauseClicked, modifier = Modifier
            .padding(end = 16.dp)
            .testTag(if (isPaused) TEST_TAG_PLAY_ICON else TEST_TAG_PAUSE_ICON)
    ) {
        Icon(
            painter = painterResource(
                id = if (isPaused) iconPackR.drawable.ic_play_medium_regular_outline
                else iconPackR.drawable.ic_pause_medium_regular_outline
            ),
            contentDescription = if (isPaused) stringResource(id = R.string.action_play)
            else stringResource(id = R.string.action_pause),
            tint = MegaOriginalTheme.colors.icon.secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun InProgressTransferItemPreview(
    @PreviewParameter(InProgressTransferItemProvider::class) inProgressTransferUI: InProgressTransferUI,
) {
    val isDark = isSystemInDarkTheme()
    OriginalTempTheme(isDark = isDark) {
        with(inProgressTransferUI) {
            InProgressTransferItem(
                tag = 1,
                isDownload = isDownload,
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                fileName = fileName,
                progress = progress,
                speed = speed,
                isPaused = isPaused,
                isQueued = isQueued,
                isOverQuota = isOverQuota,
                areTransfersPaused = areTransfersPaused,
                onPlayPauseClicked = {},
            )
        }
    }
}

internal data class InProgressTransferUI(
    val isDownload: Boolean,
    val fileTypeResId: Int?,
    val previewUri: Uri?,
    val fileName: String,
    val progress: String,
    val speed: String?,
    val isPaused: Boolean,
    val isQueued: Boolean,
    val isOverQuota: Boolean,
    val areTransfersPaused: Boolean,
)

private class InProgressTransferItemProvider : PreviewParameterProvider<InProgressTransferUI> {
    private val name = "File name.pdf"
    private val progress = "63% of 1MB"
    private val speed = "4.2MB/s"

    override val values = listOf(
        InProgressTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            progress = progress,
            speed = speed,
            isPaused = false,
            isQueued = false,
            isOverQuota = false,
            areTransfersPaused = false,
        ),
        InProgressTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            progress = progress,
            speed = speed,
            isPaused = false,
            isQueued = false,
            isOverQuota = false,
            areTransfersPaused = true,
        ),
        InProgressTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            progress = progress,
            speed = "Paused",
            isPaused = true,
            isQueued = false,
            isOverQuota = false,
            areTransfersPaused = false,
        ),
        InProgressTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            progress = progress,
            speed = "Queued",
            isPaused = false,
            isQueued = true,
            isOverQuota = false,
            areTransfersPaused = false,
        ),
        InProgressTransferUI(
            isDownload = true,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            progress = "33% Transfer over quota",
            speed = null,
            isPaused = false,
            isQueued = false,
            isOverQuota = true,
            areTransfersPaused = false,
        ),
        InProgressTransferUI(
            isDownload = false,
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            fileName = name,
            progress = "33% Storage over quota",
            speed = null,
            isPaused = false,
            isQueued = false,
            isOverQuota = true,
            areTransfersPaused = false,
        )
    ).asSequence()
}

/**
 * Tag for the in-progress transfer item.
 */
const val TEST_TAG_IN_PROGRESS_TRANSFER_ITEM =
    "transfers_view:tab_in_progress:transfer_item"

/**
 * Tag for the in-progress transfer image.
 */
const val TEST_TAG_IN_PROGRESS_TRANSFER_IMAGE =
    "transfers_view:tab_in_progress:transfer_image"

/**
 * Tag for the in-progress transfer name.
 */
const val TEST_TAG_IN_PROGRESS_TRANSFER_NAME =
    "transfers_view:tab_in_progress:transfer_name"

/**
 * Tag for the in-progress transfer progress.
 */
const val TEST_TAG_IN_PROGRESS_TRANSFER_PROGRESS =
    "transfers_view:tab_in_progress:transfer_progress"

/**
 * Tag for the in-progress transfer speed.
 */
const val TEST_TAG_IN_PROGRESS_TRANSFER_SPEED =
    "transfers_view:tab_in_progress:transfer_speed"

/**
 * Tag for the in-progress transfer queued icon.
 */
const val TEST_TAG_IN_PROGRESS_TRANSFER_QUEUED_ICON =
    "transfers_view:tab_in_progress:transfer_queued_icon"

/**
 * Tag for the in-progress transfer queue icon.
 */
const val TEST_TAG_QUEUE_ICON = "transfers_view:tab_in_progress:transfer_item:queue_icon"

/**
 * Tag for the in-progress transfer pause icon.
 */
const val TEST_TAG_PAUSE_ICON = "transfers_view:tab_in_progress:transfer_item:pause_icon"

/**
 * Tag for the in-progress transfer play icon.
 */
const val TEST_TAG_PLAY_ICON = "transfers_view:tab_in_progress:transfer_item:play_icon"