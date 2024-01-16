package mega.privacy.android.core.ui.controls.progressindicator

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

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
) =
    CircularProgressIndicator(
        modifier = modifier,
        color = MegaTheme.colors.icon.inverse.takeIf { useInverseColor }
            ?: MegaTheme.colors.icon.accent,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
    )

@CombinedThemePreviews
@Composable
private fun MegaCircularProgressIndicatorPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaCircularProgressIndicator(
            useInverseColor = initialValue
        )
    }
}