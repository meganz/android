package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor

/**
 * Top-right corner badges over the video showing non-default edit state (speed
 * multiplier, mute). Media overlays on the video frame, so they use fixed
 * translucent-black chips rather than theme surfaces. The caller decides
 * visibility (e.g. hidden while the relevant tool's deck is open).
 */
@Composable
fun PreviewStatusBadges(
    speed: Float,
    showSpeed: Boolean,
    showMute: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showSpeed && !showMute) return
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showSpeed) {
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                MegaText(
                    text = "${if (speed % 1f == 0f) speed.toInt() else speed}×",
                    style = AppTheme.typography.labelMedium,
                    textColor = TextColor.OnColor,
                )
            }
        }
        if (showMute) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                MegaIcon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    tint = IconColor.OnColor,
                    contentDescription = "Muted",
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
