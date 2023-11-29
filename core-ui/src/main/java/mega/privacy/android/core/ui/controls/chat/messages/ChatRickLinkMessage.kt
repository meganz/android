package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Chat rick link message
 *
 * @param modifier Modifier
 * @param isMe Whether the message is sent by me
 * @param title Title
 * @param contentTitle Content title
 * @param contentDescription Content description
 * @param url Url
 * @param host Host
 * @param image Image
 * @param icon Icon
 */
@Composable
fun ChatRickLinkMessage(
    isMe: Boolean,
    title: String,
    contentTitle: String,
    contentDescription: String,
    url: String,
    host: String,
    image: Painter,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    ChatBubble(
        modifier = modifier,
        isMe = isMe,
        content = {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = url,
                    style = MaterialTheme.typography.caption
                )
            }
        },
        subContent = {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Image(
                        modifier = Modifier.size(80.dp),
                        painter = image,
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .height(80.dp)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            text = contentTitle,
                            style = MaterialTheme.typography.subtitle2
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = contentDescription,
                            style = MaterialTheme.typography.caption,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(16.dp),
                        painter = icon,
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = host,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
    )
}

@CombinedThemePreviews
@Composable
fun ChatRickLinkMessagePreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatRickLinkMessage(
            modifier = Modifier,
            isMe = isMe,
            title = "Title",
            contentTitle = "Content Title",
            contentDescription = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of Lampung. It is located in the most densely populated island of Java. The name is Indonesian for 'Child of Krakatoa'.",
            url = "https://mega.nz",
            host = "mega.nz",
            image = painterResource(id = R.drawable.ic_emoji_smile),
            icon = painterResource(id = R.drawable.ic_emoji_smile),
        )
    }
}