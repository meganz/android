package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.model.HighlightedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

@Composable
fun AlbumGridItem(
    title: HighlightedText,
    coverImage: Any?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    errorPlaceholder: Painter? = null,
    isShared: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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
                contentDescription = title.full,
                placeholder = placeholder,
                error = errorPlaceholder
            )

            if (isShared) {
                BoxSurface(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .align(Alignment.TopEnd),
                    surfaceColor = SurfaceColor.SurfaceTransparent
                ) {
                    MegaIcon(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.Center),
                        imageVector = IconPack.Medium.Thin.Outline.Link02,
                        tint = IconColor.OnColor,
                        contentDescription = title.full,
                    )
                }
            }
        }

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

@CombinedThemePreviews
@Composable
private fun AlbumGridItemPreview() {
    AndroidThemeForPreviews {
        AlbumGridItem(
            modifier = Modifier.width(104.dp),
            title = HighlightedText("Album Title"),
            coverImage = null,
            isShared = true,
            errorPlaceholder = painterResource(mega.privacy.android.icon.pack.R.drawable.ic_add_to_album),
            placeholder = painterResource(mega.privacy.android.icon.pack.R.drawable.ic_add_to_album)
        )
    }
}