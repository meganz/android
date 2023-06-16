package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the behaviour of a chat message notification.
 */
class GetChatMessageNotificationBehaviourUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    suspend operator fun invoke(beep: Boolean, defaultSound: String) =
        chatRepository.getChatMessageNotificationBehaviour(beep, defaultSound)
}