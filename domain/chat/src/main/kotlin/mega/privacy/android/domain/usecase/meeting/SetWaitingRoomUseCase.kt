package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set Waiting Room Use Case
 */
class SetWaitingRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param chatId            Chat id
     * @param enabled           True, enable waiting room setting. False, disable waiting room setting
     * @return                  [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        enabled: Boolean,
    ): ChatRequest =
        chatRepository.setWaitingRoom(
            chatId,
            enabled,
        )
}