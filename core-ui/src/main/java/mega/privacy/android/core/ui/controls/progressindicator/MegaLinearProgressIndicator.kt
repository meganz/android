package mega.privacy.android.core.ui.controls.progressindicator

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Wrapper for [LinearProgressIndicator] to set default parameters to better represent the project theme
 * @param progress set the current progress [0..1] or null for an indeterminate progress indicator
 *
 */
@Composable
fun MegaLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeCap: StrokeCap = StrokeCap.Butt,
) = if (progress != null) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        progress = progress,
        color = MegaTheme.colors.icon.accent,
        strokeCap = strokeCap,
        backgroundColor = MegaTheme.colors.background.surface3
    )
} else {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colors.secondary,
        strokeCap = strokeCap,
        backgroundColor = MegaTheme.colors.background.surface3
    )
}

@CombinedThemePreviews
@Composable
private fun MegaLinearProgressIndicatorPreview(
    @PreviewParameter(BooleanProvider::class) indeterminate: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Box(modifier = Modifier.padding(16.dp)) {
            MegaLinearProgressIndicator(
                progress = 0.3f.takeIf { !indeterminate }
            )
        }
    }
}