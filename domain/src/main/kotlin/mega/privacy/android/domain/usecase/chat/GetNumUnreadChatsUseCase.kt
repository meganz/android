package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the number of unread chats for the logged in user.
 */
class GetNumUnreadChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = chatRepository.getNumUnreadChats()
}