package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.RichLinkContentView
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Chat link message view
 *
 * @param linkContent
 * @param modifier
 */
@Composable
fun ChatLinkMessageView(
    linkContent: ChatGroupLinkContent,
    modifier: Modifier = Modifier,
) {
    if (linkContent.isChatAvailable) {
        RichLinkContentView(
            modifier = modifier,
            image = painterResource(R.drawable.ic_group_chat_link),
            contentTitle = linkContent.name,
            contentDescription = stringResource(
                R.string.number_of_participants,
                linkContent.numberOfParticipants
            ),
            icon = painterResource(R.drawable.ic_logo_notifications),
            host = Uri.parse(linkContent.link).authority.orEmpty()
        )
    } else {
        RichLinkContentView(
            modifier = modifier,
            contentTitle = stringResource(id = R.string.invalid_chat_link),
            icon = painterResource(R.drawable.ic_logo_notifications),
            host = Uri.parse(linkContent.link).authority.orEmpty()
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatLinkMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = isMe, subContent = {
            ChatLinkMessageView(
                linkContent = ChatGroupLinkContent(
                    numberOfParticipants = 10,
                    name = "Group name",
                    link = "https://mega.app/chat/1234567890#1234567890",
                )
            )
        }) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = "https://mega.app/chat/1234567890#1234567890"
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInvalidLinkMessageViewPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = isMe, subContent = {
            ChatLinkMessageView(
                linkContent = ChatGroupLinkContent(
                    link = "https://mega.app/chat/1234567890#1234567890",
                )
            )
        }) {
            Text(
                modifier = Modifier.padding(12.dp),
                text = "https://mega.app/chat/1234567890#1234567890"
            )
        }
    }
}