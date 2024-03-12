package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to get if a content exists in a message.
 */
class GetExistsInMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Gets if a node exists in a message.
     *
     * @param chatId Node id.
     * @param msgId If the node exists.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long) =
        chatMessageRepository.getExistsInMessage(chatId, msgId)
}