package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.presentation.meeting.chat.extension.toUiChatStatus
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.chat.messages.ContactMessageContentView
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Chat link message view
 *
 * @param message Chat link message
 * @param modifier Modifier
 * @param viewModel Chat link message view model
 */
@Composable
fun ChatLinksMessageView(
    message: TextLinkMessage,
    modifier: Modifier = Modifier,
    viewModel: ChatLinksMessageViewModel = hiltViewModel(),
) {
    var contentLinks by remember {
        mutableStateOf(listOf<LinkContent>())
    }
    LaunchedEffect(message.links) {
        message.links.forEach {
            when (it.type) {
                RegexPatternType.CONTACT_LINK -> {
                    val linkContent = viewModel.loadContactInfo(it.link)
                    if (linkContent != null) {
                        contentLinks = contentLinks + linkContent
                    }
                }

                RegexPatternType.CHAT_LINK -> {
                    val linkContent = viewModel.loadChatLinkInfo(it.link)
                    contentLinks = contentLinks + linkContent
                }
                // other link type here
                else -> Unit
            }
        }
    }

    ChatBubble(
        isMe = message.isMine,
        modifier = modifier,
        subContent = {
            contentLinks.forEachIndexed { index, linkContent ->
                when (linkContent) {
                    is ContactLinkContent -> {
                        key(linkContent.link) {
                            ContactMessageContentView(
                                userName = linkContent.content.fullName.orEmpty(),
                                email = linkContent.content.email.orEmpty(),
                                status = linkContent.content.status.toUiChatStatus(),
                                avatar = {
                                    ChatAvatar(
                                        handle = linkContent.content.contactHandle,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                            )
                        }
                    }
                    // call other compose ui for other link type here
                    is ChatGroupLinkContent -> {
                        key(linkContent.link) {
                            ChatLinkMessageView(linkContent = linkContent)
                        }
                    }
                }

                if (index != contentLinks.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                }
            }
        },
        content = {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = message.content,
            )
        },
    )
}