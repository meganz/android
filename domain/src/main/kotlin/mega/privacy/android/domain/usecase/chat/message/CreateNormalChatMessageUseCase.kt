package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.usecase.chat.GetLinkTypesUseCase
import javax.inject.Inject

/**
 * Create normal chat message use case.
 */
class CreateNormalChatMessageUseCase @Inject constructor(
    private val getLinkTypesUseCase: GetLinkTypesUseCase,
) : CreateTypedMessageUseCase {
    //To be implemented the different type of normal messages. Check [NormalMessage].
    override fun invoke(request: CreateTypedMessageRequest): NormalMessage {
        with(request) {
            val allLinks = getLinkTypesUseCase(message.content.orEmpty())
            val hasSupportedLink = allLinks.any { it.type in supportedTypes }
            return when {
                hasSupportedLink ->
                    TextLinkMessage(
                        msgId = message.msgId,
                        time = message.timestamp,
                        isMine = isMine,
                        userHandle = message.userHandle,
                        links = allLinks,
                        content = message.content.orEmpty(),
                        tempId = message.tempId
                    )

                else -> TextMessage(
                    msgId = message.msgId,
                    time = message.timestamp,
                    isMine = isMine,
                    userHandle = message.userHandle,
                    tempId = message.tempId,
                    content = message.content,
                    hasOtherLink = allLinks.any { it.type !in supportedTypes }
                )
            }
        }
    }

    companion object {
        private val supportedTypes = setOf(
            RegexPatternType.CONTACT_LINK,
            RegexPatternType.FILE_LINK,
            RegexPatternType.FOLDER_LINK,
            RegexPatternType.CHAT_LINK,
        )
    }
}