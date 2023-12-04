package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Compose view for location message.
 * The message contains a title, latitude, longitude and a map.
 *
 * @param isMe whether the message is sent by me
 * @param title title of the message
 * @param latlon the string with latitude and longitude
 * @param map the map image
 */
@Composable
fun LocationMessageView(
    isMe: Boolean,
    title: String,
    latlon: String,
    map: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(width = 256.dp, height = 200.dp)
            .background(
                color = MegaTheme.colors.background.pageBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isMe) MegaTheme.colors.border.strongSelected else MegaTheme.colors.background.surface2,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = if (isMe) MegaTheme.colors.border.strong else MegaTheme.colors.background.surface2,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            bitmap = map,
            contentScale = ContentScale.Crop,
            contentDescription = "map",
        )
        Text(
            modifier = Modifier.padding(bottom = 6.dp, top = 16.dp, start = 12.dp),
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Normal,
            color = MegaTheme.colors.text.primary
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = latlon,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Normal,
            color = MegaTheme.colors.text.primary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LocationMessagePreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LocationMessageView(
            isMe = isMe,
            title = "Pinned location",
            latlon = "41.1472° N, 8.6179° W",
            map = ImageBitmap.imageResource(R.drawable.ic_folder_incoming),
        )
    }
}
