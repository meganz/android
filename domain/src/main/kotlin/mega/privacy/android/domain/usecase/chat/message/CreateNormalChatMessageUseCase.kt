package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.usecase.chat.GetLinkTypesUseCase
import javax.inject.Inject

/**
 * Create normal chat message use case.
 */
class CreateNormalChatMessageUseCase @Inject constructor(
    private val getLinkTypesUseCase: GetLinkTypesUseCase,
) : CreateTypedMessageUseCase {
    //To be implemented the different type of normal messages. Check [NormalMessage].
    override fun invoke(request: CreateTypedMessageInfo): NormalMessage {
        with(request) {
            val allLinks = getLinkTypesUseCase(content.orEmpty())
            val hasSupportedLink = allLinks.any { it.type in supportedTypes }
            return when {
                hasSupportedLink ->
                    TextLinkMessage(
                        msgId = msgId,
                        time = timestamp,
                        isMine = isMine,
                        userHandle = userHandle,
                        links = allLinks,
                        content = content.orEmpty(),
                        tempId = tempId,
                        shouldShowAvatar = shouldShowAvatar,
                        shouldShowTime = shouldShowTime,
                        shouldShowDate = shouldShowDate,
                    )

                else -> TextMessage(
                    msgId = msgId,
                    time = timestamp,
                    isMine = isMine,
                    userHandle = userHandle,
                    tempId = tempId,
                    content = content,
                    hasOtherLink = allLinks.any { it.type !in supportedTypes },
                    shouldShowAvatar = shouldShowAvatar,
                    shouldShowTime = shouldShowTime,
                    shouldShowDate = shouldShowDate,
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