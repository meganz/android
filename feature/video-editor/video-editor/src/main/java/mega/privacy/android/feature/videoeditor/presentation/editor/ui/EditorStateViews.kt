package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Preview placeholder shown while the source video is being prepared (downloaded). Loads the node's
 * preview/thumbnail still ([imagePath]) with Coil, scaled to fit on a black background, so the
 * preview matches the eventual video frame instead of showing an empty black area.
 */
@Composable
fun PreparingPreview(
    imagePath: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imagePath)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
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
