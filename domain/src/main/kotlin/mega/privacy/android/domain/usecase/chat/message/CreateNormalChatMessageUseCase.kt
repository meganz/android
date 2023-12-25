package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.normal.ContactLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.usecase.link.ExtractContactLinkUseCase
import javax.inject.Inject

/**
 * Create normal chat message use case.
 */
class CreateNormalChatMessageUseCase @Inject constructor(
    private val extractContactLinkUseCase: ExtractContactLinkUseCase,
) : CreateTypedMessageUseCase {
    //To be implemented the different type of normal messages. Check [NormalMessage].
    override fun invoke(message: ChatMessage, isMine: Boolean): NormalMessage {
        val contactLink = extractContactLinkUseCase(message.content.orEmpty())
        return when {
            !contactLink.isNullOrBlank() ->
                ContactLinkMessage(
                    msgId = message.msgId,
                    time = message.timestamp,
                    isMine = isMine,
                    userHandle = message.userHandle,
                    contactLink = contactLink,
                    content = message.content.orEmpty(),
                    tempId = message.tempId
                )

            else -> TextMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                tempId = message.tempId,
                content = message.content
            )
        }
    }
}