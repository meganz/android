package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject


/**
 * Invite to chat
 */
class InviteParticipantToChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param chatId       Chat id
     * @param handle       User handle
     */
    suspend operator fun invoke(
        chatId: Long,
        handle: Long,
    ): ChatRequest =
        chatRepository.inviteParticipantToChat(
            chatId,
            handle,
        )
}