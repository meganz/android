package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject


/**
 * Use case for clearing all chat data from database
 */
class ClearAllChatDataUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke() =
        chatMessageRepository.clearAllData()
}