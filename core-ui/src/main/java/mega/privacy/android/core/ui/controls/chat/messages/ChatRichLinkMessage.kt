package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
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
 * @param isMine Whether the message is sent by me
 * @param title Title
 * @param contentTitle Content title
 * @param contentDescription Content description
 * @param content Message content
 * @param host Host
 * @param image Image
 * @param icon Icon
 */
@Composable
fun ChatRichLinkMessage(
    isMine: Boolean,
    title: String,
    contentTitle: String,
    contentDescription: String,
    content: AnnotatedString,
    host: String,
    image: Painter?,
    icon: Painter?,
    modifier: Modifier = Modifier,
) {
    ChatBubble(
        modifier = modifier,
        isMe = isMine,
        content = {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = content,
                    style = MaterialTheme.typography.caption
                )
            }
        },
        subContent = {
            RichLinkContentView(
                image = image,
                contentTitle = contentTitle,
                contentDescription = contentDescription,
                icon = icon,
                host = host
            )
        },
    )
}

@CombinedThemePreviews
@Composable
private fun ChatRickLinkMessagePreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatRichLinkMessage(
            modifier = Modifier,
            isMine = isMe,
            title = "Title",
            contentTitle = "Content Title",
            contentDescription = "is a caldera in the Sunda Strait between the islands of Java and Sumatra in the Indonesian province of Lampung. It is located in the most densely populated island of Java. The name is Indonesian for 'Child of Krakatoa'.",
            content = buildAnnotatedString { append("https://mega.nz") },
            host = "mega.nz",
            image = painterResource(R.drawable.ic_select_folder),
            icon = painterResource(R.drawable.ic_select_folder),
        )
    }
}