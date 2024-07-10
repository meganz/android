package mega.privacy.android.shared.original.core.ui.controls.widgets

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Widget to show current transfers progress
 */
@Composable
fun TransfersWidgetView(
    transfersInfo: TransfersInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
        backgroundColor = MegaOriginalTheme.colors.background.surface1,
    ) {

        val progressBackgroundColor = MegaOriginalTheme.colors.background.surface3
        val progressColor = transfersInfo.progressColor()
        val progressArc = transfersInfo.completedProgress * 360f
        Canvas(modifier = Modifier.size(diameter.dp)) {
            //background
            drawCircle(
                color = progressBackgroundColor,
                radius = innerRadius.dp.toPx() + thickness.dp.toPx() / 2f,
                style = Stroke(width = thickness.dp.toPx())
            )
            //progress
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progressArc,
                useCenter = false,
                topLeft = Offset(padding.dp.toPx(), padding.dp.toPx()),
                size = Size(outerRadius.dp.toPx() * 2, outerRadius.dp.toPx() * 2),
                style = Stroke(width = thickness.dp.toPx())
            )
        }
        // upload / download icon
        Image(
            painter = painterResource(if (transfersInfo.uploading) iconPackR.drawable.ic_transfers_upload else iconPackR.drawable.ic_transfers_download),
            contentDescription = if (transfersInfo.uploading) "Upload" else "Download",
            modifier = Modifier
                .size(diameter.dp)
                .testTag(TAG_UPLOADING_DOWNLOADING_ICON),
            contentScale = ContentScale.None,
        )
        // status icon
        transfersInfo.statusIconRes()?.let {
            Box(
                modifier = Modifier.size(diameter.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Icon(
                    painter = painterResource(id = it),
                    contentDescription = transfersInfo.status.name,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(16.dp)
                        .background(
                            color = MegaOriginalTheme.colors.background.pageBackground,
                            shape = CircleShape
                        )
                        .padding(2.dp)
                        .testTag(TAG_STATUS_ICON),
                    tint = progressColor
                )
            }
        }
    }
}

@Composable
private fun TransfersInfo.progressColor() = when (status) {
    TransfersStatus.OverQuota -> MegaOriginalTheme.colors.support.warning
    TransfersStatus.TransferError -> MegaOriginalTheme.colors.support.error
    else -> MegaOriginalTheme.colors.icon.secondary
}

@DrawableRes
private fun TransfersInfo.statusIconRes() = when (status) {
    TransfersStatus.Paused -> iconPackR.drawable.ic_pause_circle_small_regular_solid
    TransfersStatus.OverQuota -> iconPackR.drawable.ic_alert_triangle_small_regular_solid
    TransfersStatus.TransferError -> iconPackR.drawable.ic_alert_circle_small_regular_solid
    else -> null
}

private const val diameter = 56f
private const val thickness = diameter / 25f
private const val innerRadius = diameter / 2.75f
private const val outerRadius = innerRadius + thickness / 2f
private const val padding = diameter / 2f - outerRadius

/**
 * debug tag for status icon
 */
const val TAG_STATUS_ICON = "statusIcon"

/**
 * debug tag for downloading / uploading icon
 */
const val TAG_UPLOADING_DOWNLOADING_ICON = "uploadingDownloading"

@CombinedThemePreviews
@Composable
private fun TransfersWidgetPreview(
    @PreviewParameter(TransfersWidgetPreviewProvider::class) parameter: TransfersInfo,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransfersWidgetView(parameter, onClick = {})
    }
}

private class TransfersWidgetPreviewProvider :
    PreviewParameterProvider<TransfersInfo> {
    override val values: Sequence<TransfersInfo>
        get() = listOf(
            TransfersInfo(
                status = TransfersStatus.Transferring,
                totalSizeAlreadyTransferred = 3,
                totalSizeToTransfer = 10,
                uploading = true
            ),
            TransfersInfo(
                status = TransfersStatus.Transferring,
                totalSizeAlreadyTransferred = 4,
                totalSizeToTransfer = 10,
                uploading = false
            ),
            TransfersInfo(
                status = TransfersStatus.Paused,
                totalSizeAlreadyTransferred = 5,
                totalSizeToTransfer = 10,
                uploading = true
            ),
            TransfersInfo(
                status = TransfersStatus.OverQuota,
                totalSizeAlreadyTransferred = 6,
                totalSizeToTransfer = 10,
                uploading = false
            ),
            TransfersInfo(
                status = TransfersStatus.TransferError,
                totalSizeAlreadyTransferred = 7,
                totalSizeToTransfer = 10,
                uploading = true
            ),
        ).asSequence()
}