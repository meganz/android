package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Returns if audio level monitor is enabled
 *
 * It's false by default
 */
class IsAudioLevelMonitorEnabledUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Invoke the use case
     *
     * @note If there isn't a call in that chatroom in which user is participating,
     * audio Level monitor will be always false
     *
     * @param chatId MegaChatHandle that identifies the chat room from we want know if audio level monitor is disabled
     * @return true if audio level monitor is enabled
     */
    suspend operator fun invoke(chatId: Long): Boolean =
        chatRepository.isAudioLevelMonitorEnabled(chatId = chatId)
}