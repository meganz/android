package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for checking if a chat is notifiable or not.
 */
class IsChatNotifiableUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    suspend operator fun invoke(chatId: Long) = chatRepository.isChatNotifiable(chatId)
}