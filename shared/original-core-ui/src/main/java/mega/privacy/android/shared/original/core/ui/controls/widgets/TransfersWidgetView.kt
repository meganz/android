package mega.privacy.android.shared.original.core.ui.controls.widgets

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Sets a transfers widget as the content of this ComposeView.
 *
 * @param transfersInfoFlow a flow with the info to update the widget
 * @param hideFlow a flow to hide the widget regardless of [transfersInfoFlow]
 */
fun ComposeView.setTransfersWidgetContent(
    transfersInfoFlow: Flow<TransfersInfo>,
    hideFlow: Flow<Boolean>,
    onClick: () -> Unit,
) {

    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val transfersInfo by transfersInfoFlow.collectAsStateWithLifecycle(
            TransfersInfo()
        )
        val widgetHide by hideFlow.collectAsStateWithLifecycle(
            true
        )
        OriginalTempTheme(isDark = isSystemInDarkTheme()) {
            if (!widgetHide) {
                TransfersWidgetViewAnimated(
                    transfersInfo = transfersInfo,
                    onClick = onClick,
                    modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
                )
            }
        }
    }
}

/**
 * Widget to show current transfers progress with animated visibility
 */
@Composable
fun TransfersWidgetViewAnimated(
    transfersInfo: TransfersInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var previousStatus by remember { mutableStateOf(TransfersStatus.NotTransferring) }
    var justCompleted by remember(transfersInfo.status) {
        mutableStateOf(
            transfersInfo.status == TransfersStatus.NotTransferring
                    && previousStatus == TransfersStatus.Transferring
        )
    }
    LaunchedEffect(justCompleted) {
        if (justCompleted) {
            coroutineScope.launch {
                delay(4.seconds - animationDuration.milliseconds)
                justCompleted = false
            }
        }
    }

    SideEffect {
        previousStatus = transfersInfo.status
    }

    AnimatedVisibility(
        visible = transfersInfo.status != TransfersStatus.NotTransferring || justCompleted,
        enter = scaleIn(animationSpecs, initialScale = animationScale) + fadeIn(animationSpecs),
        exit = scaleOut(animationSpecs, targetScale = animationScale) + fadeOut(animationSpecs),
        modifier = modifier,
    ) {
        TransfersWidgetView(
            transfersInfo = transfersInfo,
            completed = justCompleted,
            onClick = onClick,
        )
    }
}

/**
 * Widget to show current transfers progress
 */
/**
 * Widget to show current transfers progress
 */
@Composable
internal fun TransfersWidgetView(
    transfersInfo: TransfersInfo,
    modifier: Modifier = Modifier,
    completed: Boolean = false,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .testTag(TAG_TRANSFERS_WIDGET),
        backgroundColor = MegaOriginalTheme.colors.background.surface1,
    ) {

        val progressBackgroundColor = MegaOriginalTheme.colors.background.surface3
        val progressColor = progressColor(transfersInfo, completed)
        val progressArc = if (completed) 360f else transfersInfo.completedProgress * 360f
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
        statusIconRes(transfersInfo, completed)?.let {
            Box(
                modifier = Modifier.size(diameter.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Icon(
                    painter = painterResource(id = it),
                    contentDescription = if (completed) "Completed" else transfersInfo.status.name,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(16.dp)
                        .background(
                            color = MegaOriginalTheme.colors.background.surface1,
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
private fun progressColor(transfersInfo: TransfersInfo, justCompleted: Boolean) =
    if (justCompleted) MegaOriginalTheme.colors.support.success
    else with(transfersInfo) {
        when (status) {
            TransfersStatus.OverQuota -> MegaOriginalTheme.colors.support.warning
            TransfersStatus.TransferError -> MegaOriginalTheme.colors.support.error
            else -> MegaOriginalTheme.colors.icon.secondary
        }
    }

@DrawableRes
private fun statusIconRes(transfersInfo: TransfersInfo, justCompleted: Boolean) =
    if (justCompleted) iconPackR.drawable.ic_check_circle_small_regular_solid
    else with(transfersInfo) {
        when (status) {
            TransfersStatus.Paused -> iconPackR.drawable.ic_pause_circle_small_regular_solid
            TransfersStatus.OverQuota -> iconPackR.drawable.ic_alert_triangle_small_regular_solid
            TransfersStatus.TransferError -> iconPackR.drawable.ic_alert_circle_small_regular_solid
            else -> null
        }
    }

private const val diameter = 56f
private const val thickness = diameter / 25f
private const val innerRadius = diameter / 2.75f
private const val outerRadius = innerRadius + thickness / 2f
private const val padding = diameter / 2f - outerRadius

private const val animationScale = 0.2f
private const val animationDuration = 300
private val animationSpecs = TweenSpec<Float>(durationMillis = animationDuration)

/**
 * debug tag for status icon
 */
internal const val TAG_STATUS_ICON = "statusIcon"

/**
 * debug tag for downloading / uploading icon
 */
internal const val TAG_UPLOADING_DOWNLOADING_ICON = "uploadingDownloading"

internal const val TAG_TRANSFERS_WIDGET = "transfers_widget_view:button:floating_button"


@CombinedThemePreviews
@Composable
private fun TransfersWidgetPreview(
    @PreviewParameter(TransfersWidgetPreviewProvider::class) parameter: Pair<TransfersInfo, Boolean>,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransfersWidgetView(parameter.first, completed = parameter.second) {}
    }
}

private class TransfersWidgetPreviewProvider :
    PreviewParameterProvider<Pair<TransfersInfo, Boolean>> {
    override val values: Sequence<Pair<TransfersInfo, Boolean>>
        get() = listOf(
            TransfersInfo(
                status = TransfersStatus.Transferring,
                totalSizeAlreadyTransferred = 3,
                totalSizeToTransfer = 10,
                uploading = true
            ) to false,
            TransfersInfo(
                status = TransfersStatus.Transferring,
                totalSizeAlreadyTransferred = 4,
                totalSizeToTransfer = 10,
                uploading = false
            ) to false,
            TransfersInfo(
                status = TransfersStatus.Paused,
                totalSizeAlreadyTransferred = 5,
                totalSizeToTransfer = 10,
                uploading = true
            ) to false,
            TransfersInfo(
                status = TransfersStatus.OverQuota,
                totalSizeAlreadyTransferred = 6,
                totalSizeToTransfer = 10,
                uploading = false
            ) to false,
            TransfersInfo(
                status = TransfersStatus.TransferError,
                totalSizeAlreadyTransferred = 7,
                totalSizeToTransfer = 10,
                uploading = true
            ) to false,
            TransfersInfo(
                status = TransfersStatus.NotTransferring,
                totalSizeAlreadyTransferred = 10,
                totalSizeToTransfer = 10,
                uploading = false
            ) to true,
        ).asSequence()
}