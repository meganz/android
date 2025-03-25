package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set Chat Opening with Link Use Case
 *
 */
class IsChatOpeningWithLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(chatId: Long) = chatRepository.isChatOpeningWithLink(chatId)
}
