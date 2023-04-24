package mega.privacy.android.app.components.transferWidget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.amber_700_amber_300
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersStatus.*

@Composable
internal fun TransfersWidgetView(
    transfersData: TransfersInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .padding(16.dp),
        backgroundColor = MaterialTheme.colors.primary,
    ) {

        val progressBackgroundColor = MaterialTheme.colors.grey_alpha_012_white_alpha_012
        val progressColor = transfersData.progressColor()
        val progressArc = transfersData.completedProgress * 360f
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
            painter = painterResource(if (transfersData.uploading) R.drawable.ic_transfers_upload else R.drawable.ic_transfers_download),
            contentDescription = stringResource(if (transfersData.uploading) R.string.context_upload else R.string.context_download),
            modifier = Modifier
                .size(diameter.dp)
                .testTag(TAG_UPLOADING_DOWNLOADING_ICON),
            contentScale = ContentScale.None,
        )
        // status icon
        transfersData.statusIconRes()?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = transfersData.status.name,
                alignment = Alignment.TopEnd,
                modifier = Modifier
                    .size(diameter.dp)
                    .padding(end = 8.dp, top = 3.dp)
                    .testTag(TAG_STATUS_ICON),
                contentScale = ContentScale.None
            )
        }
    }
}

@Composable
private fun TransfersInfo.progressColor() = when (status) {
    Paused -> MaterialTheme.colors.secondary
    OverQuota -> MaterialTheme.colors.amber_700_amber_300
    TransferError -> MaterialTheme.colors.error
    else -> MaterialTheme.colors.secondary
}

@DrawableRes
private fun TransfersInfo.statusIconRes() = when (status) {
    Paused -> R.drawable.ic_transfers_paused
    OverQuota -> R.drawable.ic_transfers_overquota
    TransferError -> R.drawable.ic_transfers_error
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
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TransfersWidgetView(parameter, onClick = {})
    }
}

private class TransfersWidgetPreviewProvider :
    PreviewParameterProvider<TransfersInfo> {
    override val values: Sequence<TransfersInfo>
        get() = listOf(
            TransfersInfo(
                status = Transferring,
                totalSizeTransferred = 3,
                totalSizePendingTransfer = 10,
                uploading = true
            ),
            TransfersInfo(
                status = Transferring,
                totalSizeTransferred = 4,
                totalSizePendingTransfer = 10,
                uploading = false
            ),
            TransfersInfo(
                status = Paused,
                totalSizeTransferred = 5,
                totalSizePendingTransfer = 10,
                uploading = true
            ),
            TransfersInfo(
                status = OverQuota,
                totalSizeTransferred = 6,
                totalSizePendingTransfer = 10,
                uploading = false
            ),
            TransfersInfo(
                status = TransferError,
                totalSizeTransferred = 7,
                totalSizePendingTransfer = 10,
                uploading = true
            ),
        ).asSequence()
}