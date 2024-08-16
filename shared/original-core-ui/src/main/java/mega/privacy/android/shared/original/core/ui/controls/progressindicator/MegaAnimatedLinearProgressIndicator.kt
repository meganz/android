package mega.privacy.android.shared.original.core.ui.controls.progressindicator

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Wrapper for [MegaAnimatedLinearProgressIndicator] to set default parameters to better represent the project theme
 * @param indicatorProgress set the current progress [0..1] or null for an indeterminate progress indicator
 *
 */
@Composable
fun MegaAnimatedLinearProgressIndicator(
    modifier: Modifier = Modifier,
    indicatorProgress: Float = 0f,
    fastAnimation: Boolean = false,
    height: Dp = 8.dp,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val isInPreview = LocalInspectionMode.current
    var progress by remember { mutableFloatStateOf(if (isInPreview) indicatorProgress else 0f) }
    val progressAnimDuration = if (fastAnimation) 1000 else 3000
    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = progressAnimDuration, easing = FastOutSlowInEasing),
        label = "Progress Animation"
    )

    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp)),
        progress = progressAnimation,
        color = MegaOriginalTheme.colors.button.brand,
        strokeCap = strokeCap,
        backgroundColor = MegaOriginalTheme.colors.background.surface2
    )

    LaunchedEffect(indicatorProgress) {
        progress = indicatorProgress
    }
}

@CombinedThemePreviews
@Composable
private fun MegaAnimatedLinearProgressIndicatorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Box(modifier = Modifier.padding(16.dp)) {
            MegaAnimatedLinearProgressIndicator(
                indicatorProgress = 0.5f
            )
        }
    }
}