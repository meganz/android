package mega.privacy.android.feature.fileinfo.presentation.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * A small translucent pill showing a media playback duration (e.g. "1:24"), overlaid on the header
 * preview for audio/video nodes.
 *
 * @param text the formatted duration
 * @param modifier modifier for the badge
 */
@Composable
internal fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.SurfaceTransparent,
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
    ) {
        MegaText(
            text = text,
            textColor = TextColor.OnColor,
            style = AppTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DurationBadgePreview() {
    AndroidThemeForPreviews {
        DurationBadge(text = "1:24")
    }
}
