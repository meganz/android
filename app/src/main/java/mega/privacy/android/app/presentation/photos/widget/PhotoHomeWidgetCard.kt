package mega.privacy.android.app.presentation.photos.widget

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.photos.Photo

@Composable
internal fun PhotoHomeWidgetCard(photo: Photo, modifier: Modifier) {
    CardSurface(surfaceColor = SurfaceColor.Surface1) {
        AsyncImage(
            model = ImageRequest
                .Builder(LocalContext.current)
                .data(photo.previewFilePath)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            error = painterResource(R.drawable.deny_participant_icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
    }
}