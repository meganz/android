package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Remove Chat Opening with Link Use Case
 *
 */
class RemoveChatOpeningWithLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(chatId: Long) {
        chatRepository.removeChatOpeningWithLink(chatId)
    }
}
