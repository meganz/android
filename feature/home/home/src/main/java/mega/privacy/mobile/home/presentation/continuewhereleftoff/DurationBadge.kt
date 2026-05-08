package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Semi-transparent rounded badge showing audio/video duration.
 * Used in CWLO carousel, list, and grid items.
 */
@Composable
internal fun DurationBadge(
    duration: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = duration,
        style = AppTheme.typography.bodySmall,
        textColor = TextColor.OnColor,
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 3.dp))
            .background(color = Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
