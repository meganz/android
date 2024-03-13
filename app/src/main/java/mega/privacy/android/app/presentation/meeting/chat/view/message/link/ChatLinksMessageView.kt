package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.message.getMessageText
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Chat links message view
 *
 * @param message
 * @param modifier
 * @param linkViews
 */
@Composable
fun ChatLinksMessageView(
    message: TextLinkMessage,
    modifier: Modifier = Modifier,
    linkViews: @Composable () -> Unit,
) {
    with(message) {
        ChatBubble(
            isMe = isMine,
            modifier = modifier,
            subContent = linkViews,
            content = {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = getMessageText(
                        message = content,
                        isEdited = isEdited,
                    ),
                )
            },
        )
    }
}