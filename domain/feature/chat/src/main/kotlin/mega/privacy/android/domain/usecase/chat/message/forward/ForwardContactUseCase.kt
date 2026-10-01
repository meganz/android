package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.CreateSaveSentMessageRequestUseCase
import javax.inject.Inject

/**
 * Use case for forwarding a contact.
 */
class ForwardContactUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
) : ForwardMessageUseCase() {

    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val contactMessage = message as? ContactAttachmentMessage ?: return null
        chatMessageRepository.forwardContact(message.chatId, contactMessage.msgId, targetChatId)
            ?.let {
                val request = createSaveSentMessageRequestUseCase(it, targetChatId)
                chatRepository.storeMessages(listOf(request))
            }
        return ForwardResult.Success(targetChatId)
    }
}