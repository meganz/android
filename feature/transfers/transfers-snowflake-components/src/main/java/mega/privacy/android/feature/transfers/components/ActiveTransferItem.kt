package mega.privacy.android.feature.transfers.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import mega.android.core.ui.components.indicators.ProgressBarIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
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
    enableSwipeToDismiss: Boolean,
    onPlayPauseClicked: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isDraggable: Boolean = true,
    isSelected: Boolean? = null,
    isBeingDragged: Boolean = false,
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { swipeToDismissBoxValue ->
            if (swipeToDismissBoxValue == SwipeToDismissBoxValue.EndToStart) {
                onCancel()
            }

            swipeToDismissBoxValue != SwipeToDismissBoxValue.StartToEnd
        }
    )

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        backgroundContent = {
            when (swipeToDismissBoxState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> if (enableSwipeToDismiss) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash),
                        contentDescription = "Cancel icon",
                        tint = IconColor.Inverse,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DSTokens.colors.support.error)
                            .wrapContentSize(Alignment.CenterEnd)
                            .padding(12.dp)
                            .testTag(TEST_TAG_CANCEL_ICON)
                    )
                }

                else -> {}
            }
        },
        modifier = modifier
            .height(68.dp)
            .fillMaxWidth()
            .testTag(TEST_TAG_ACTIVE_TRANSFER_ITEM + "_$tag"),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enableSwipeToDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isBeingDragged) DSTokens.colors.background.surface1 else DSTokens.colors.background.pageBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 16.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subTitle = listOf(
                    progressPercentageString,
                    progressSizeString,
                    speed,
                ).joinToString(" · ")
                AnimatedVisibility(
                    isDraggable,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Small.Thin.Outline.QueueLine),
                        contentDescription = "Reorder icon",
                        tint = IconColor.Secondary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                            .testTag(TEST_TAG_QUEUE_ICON)
                    )
                }
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
                            isOverQuota = isOverQuota,
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
                if (isSelected == null) {
                    IconButton(
                        onClick = onPlayPauseClicked,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp)
                            .testTag(if (isPaused) TEST_TAG_PLAY_ICON else TEST_TAG_PAUSE_ICON)
                    ) {
                        MegaIcon(
                            painter = rememberVectorPainter(if (isPaused) IconPack.Medium.Thin.Outline.Play else IconPack.Medium.Thin.Outline.Pause),
                            contentDescription = if (isPaused) stringResource(id = sharedR.string.transfers_section_action_play)
                            else stringResource(id = sharedR.string.transfers_section_action_pause),
                            tint = if (areTransfersPaused) IconColor.Disabled else IconColor.Secondary,
                        )
                    }
                } else {
                    SelectedTransferIcon(
                        isSelected,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            ProgressBarIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                progressPercentage = progress * 100f,
                supportColor = if (isOverQuota) SupportColor.Warning else SupportColor.Success
            )
        }
    }
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
                enableSwipeToDismiss = true,
                isSelected = isSelected,
                onPlayPauseClicked = {},
                onCancel = {},
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
    val isDraggable: Boolean = true,
    val isSelected: Boolean? = null,
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
        listOf(true, false).flatMap { isSelected ->
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
                    isDraggable = !isSelected,
                    isSelected = isSelected,
                )
            }
        }
    }.asSequence()
}

/**
 * Tag for the active tab
 */
internal const val TEST_TAG_ACTIVE_TAB = "transfers_view:tab_active"

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
const val TEST_TAG_QUEUE_ICON = "$TEST_TAG_ACTIVE_TRANSFER_ITEM:queue_icon"

/**
 * Tag for the active transfer pause icon.
 */
const val TEST_TAG_PAUSE_ICON = "$TEST_TAG_ACTIVE_TRANSFER_ITEM:pause_icon"

/**
 * Tag for the active transfer play icon.
 */
const val TEST_TAG_PLAY_ICON = "$TEST_TAG_ACTIVE_TRANSFER_ITEM:play_icon"

/**
 * Tag for the active transfer cancel icon.
 */
const val TEST_TAG_CANCEL_ICON = "$TEST_TAG_ACTIVE_TRANSFER_ITEM:cancel_icon"