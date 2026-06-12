package mega.privacy.android.feature.videoeditor.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.TextOnlyButtonM3
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Shared shell for the editor's blocking progress dialogs: a centered card with a title, an optional ellipsized
 * supporting line, a status row annotated with the percentage, a determinate
 * progress bar, and a Cancel action.
 *
 * The dialog cannot be dismissed by tapping outside — these dialogs guard
 * long-running work an accidental scrim tap must not abort. [onCancel] (the
 * Cancel button or a back press) is the only way out.
 *
 * @param title headline of the dialog.
 * @param percent progress, 0..100.
 * @param onCancel cancels the guarded work.
 * @param description optional ellipsized line under the title (e.g. a file name).
 * @param statusContent left side of the status row, opposite the percentage readout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockingProgressDialog(
    title: String,
    percent: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    statusContent: @Composable RowScope.() -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        ColumnSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                MegaText(
                    text = title,
                    style = AppTheme.typography.headlineSmall,
                    textColor = TextColor.Primary,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MegaText(
                        text = description,
                        style = AppTheme.typography.bodyMedium,
                        textColor = TextColor.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = statusContent,
                    )
                    MegaText(
                        text = "$percent%",
                        style = AppTheme.typography.titleSmall,
                        textColor = TextColor.Primary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CustomProgressBar(
                    indicatorProgress = percent / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextOnlyButtonM3(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
internal fun CustomProgressBar(
    indicatorProgress: Float,
    modifier: Modifier = Modifier,
) {
    val isInPreview = LocalInspectionMode.current
    var progress by remember { mutableFloatStateOf(if (isInPreview) indicatorProgress else 0f) }
    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "Progress Animation"
    )

    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(20.dp)),
        progress = { progressAnimation.coerceAtLeast(0.0f) },
        color = DSTokens.colors.support.success,
        strokeCap = StrokeCap.Square,
        trackColor = DSTokens.colors.background.surface2,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )

    LaunchedEffect(indicatorProgress) {
        progress = indicatorProgress
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExportProgressDialogPreview() {
    AndroidThemeForPreviews {
        BlockingProgressDialog(
            percent = 42,
            title = "Saving",
            description = "Holiday in Queenstown.mp4",
            statusContent = { },
            onCancel = {},
        )
    }
}
