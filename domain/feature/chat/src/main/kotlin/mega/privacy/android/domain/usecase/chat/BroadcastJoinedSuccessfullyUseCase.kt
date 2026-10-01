package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when successfully joined to a chat.
 */
class BroadcastJoinedSuccessfullyUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = chatRepository.broadcastJoinedSuccessfully()
}