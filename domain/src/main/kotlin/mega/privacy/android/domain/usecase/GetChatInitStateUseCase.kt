package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get chat API init status
 */
class GetChatInitStateUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Invoke
     * @return chat init state
     */
    suspend operator fun invoke(): ChatInitState = chatRepository.getChatInitState()
}