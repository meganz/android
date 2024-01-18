package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Rich link content
 *
 * @param image
 * @param contentTitle
 * @param contentDescription
 * @param icon
 * @param host
 */
@Composable
fun RichLinkContentView(
    contentTitle: String,
    icon: Painter?,
    host: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    image: Painter? = null,
    isFullImage: Boolean = true,
) {
    Column(modifier = modifier.padding(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            image?.let {
                Box(modifier = Modifier.size(80.dp)) {
                    Image(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(if (isFullImage) 80.dp else 48.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        painter = it,
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(
                modifier = Modifier,
            ) {
                Text(
                    text = contentTitle,
                    style = MaterialTheme.typography.subtitle2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
                contentDescription?.let {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = contentDescription,
                        style = MaterialTheme.typography.caption,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Image(
                    modifier = Modifier.size(16.dp),
                    painter = it,
                    contentDescription = "Image",
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = host,
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RichLinkContentViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CompositionLocalProvider(
            LocalContentColor provides MegaTheme.colors.text.primary,
        ) {
            RichLinkContentView(
                contentTitle = "Title",
                contentDescription = "Description",
                host = "mega.nz",
                image = painterResource(R.drawable.ic_select_folder),
                icon = painterResource(R.drawable.ic_select_folder),
            )
        }
    }
}