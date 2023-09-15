package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Attach voice message use case
 *
 * @property chatRepository [ChatRepository]
 */
class AttachVoiceMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat identifier.
     * @param nodeHandle Node identifier.
     */
    suspend operator fun invoke(chatId: Long, nodeHandle: Long) =
        chatRepository.attachVoiceMessage(chatId, nodeHandle)
}