package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * unmute chat notification use case
 */
class UnmuteChatNotificationUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke the use case
     * @param chatId    Id of the chat room
     */
    suspend operator fun invoke(chatId: Long) =
        notificationsRepository.setChatEnabled(chatId, true)
}