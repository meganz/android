package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Get the timestamp until which the notifications of a chat room are muted
 */
class GetChatDoNotDisturbTimeUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * invoke
     *
     * @param chatId id of the chat room
     * @return timestamp until DND mode is enabled (in seconds since the Epoch), 0 when
     * notifications are muted until turned back on
     */
    suspend operator fun invoke(chatId: Long): Long =
        notificationsRepository.getChatDoNotDisturbTime(chatId)
}
