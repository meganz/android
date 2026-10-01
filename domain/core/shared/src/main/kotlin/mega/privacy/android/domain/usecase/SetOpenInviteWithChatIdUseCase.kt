package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for set open invite for chat
 */
class SetOpenInviteWithChatIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @param chatId  The chat id.
     * @return  True if it's enabled, false if not.
     */
    suspend operator fun invoke(chatId: Long): Boolean =
        chatRepository.setOpenInvite(chatId = chatId)
}
