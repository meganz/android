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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.indicators.ProgressBarIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Core component for a active transfer item.
 */
@Composable
fun ActiveTransferItem(
    tag: Int,
    isDownload: Boolean,
    fileTypeResId: Int?,
    previewUri: Uri?,
    fileName: String,
    progressPercentageString: String,
    progressSizeString: String,
    progress: Float,
    speed: String?,
    isPaused: Boolean,
    isOverQuota: Boolean,
    areTransfersPaused: Boolean,
    onPlayPauseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .height(68.dp)
        .fillMaxWidth()
        .testTag(TEST_TAG_ACTIVE_TRANSFER_ITEM + "_$tag")
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
        MegaIcon(
            painter = painterResource(id = iconPackR.drawable.ic_queue_line_small_regular_outline),
            contentDescription = "Reorder icon",
            tint = IconColor.Secondary,
            modifier = Modifier
                .size(16.dp)
                .testTag(TEST_TAG_QUEUE_ICON)
        )
        TransferImage(
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier
                .padding(start = 4.dp)
                .testTag(TEST_TAG_ACTIVE_TRANSFER_IMAGE),
        )
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .weight(1f)
        ) {
            MegaText(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = AppTheme.typography.titleMedium,
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
                    isOverQuota = isOverQuota,
                )
                MegaText(
                    text = subTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.bodySmall,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_ACTIVE_TRANSFER_SUBTITLE),
                )
            }
        }
        IconButton(
            onClick = onPlayPauseClicked, modifier = Modifier
                .padding(start = 8.dp)
                .testTag(if (isPaused) TEST_TAG_PLAY_ICON else TEST_TAG_PAUSE_ICON)
        ) {
            MegaIcon(
                painter = painterResource(
                    id = if (isPaused) iconPackR.drawable.ic_play_medium_regular_outline
                    else iconPackR.drawable.ic_pause_medium_regular_outline
                ),
                contentDescription = if (isPaused) stringResource(id = sharedR.string.transfers_section_action_play)
                else stringResource(id = sharedR.string.transfers_section_action_pause),
                tint = if (areTransfersPaused) IconColor.Disabled else IconColor.Secondary,
            )
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

@CombinedThemePreviews
@Composable
private fun ActiveTransferItemPreview(
    @PreviewParameter(ActiveTransferItemOrdinaryProvider::class) activeTransferUI: ActiveTransferUI,
) = Preview(activeTransferUI)

@CombinedThemePreviews
@Composable
private fun ActiveTransferItemUnusualPreview(
    @PreviewParameter(ActiveTransferItemUnusualProvider::class) activeTransferUI: ActiveTransferUI,
) = Preview(activeTransferUI)

@Composable
private fun Preview(activeTransferUI: ActiveTransferUI) {
    AndroidThemeForPreviews {
        with(activeTransferUI) {
            ActiveTransferItem(
                tag = 1,
                isDownload = isDownload,
                fileTypeResId = fileTypeResId,
                previewUri = previewUri,
                fileName = fileName,
                progressSizeString = progressSizeString,
                progressPercentageString = progressPercentageString,
                progress = progress,
                speed = speed,
                isPaused = isPaused,
                isOverQuota = isOverQuota,
                areTransfersPaused = areTransfersPaused,
                onPlayPauseClicked = {},
            )
        }
    }
}

internal data class ActiveTransferUI(
    val isDownload: Boolean,
    val fileTypeResId: Int?,
    val previewUri: Uri?,
    val fileName: String,
    val progressSizeString: String,
    val progressPercentageString: String,
    val progress: Float,
    val speed: String,
    val isPaused: Boolean,
    val isOverQuota: Boolean,
    val areTransfersPaused: Boolean,
)

private const val NAME = "File name.pdf"
private const val PROGRESS_SIZE = "6MB of 10MB"
private const val PROGRESS_PERCENT = "60%"
private const val PROGRESS = 0.6f
private const val SPEED = "4.2MB/s"
private const val PAUSED = "Paused"

private class ActiveTransferItemOrdinaryProvider :
    PreviewParameterProvider<ActiveTransferUI> {
    override val values =
        listOf(false, true).flatMap { isPaused ->
            listOf(true, false).map { isDownload ->
                ActiveTransferUI(
                    isDownload = isDownload,
                    fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                    previewUri = null,
                    fileName = NAME,
                    progressSizeString = PROGRESS_SIZE,
                    progressPercentageString = PROGRESS_PERCENT,
                    progress = PROGRESS,
                    speed = if (isPaused) PAUSED else SPEED,
                    isPaused = isPaused,
                    isOverQuota = false,
                    areTransfersPaused = false,
                )
            }
        }.asSequence()
}

private class ActiveTransferItemUnusualProvider :
    PreviewParameterProvider<ActiveTransferUI> {
    override val values = listOf(false, true).flatMap { isOverQuota ->
        listOf(false, true).map { areTransfersPaused ->
            ActiveTransferUI(
                isDownload = false,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = NAME,
                progressSizeString = PROGRESS_SIZE,
                progressPercentageString = PROGRESS_PERCENT,
                progress = PROGRESS,
                speed = if (areTransfersPaused) PAUSED else SPEED,
                isPaused = false,
                isOverQuota = isOverQuota,
                areTransfersPaused = areTransfersPaused,
            )
        }
    }.asSequence()
}

/**
 * Tag for the active tab
 */
private const val TEST_TAG_ACTIVE_TAB = "transfers_view:tab_active"

/**
 * Tag for the active transfer item.
 */
const val TEST_TAG_ACTIVE_TRANSFER_ITEM = "$TEST_TAG_ACTIVE_TAB:transfer_item"

/**
 * Tag for the active transfer image.
 */
const val TEST_TAG_ACTIVE_TRANSFER_IMAGE = "$TEST_TAG_ACTIVE_TAB:transfer_image"

/**
 * Tag for the active transfer name.
 */
const val TEST_TAG_ACTIVE_TRANSFER_NAME = "$TEST_TAG_ACTIVE_TAB:transfer_name"

/**
 * Tag for the active transfer progress.
 */
const val TEST_TAG_ACTIVE_TRANSFER_SUBTITLE = "$TEST_TAG_ACTIVE_TAB:transfer_subtitle"

/**
 * Tag for the active transfer queued icon.
 */
const val TEST_TAG_ACTIVE_TRANSFER_TYPE_ICON = "$TEST_TAG_ACTIVE_TAB:transfer_type_icon"

/**
 * Tag for the active transfer queue icon.
 */
const val TEST_TAG_QUEUE_ICON = "$TEST_TAG_ACTIVE_TAB:transfer_item:queue_icon"

/**
 * Tag for the active transfer pause icon.
 */
const val TEST_TAG_PAUSE_ICON = "$TEST_TAG_ACTIVE_TAB:transfer_item:pause_icon"

/**
 * Tag for the active transfer play icon.
 */
const val TEST_TAG_PLAY_ICON = "$TEST_TAG_ACTIVE_TAB:transfer_item:play_icon"