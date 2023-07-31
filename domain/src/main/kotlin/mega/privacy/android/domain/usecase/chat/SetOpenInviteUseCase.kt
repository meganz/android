package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set open invite use case
 */
class SetOpenInviteUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @param isOpenInvite  True, should be enabled. False, otherwise.
     */
    suspend operator fun invoke(
        chatId: Long,
        isOpenInvite: Boolean,
    ): ChatRequest =
        chatRepository.setOpenInvite(
            chatId,
            isOpenInvite,
        )
}