package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Check if notification of a chat room is muted
 */
class IsChatNotificationMuteUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * invoke
     * @param chatId - id of the chat room nod
     * @return true if notification is mute. false otherwise.
     */
    suspend operator fun invoke(chatId: Long) =
        notificationsRepository.isChatDoNotDisturbEnabled(chatId)
}