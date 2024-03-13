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
    override suspend fun invoke(request: CreateTypedMessageInfo): NormalMessage {
        with(request) {
            val allLinks = getLinkTypesUseCase(content.orEmpty())
            val hasSupportedLink = allLinks.any { it.type in supportedTypes }
            return when {
                hasSupportedLink ->
                    TextLinkMessage(
                        chatId = chatId,
                        msgId = messageId,
                        time = timestamp,
                        isDeletable = isDeletable,
                        isEditable = isEditable,
                        isMine = isMine,
                        userHandle = userHandle,
                        links = allLinks,
                        content = content.orEmpty(),
                        shouldShowAvatar = shouldShowAvatar,
                        reactions = reactions,
                        status = status,
                        rowId = rowId,
                        isEdited = isEdited,
                    )

                else -> TextMessage(
                    chatId = chatId,
                    msgId = messageId,
                    time = timestamp,
                    isDeletable = isDeletable,
                    isEditable = isEditable,
                    isMine = isMine,
                    userHandle = userHandle,
                    content = content.orEmpty(),
                    hasOtherLink = allLinks.any { it.type !in supportedTypes },
                    shouldShowAvatar = shouldShowAvatar,
                    reactions = reactions,
                    isEdited = isEdited,
                    status = status,
                    rowId = rowId,
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