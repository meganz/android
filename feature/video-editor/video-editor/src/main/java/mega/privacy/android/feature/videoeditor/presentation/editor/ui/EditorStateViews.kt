package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/** Full-screen download progress shown while the source video is fetched. */
@Composable
fun EditorDownloadState(
    percent: Int,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.PageBackground,
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InfiniteProgressBarIndicator()
            if (percent in 1..99) {
                MegaText(
                    text = "$percent%",
                    style = AppTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** Full-screen error state shown when the source video cannot be loaded. */
@Composable
fun EditorErrorState(
    message: String,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.PageBackground,
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MegaText(
            text = message,
            style = AppTheme.typography.bodyMedium,
            textColor = TextColor.Primary,
        )
    }
}
