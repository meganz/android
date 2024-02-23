package mega.privacy.android.domain.usecase.chat.message.edit

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for editing a message.
 */
class EditMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     * @param msgId Message ID.
     * @param content New content of the message.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long, content: String) =
        chatMessageRepository.editMessage(chatId, msgId, content)
}