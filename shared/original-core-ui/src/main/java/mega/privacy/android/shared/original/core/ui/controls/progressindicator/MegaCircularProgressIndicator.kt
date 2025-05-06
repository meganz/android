package mega.privacy.android.shared.original.core.ui.controls.progressindicator

import androidx.annotation.FloatRange
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import mega.android.core.ui.theme.values.BackgroundColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.backgroundColor
import mega.privacy.android.shared.original.core.ui.theme.supportColor

/**
 * Wrapper for [CircularProgressIndicator] to set default parameters to better represent the project theme
 *
 */
@Composable
fun MegaCircularProgressIndicator(
    modifier: Modifier = Modifier,
    useInverseColor: Boolean = false,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    strokeCap: StrokeCap = StrokeCap.Square,
    @FloatRange(from = 0.0, to = 1.0)
    progress: Float? = null,
) = if (progress != null) {
    CircularProgressIndicator(
        modifier = modifier,
        color = DSTokens.colors.icon.inverse.takeIf { useInverseColor }
            ?: DSTokens.colors.icon.accent,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        progress = progress,
    )
} else {
    CircularProgressIndicator(
        modifier = modifier,
        color = DSTokens.colors.icon.inverse.takeIf { useInverseColor }
            ?: DSTokens.colors.icon.accent,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
    )
}

/**
 * Wrapper for [CircularProgressIndicator] with custom colors to set default parameters to better represent the project theme
 *
 */
@Composable
fun MegaCircularProgressIndicator(
    supportColor: SupportColor,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    strokeCap: StrokeCap = StrokeCap.Square,
    @FloatRange(from = 0.0, to = 1.0)
    progress: Float? = null,
    backgroundColor: BackgroundColor? = null,
) = if (progress != null) {
    CircularProgressIndicator(
        modifier = modifier,
        color = DSTokens.supportColor(supportColor),
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        progress = progress,
        backgroundColor = backgroundColor?.let {
            DSTokens.backgroundColor(backgroundColor)
        } ?: Color.Transparent
    )
} else {
    CircularProgressIndicator(
        modifier = modifier,
        color = DSTokens.supportColor(supportColor),
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        backgroundColor = backgroundColor?.let {
            DSTokens.backgroundColor(backgroundColor)
        } ?: Color.Transparent
    )
}

@CombinedThemePreviews
@Composable
private fun MegaCircularProgressIndicatorPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaCircularProgressIndicator(
            useInverseColor = initialValue
        )
    }
}