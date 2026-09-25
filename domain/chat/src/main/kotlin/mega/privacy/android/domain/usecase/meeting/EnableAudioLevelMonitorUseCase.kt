package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Enable or disable audio level monitor
 */
class EnableAudioLevelMonitorUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Invoke the use case
     *
     * @param enable True for enable audio level monitor, False to disable
     * @param chatId MegaChatHandle that identifies the chat room where we can enable audio level monitor
     */
    suspend operator fun invoke(enable: Boolean, chatId: Long) =
        chatRepository.enableAudioLevelMonitor(enable = enable, chatId = chatId)
}