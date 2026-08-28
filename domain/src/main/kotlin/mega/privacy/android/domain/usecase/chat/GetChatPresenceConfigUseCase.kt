package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get chat presence config use case
 *
 * @property chatRepository
 */
class GetChatPresenceConfigUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @return The current [ChatPresenceConfig], or null if not available yet.
     */
    suspend operator fun invoke(): ChatPresenceConfig? =
        chatRepository.getChatPresenceConfig()
}
