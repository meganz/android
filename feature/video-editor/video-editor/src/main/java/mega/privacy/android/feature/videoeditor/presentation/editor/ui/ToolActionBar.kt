package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor

/**
 * Cancel / label / Apply bar shown beneath the active tool's panel. The surface
 * fills behind the navigation-bar inset; the inner row carries the inset
 * padding.
 */
@Composable
fun ToolActionBar(
    toolLabel: String,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                MegaIcon(
                    imageVector = Icons.Filled.Close,
                    tint = IconColor.Primary,
                    contentDescription = "Cancel",
                )
            }
            MegaText(
                text = toolLabel,
                style = AppTheme.typography.titleMedium,
                textColor = TextColor.Primary,
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onApply),
                contentAlignment = Alignment.Center,
            ) {
                MegaIcon(
                    imageVector = Icons.Filled.Check,
                    tint = IconColor.Brand,
                    contentDescription = "Apply",
                )
            }
        }
    }
}
