package mega.privacy.android.core.ui.controls.progressindicator

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_200_grey_700

/**
 * Wrapper for [LinearProgressIndicator] to set default parameters to better represent the project theme
 *
 */
@Composable
fun MegaLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    strokeCap: StrokeCap = StrokeCap.Butt,
) =
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        progress = progress,
        color = MaterialTheme.colors.secondaryVariant,
        strokeCap = strokeCap,
        backgroundColor = MaterialTheme.colors.grey_200_grey_700
    )

@CombinedThemePreviews
@Composable
private fun MegaLinearProgressIndicatorPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaLinearProgressIndicator(
            progress = 0.3f
        )
    }
}