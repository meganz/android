package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

@Composable
fun AlbumGridItem(
    title: String,
    coverImage: Any?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    errorPlaceholder: Painter? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape = RoundedCornerShape(4.dp)),
            model = ImageRequest
                .Builder(LocalContext.current)
                .data(coverImage)
                .crossfade(true)
                .build(),
            contentDescription = title,
            placeholder = placeholder,
            error = errorPlaceholder
        )

        MegaText(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = title,
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
    }
}