package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor

/**
 * Styled container for the active tool's panel. The panel itself is supplied by
 * the caller via [content], so the deck stays decoupled from any concrete tool.
 *
 * `systemGestureExclusion()` is applied at the deck level so any draggable /
 * pinchable control inside a tool panel inherits exclusion from Android's edge
 * back-swipe. Horizontal inset is left to the panel so tools that need
 * edge-to-edge content can opt out of a side gutter.
 */
@Composable
fun ToolDeck(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ColumnSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .systemGestureExclusion(),
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp), content = content)
    }
}
