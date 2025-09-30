package mega.privacy.android.feature.transfers.components.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Widget to show current transfers progress in the toolbar with completed animation
 */
@Composable
fun TransfersToolbarWidgetViewAnimated(
    transfersToolbarWidgetStatus: TransfersToolbarWidgetStatus,
    totalSizeAlreadyTransferred: Long,
    totalSizeToTransfer: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var previousStatus by remember { mutableStateOf(TransfersToolbarWidgetStatus.Completed) }
    var justCompleted by remember(transfersToolbarWidgetStatus) {
        mutableStateOf(
            transfersToolbarWidgetStatus == TransfersToolbarWidgetStatus.Completed
                    && previousStatus == TransfersToolbarWidgetStatus.Transferring
        )
    }
    LaunchedEffect(justCompleted) {
        if (justCompleted) {
            coroutineScope.launch {
                delay(4.seconds - ANIMATION_DURATION.milliseconds)
                justCompleted = false
            }
        }
    }

    SideEffect {
        previousStatus = transfersToolbarWidgetStatus
    }

    AnimatedVisibility(
        visible = !transfersToolbarWidgetStatus.hasFinished() || justCompleted,
        enter = scaleIn(animationSpecs, initialScale = ANIMATION_SCALE) + fadeIn(animationSpecs),
        exit = scaleOut(animationSpecs, targetScale = ANIMATION_SCALE) + fadeOut(animationSpecs),
        modifier = modifier,
    ) {
        TransfersToolbarWidgetView(
            transfersToolbarWidgetStatus = transfersToolbarWidgetStatus,
            totalSizeAlreadyTransferred = totalSizeAlreadyTransferred,
            totalSizeToTransfer = totalSizeToTransfer,
            onClick = onClick,
        )
    }
}

/**
 * Widget to show current transfers progress in the toolbar
 */
@Composable
internal fun TransfersToolbarWidgetView(
    transfersToolbarWidgetStatus: TransfersToolbarWidgetStatus,
    totalSizeAlreadyTransferred: Long,
    totalSizeToTransfer: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .padding(PADDING.dp)
            .size(SIZE.dp)
            .clickable(
                onClick = onClick,
                role = Role.Button,
            )
            .testTag(TAG_TRANSFERS_WIDGET),
    ) {
        val icon = when (transfersToolbarWidgetStatus) {
            TransfersToolbarWidgetStatus.Transferring, TransfersToolbarWidgetStatus.Idle -> IconPack.Medium.Thin.Outline.TransferArrowsUpDownAlt
            TransfersToolbarWidgetStatus.Paused, TransfersToolbarWidgetStatus.Completed, TransfersToolbarWidgetStatus.Error -> IconPack.Medium.Thin.Outline.TransferArrowsUpDownAltCircleCutout
            TransfersToolbarWidgetStatus.OverQuota -> IconPack.Medium.Thin.Outline.TransferArrowsUpDownAltTriangleCutout
        }
        MegaIcon(
            rememberVectorPainter(icon),
            tint = IconColor.Primary,
            modifier = Modifier.align(Alignment.Center)
        )
        when (transfersToolbarWidgetStatus) {
            TransfersToolbarWidgetStatus.Transferring -> {
                ProgressCircle(totalSizeAlreadyTransferred, totalSizeToTransfer)
            }

            TransfersToolbarWidgetStatus.Idle -> IdleCircle()
            else -> StatusIcon(transfersToolbarWidgetStatus)
        }
    }
}

@Composable
private fun StatusIcon(
    transfersToolbarWidgetStatus: TransfersToolbarWidgetStatus,
) {
    when (transfersToolbarWidgetStatus) {
        TransfersToolbarWidgetStatus.Transferring -> null
        TransfersToolbarWidgetStatus.Completed -> IconPack.Small.Thin.Outline.TransferCheckCircle to DSTokens.colors.support.success
        TransfersToolbarWidgetStatus.Paused -> IconPack.Small.Thin.Outline.TransferPause to DSTokens.colors.icon.secondary
        TransfersToolbarWidgetStatus.OverQuota -> IconPack.Small.Thin.Outline.TransferWarning to DSTokens.colors.support.warning
        TransfersToolbarWidgetStatus.Error -> IconPack.Small.Thin.Outline.TransferError to DSTokens.colors.support.error
        TransfersToolbarWidgetStatus.Idle -> null
    }?.let { (icon, color) ->
        Icon(
            icon,
            contentDescription = transfersToolbarWidgetStatus.name,
            tint = color,
            modifier = Modifier.testTag(TAG_STATUS_ICON)
        )
    }
}

@Composable
private fun ProgressCircle(
    totalSizeAlreadyTransferred: Long,
    totalSizeToTransfer: Long,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (totalSizeToTransfer <= 0L) {
            0f
        } else {
            (totalSizeAlreadyTransferred.toFloat() / totalSizeToTransfer.toFloat())
        },
        animationSpec = tween(1500),
        label = "progress",
    )
    val successColor = DSTokens.colors.support.success
    val background = DSTokens.colors.border.strong
    Canvas(modifier = modifier.fillMaxSize()) {
        val diameter = (RADIUS * 2).dp.toPx()
        val width = PROGRESS_THICKNESS.dp.toPx()
        //background
        drawCircle(
            color = background,
            radius = RADIUS.dp.toPx(),
            style = Stroke(width = width)
        )
        //progress
        drawArc(
            color = successColor,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = Offset(width / 2, width / 2),
            size = Size(diameter, diameter),
            style = Stroke(width = width, cap = StrokeCap.Round)
        )
    }
}


@Composable
private fun IdleCircle(
    modifier: Modifier = Modifier,
) {
    val background = DSTokens.colors.icon.primary
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            color = background,
            radius = RADIUS.dp.toPx(),
            style = Stroke(width = IDLE_THICKNESS.dp.toPx())
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TransfersToolbarWidgetPreview(
    @PreviewParameter(TransfersWidgetPreviewProvider::class) parameter: Pair<Long, TransfersToolbarWidgetStatus>,
) {
    AndroidThemeForPreviews {
        TransfersToolbarWidgetView(parameter.second, parameter.first, 100L) { }
    }
}

@CombinedThemePreviews
@Composable
private fun TransfersToolbarWidgetInAppBarPreview(
    @PreviewParameter(TransfersWidgetStatusPreviewProvider::class) status: TransfersToolbarWidgetStatus,
) {
    AndroidThemeForPreviews {
        MegaTopAppBar(
            title = status.name,
            navigationType = AppBarNavigationType.None,
            trailingIcons = {
                TransfersToolbarWidgetView(
                    status,
                    30L,
                    100L
                )
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TransfersToolbarWidgetInAppBarWithActionsPreview(
    @PreviewParameter(TransfersWidgetStatusPreviewProvider::class) status: TransfersToolbarWidgetStatus,
) {
    AndroidThemeForPreviews {
        MegaTopAppBar(
            title = status.name,
            navigationType = AppBarNavigationType.None,
            trailingIcons = {
                TransfersToolbarWidgetView(
                    status,
                    30L,
                    100L
                )
            },
            actions = listOf(
                object : MenuActionWithIcon {
                    override val testTag = "test"

                    @Composable
                    override fun getDescription() = "test"

                    @Composable
                    override fun getIconPainter() =
                        rememberVectorPainter(IconPack.Medium.Thin.Outline.Bell)
                },
                object : MenuActionWithIcon {
                    override val testTag = "test"

                    @Composable
                    override fun getDescription() = "test"

                    @Composable
                    override fun getIconPainter() =
                        rememberVectorPainter(IconPack.Medium.Thin.Outline.AlertTriangle)
                }),
            onActionPressed = {}
        )
    }
}

private class TransfersWidgetStatusPreviewProvider :
    PreviewParameterProvider<TransfersToolbarWidgetStatus> {
    override val values = TransfersToolbarWidgetStatus.entries.asSequence()
}

private class TransfersWidgetPreviewProvider :
    PreviewParameterProvider<Pair<Long, TransfersToolbarWidgetStatus>> {
    override val values = TransfersToolbarWidgetStatus.entries.flatMap {
        if (it == TransfersToolbarWidgetStatus.Transferring) {
            listOf(0L to it, 1L to it, 50L to it, 90L to it)
        } else {
            listOf(0L to it)
        }
    }.asSequence()
}

internal const val TAG_STATUS_ICON = "transfers_toolbar_widget_view:button:status_button"
internal const val TAG_TRANSFERS_WIDGET = "transfers_widget_view:button:floating_button"

private const val SIZE = 24f
private const val BOX_SIZE = 48f
private const val PADDING = (BOX_SIZE - SIZE) / 2
private const val PROGRESS_THICKNESS = 2f
private const val IDLE_THICKNESS = 1.5f
private const val RADIUS = (SIZE - PROGRESS_THICKNESS) / 2f
private const val ANIMATION_SCALE = 0.2f
private const val ANIMATION_DURATION = 300
private val animationSpecs = TweenSpec<Float>(durationMillis = ANIMATION_DURATION)