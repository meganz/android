package mega.privacy.android.shared.original.core.ui.controls.progressindicator

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

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
        color = MegaOriginalTheme.colors.icon.inverse.takeIf { useInverseColor }
            ?: MegaOriginalTheme.colors.icon.accent,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
    )

@CombinedThemePreviews
@Composable
private fun MegaCircularProgressIndicatorPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaCircularProgressIndicator(
            useInverseColor = initialValue
        )
    }
}