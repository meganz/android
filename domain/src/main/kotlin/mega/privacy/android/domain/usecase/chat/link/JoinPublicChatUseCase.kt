package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Join public chat
 *
 * @property chatRepository
 */
class JoinPublicChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @param chatPublicHandle Chat public handle. Mandatory in case of re-joining.
     */
    suspend operator fun invoke(
        chatId: Long,
        chatPublicHandle: Long? = null,
    ) {
        chatPublicHandle?.let {
            chatRepository.autorejoinPublicChat(chatId, chatPublicHandle)
        } ?: chatRepository.autojoinPublicChat(chatId)
        chatRepository.setLastPublicHandle(chatId)
    }
}